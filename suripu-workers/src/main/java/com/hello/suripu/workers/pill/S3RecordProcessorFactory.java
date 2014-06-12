package com.hello.suripu.workers.pill;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.s3.AmazonS3Client;

public class S3RecordProcessorFactory implements IRecordProcessorFactory {

    private final AmazonS3Client amazonS3Client;
    private final String s3BucketName;
    private final KinesisClientLibConfiguration configuration;

    public S3RecordProcessorFactory(
            final AmazonS3Client amazonS3Client,
            final String s3BucketName,
            final KinesisClientLibConfiguration configuration) {
        this.amazonS3Client = amazonS3Client;
        this.s3BucketName = s3BucketName;
        this.configuration = configuration;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new S3RecordProcessor(amazonS3Client, s3BucketName);
    }
}
