package com.hello.suripu.admin;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class PopulateVersionNumbers {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopulateVersionNumbers.class);

    public static void main(String[] args) throws InterruptedException {


        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonS3 amazonS3 = new AmazonS3Client(awsCredentialsProvider);
        final AmazonDynamoDB amazonDynamoDB = new AmazonDynamoDBClient(awsCredentialsProvider);
        final FirmwareVersionMappingDAO firmwareVersionMappingDAO = new FirmwareVersionMappingDAO(amazonDynamoDB, "firmware_versions_mapping");
        ObjectListing objectListing;

        final ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName("hello-firmware").withPrefix("sense/0.");

        final List<String> keys = Lists.newArrayList();
        int i = 0;
        do {
            objectListing = amazonS3.listObjects(listObjectsRequest);
            for (S3ObjectSummary objectSummary :
                    objectListing.getObjectSummaries()) {
                if(objectSummary.getKey().contains("build_info.txt")) {
                    keys.add(objectSummary.getKey());
                }
            }
            listObjectsRequest.setMarker(objectListing.getNextMarker());
            i ++;
            LOGGER.info("Iteration: {}", i);
        } while (objectListing.isTruncated() && i < 5);


        for(final String key: keys) {
            final S3Object s3Object = amazonS3.getObject("hello-firmware", key);

            final S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
            String text;
            try {
                text = CharStreams.toString(new InputStreamReader(s3ObjectInputStream, Charsets.UTF_8));
            } catch (IOException e) {
                LOGGER.error("Failed reading build_info for key {} from s3 : {}", key, e.getMessage());
                continue;
            }

            final Iterable<String> strings = Splitter.on("\n").split(text);
            final String firstLine = strings.iterator().next();
            String[] parts = firstLine.split(":");
            final String hash = parts[1].trim();
            final String humanVersion = key.split("/")[1];
            firmwareVersionMappingDAO.put(hash, humanVersion);
            LOGGER.info("Hash = {} and Key = {}", hash, key);
            Thread.sleep(1200L);
        }
    }
}
