package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.UserInfo;
import com.hello.suripu.core.util.PillColorUtil;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Created by pangwu on 1/9/15.
 */
public class RecreatePillColorCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    private final static Logger LOGGER = LoggerFactory.getLogger(RecreateEventsCommand.class);

    public RecreatePillColorCommand(){
        super("gen_colors", "Recreate pill colors from DeviceDAO");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource dataSource = managedDataSourceFactory.build(configuration.getCommonDB());

        final DBI jdbi = new DBI(dataSource);
        jdbi.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        jdbi.registerContainerFactory(new ImmutableListContainerFactory());
        jdbi.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbi.registerContainerFactory(new OptionalContainerFactory());
        jdbi.registerArgumentFactory(new JodaArgumentFactory());

        final DeviceDAO deviceDAO = jdbi.onDemand(DeviceDAO.class);

        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getUserInfoDynamoDBConfiguration().getEndpoint());
        final String eventTableName = configuration.getUserInfoDynamoDBConfiguration().getTableName();
        final MergedUserInfoDynamoDB mergedUserInfoDynamoDB = new MergedUserInfoDynamoDB(client, eventTableName);

        final List<DeviceAccountPair> activePills = deviceDAO.getAllPills(true);
        final HashMap<String, ArrayList<DeviceAccountPair>> sensePillMap = new HashMap<>();
        for(final DeviceAccountPair pill:activePills){
            final List<DeviceAccountPair> sensePairedWithAccount = deviceDAO.getSensesForAccountId(pill.accountId);
            for(final DeviceAccountPair sense:sensePairedWithAccount){
                if(!sensePillMap.containsKey(sense.externalDeviceId)){
                    sensePillMap.put(sense.externalDeviceId, new ArrayList<DeviceAccountPair>());
                }
                sensePillMap.get(sense.externalDeviceId).add(pill);
            }
        }

        for(final String senseId:sensePillMap.keySet()){
            final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(senseId);

            final ArrayList<DeviceAccountPair> pillsLinkedToSense = sensePillMap.get(senseId);
            Collections.sort(pillsLinkedToSense, new Comparator<DeviceAccountPair>() {
                @Override
                public int compare(DeviceAccountPair o1, DeviceAccountPair o2) {
                    return Long.compare(o1.accountId, o2.accountId);
                }
            });

            for(final DeviceAccountPair pill:pillsLinkedToSense){
                final Color pillColor = PillColorUtil.getPillColorByAccountRegistrationOrder(userInfoList.size());
                mergedUserInfoDynamoDB.setPillColor(senseId, pill.accountId, pill.externalDeviceId, pillColor);
            }
        }
    }
}
