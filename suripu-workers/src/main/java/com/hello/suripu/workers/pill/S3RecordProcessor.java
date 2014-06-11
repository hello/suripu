package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class S3RecordProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(S3RecordProcessor.class);

    private final AmazonS3Client amazonS3Client;
    private final String s3BucketName;

    public S3RecordProcessor(final AmazonS3Client amazonS3Client, final String s3BucketName) {
        this.amazonS3Client = amazonS3Client;
        this.s3BucketName = s3BucketName;
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        LOGGER.debug("Size = {}", records.size());

        final InputProtos.PillBlob.Builder builder = InputProtos.PillBlob.newBuilder();
        final ArrayList<String> sequenceNumbers = new ArrayList<String>();
        final ArrayList<String> accountIds = new ArrayList<String>();


        for(Record record : records) {
            sequenceNumbers.add(record.getSequenceNumber());
            LOGGER.debug("PartitionKey: {}", record.getPartitionKey());

            try {
                final InputProtos.PillData data = InputProtos.PillData.parseFrom(record.getData().array());
                // Adding account ids to sort them later
                accountIds.add(data.getAccountId());
                builder.addItems(data);
            } catch (InvalidProtocolBufferException e) {
                LOGGER.error("Failed to decode protobuf: {}", e.getMessage());
                // TODO: increment error counter somewhere
            }
        }

        try {
            if(persistData(builder, sequenceNumbers, accountIds)) {
                iRecordProcessorCheckpointer.checkpoint();
            }
        } catch (InvalidStateException e) {
            LOGGER.error("{}", e.getMessage());
        } catch (ShutdownException e) {
            LOGGER.error("Received shutdown command, bailing. {}", e.getMessage());
        }
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
        amazonS3Client.shutdown();
    }

    private Optional<byte[]> buildIndex(final List<String> accountIds) {
        final RoaringBitmap roaringBitmap = new RoaringBitmap();
        for(String accountId : accountIds) {
            roaringBitmap.add(Integer.valueOf(accountId));
        }

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(bos);
        try {
            roaringBitmap.serialize(dos);
        } catch (IOException e) {
            LOGGER.error("Failed generating Bitmap index: {}", e.getMessage());
            return Optional.absent();
        }
        return Optional.of(bos.toByteArray());
    }


    /**
     * Persist Pill Data
     * @param builder
     * @param sequenceNumbers
     * @param accountIds
     */
    private boolean persistData(final InputProtos.PillBlob.Builder builder, final List<String> sequenceNumbers, final List<String> accountIds) {
        final String filename = String.format("%s-%s", sequenceNumbers.get(0), sequenceNumbers.get(sequenceNumbers.size() -1));
        final String headerFilename = String.format("%s-header", filename);

        final Optional<byte[]> compressedIndex = buildIndex(accountIds);
        if(!compressedIndex.isPresent()) {
            LOGGER.warn("Failed to compress accountId index. Bailing.");
            return false;
        }

        final InputProtos.PillBlobHeader pillBlodHeader = InputProtos.PillBlobHeader.newBuilder()
                .setCompressedBitmapAccountIds(ByteString.copyFrom(compressedIndex.get()))
                .setFirstSequenceNumber(sequenceNumbers.get(0))
                .setLastSequenceNumber(sequenceNumbers.get(sequenceNumbers.size() - 1))
                .setDataFileName(filename)
                .setNumItems(sequenceNumbers.size())
                .build();

        final byte[] headerBytes = pillBlodHeader.toByteArray();
        final byte[] dataBytes = builder.build().toByteArray();

        LOGGER.debug("Filename = {}", filename);
        LOGGER.debug("Header Filename = {}", headerFilename);

        final Long headerContentLength = Long.valueOf(headerBytes.length);
        final ObjectMetadata headerMetadata = new ObjectMetadata();
        headerMetadata.setContentLength(headerContentLength);


        final Long dataContentLength = Long.valueOf(dataBytes.length);
        final ObjectMetadata dataMetadata = new ObjectMetadata();
        dataMetadata.setContentLength(dataContentLength);

        final PutObjectResult blobResult = amazonS3Client.putObject(s3BucketName, filename, new ByteArrayInputStream(dataBytes), dataMetadata);
        final PutObjectResult headerResult = amazonS3Client.putObject(s3BucketName, headerFilename, new ByteArrayInputStream(headerBytes), headerMetadata);

        LOGGER.debug("Blob content MD5: {}", blobResult.getContentMd5());
        LOGGER.debug("Header content MD5: {}", headerResult.getContentMd5());

        return true;
    }
}
