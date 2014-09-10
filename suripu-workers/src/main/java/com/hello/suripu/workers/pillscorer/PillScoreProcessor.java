package com.hello.suripu.workers.pillscorer;

import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.db.SleepScoreDAO;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PillScoreProcessor implements IRecordProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillScoreProcessor.class);

    private final SleepScoreDAO sleepScoreDAO;


    public PillScoreProcessor(final SleepScoreDAO sleepScoreDAO) {
        this.sleepScoreDAO = sleepScoreDAO;
    }

    @Override
    public void initialize(String s) {

    }

    @Override
    public void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
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
    public void shutdown(final IRecordProcessorCheckpointer iRecordProcessorCheckpointer, final ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
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
            return Optional.of(bos.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Failed generating Bitmap index: {}", e.getMessage());
            return Optional.absent();
        } finally {
            try {
                bos.close();
                dos.close();
            } catch (IOException e) {
                LOGGER.error("{}", e.getMessage());
            }
        }

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

        // TODO write scoring results to DB

        return true;
    }
}
