package com.hello.suripu.admin.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.MergeShardsRequest;
import com.amazonaws.services.kinesis.model.Shard;
import com.amazonaws.services.kinesis.model.SplitShardRequest;
import com.google.common.collect.Lists;
import com.hello.suripu.admin.configuration.SuripuAdminConfiguration;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

public class ManageKinesisStreams extends ConfiguredCommand<SuripuAdminConfiguration> {


    private final static Logger LOGGER = LoggerFactory.getLogger(ManageKinesisStreams.class);


    public ManageKinesisStreams() {
        super("kinesis", "Manage Kinesis stream");
    }

    @Override
    protected void run(Bootstrap<SuripuAdminConfiguration> bootstrap, Namespace namespace, SuripuAdminConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonKinesis client = new AmazonKinesisClient(awsCredentialsProvider );
        split(client, "INSERT STREAM NAME HERE");
    }


    private void merge(final AmazonKinesis configuredClient, final String streamName) {
        final MergeShardsRequest mergeShardsRequest = new MergeShardsRequest();
        mergeShardsRequest.setStreamName(streamName);
        mergeShardsRequest.setShardToMerge("4");
        mergeShardsRequest.setAdjacentShardToMerge("5");

        configuredClient.mergeShards(mergeShardsRequest);
    }

    private void split(final AmazonKinesis configuredClient, final String streamName) {
        final DescribeStreamResult result = configuredClient.describeStream(streamName);
        final List<Shard> shards = result.getStreamDescription().getShards();

        final List<Shard> openShards = Lists.newArrayList();

        for(final Shard shard : shards) {
            if (shard.getSequenceNumberRange().getEndingSequenceNumber() == null) {
                openShards.add(shard);
            } else {
                LOGGER.warn("Shard {} is closed", shard.getShardId());
            }
        }

        if(openShards.isEmpty() || openShards.size() > 1) {
            throw new RuntimeException("Expected only one shard. Got " + openShards.size());
        }

        final SplitShardRequest splitShardRequest = new SplitShardRequest();
        final Shard shard = openShards.get(0);
        splitShardRequest.setStreamName(streamName);
        splitShardRequest.setShardToSplit(shard.getShardId());

        final BigInteger startingHashKey = new BigInteger(shard.getHashKeyRange().getStartingHashKey());
        final BigInteger endingHashKey = new BigInteger(shard.getHashKeyRange().getEndingHashKey());
        final String newStartingHashKey = startingHashKey.add(endingHashKey).divide(new BigInteger("2")).toString();
        splitShardRequest.setNewStartingHashKey(newStartingHashKey);
        configuredClient.splitShard(splitShardRequest);

    }
}
