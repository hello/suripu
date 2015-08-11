package com.hello.suripu.app.cli;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.AlgorithmResultsDAODynamoDB;
import com.hello.suripu.core.db.BayesNetHmmModelDAODynamoDB;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.hello.suripu.coredw.db.SleepHmmDAODynamoDB;
import com.hello.suripu.coredw.db.TimelineDAODynamoDB;
import com.hello.suripu.coredw.db.TimelineLogDAODynamoDB;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class CreateDynamoDBTables extends ConfiguredCommand<SuripuAppConfiguration> {

    public CreateDynamoDBTables() {
        super("create_dynamodb_tables", "Create dynamoDB tables");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();

        createUserInfoTable(configuration, awsCredentialsProvider);
        createAlarmTable(configuration, awsCredentialsProvider);
        createFeaturesTable(configuration, awsCredentialsProvider);
        createTeamsTable(configuration, awsCredentialsProvider);
        createSleepScoreTable(configuration, awsCredentialsProvider);
        createScheduledRingTimeHistoryTable(configuration, awsCredentialsProvider);
        createTimeZoneHistoryTable(configuration, awsCredentialsProvider);
        createInsightsTable(configuration, awsCredentialsProvider);
        createAccountPreferencesTable(configuration, awsCredentialsProvider);
        createSenseKeyStoreTable(configuration, awsCredentialsProvider);
        createPillKeyStoreTable(configuration, awsCredentialsProvider);
        createTimelineTable(configuration, awsCredentialsProvider);
        createPasswordResetTable(configuration, awsCredentialsProvider);
        createSleepHmmTable(configuration, awsCredentialsProvider);
        createRingTimeHistoryTable(configuration, awsCredentialsProvider);
        createSleepStatsTable(configuration, awsCredentialsProvider);
        createAlgorithmTestTable(configuration, awsCredentialsProvider);
        createTimelineLogTable(configuration, awsCredentialsProvider);
        createSmartAlarmLogTable(configuration, awsCredentialsProvider);
        createOTAHistoryTable(configuration, awsCredentialsProvider);
        createResponseCommandsTable(configuration, awsCredentialsProvider);
        createFWUpgradePathTable(configuration, awsCredentialsProvider);
        createHmmBayesNetModelPriorTable(configuration,awsCredentialsProvider);
        createHmmBayesNetModelTable(configuration,awsCredentialsProvider);
        CreatedCalibrationTable(configuration, awsCredentialsProvider);
    }

    private void createSmartAlarmLogTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider){
        final DynamoDBConfiguration config = configuration.getSmartAlarmLogDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final String tableName = config.getTableName();
        client.setEndpoint(config.getEndpoint());
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SmartAlarmLoggerDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createAccountPreferencesTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final DynamoDBConfiguration config = configuration.getPreferencesDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final String tableName = config.getTableName();
        client.setEndpoint(config.getEndpoint());
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AccountPreferencesDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createScheduledRingTimeHistoryTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        DynamoDBConfiguration config = configuration.getScheduledRingTimeHistoryDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final String tableName = config.getTableName();
        client.setEndpoint(config.getEndpoint());
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = ScheduledRingTimeHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createSleepScoreTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final DynamoDBConfiguration config = configuration.getSleepScoreDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final String version = configuration.getSleepScoreVersion();

        client.setEndpoint(config.getEndpoint());
        final String tableName = config.getTableName() + "_" + version;
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AggregateSleepScoreDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createAlgorithmTestTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final DynamoDBConfiguration config = configuration.getAlgorithmTestDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.getEndpoint());
        final String tableName = config.getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AlgorithmResultsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createInsightsTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final DynamoDBConfiguration config = configuration.getInsightsDynamoDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.getEndpoint());
        final String tableName = config.getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = InsightsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTeamsTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {

        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        client.setEndpoint(configuration.getTeamsDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getTeamsDynamoDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TeamStore.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createUserInfoTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {

        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getUserInfoDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getUserInfoDynamoDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = MergedUserInfoDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createFeaturesTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {

        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getFeaturesDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getFeaturesDynamoDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = FeatureStore.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createAlarmTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {

        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getAlarmDBConfiguration().getEndpoint());
        final String tableName = configuration.getAlarmDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AlarmDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTimeZoneHistoryTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {

        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getTimeZoneHistoryDBConfiguration().getEndpoint());
        final String tableName = configuration.getTimeZoneHistoryDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TimeZoneHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createSenseKeyStoreTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getSenseKeyStoreDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getSenseKeyStoreDynamoDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = KeyStoreDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createPillKeyStoreTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getPillKeyStoreDynamoDBConfiguration().getEndpoint());
        final String tableName = configuration.getPillKeyStoreDynamoDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = KeyStoreDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTimelineTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getTimelineDBConfiguration().getEndpoint());
        final String tableName = configuration.getTimelineDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TimelineDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }


    private void createPasswordResetTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getPasswordResetDBConfiguration().getEndpoint());
        final String tableName = configuration.getPasswordResetDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = PasswordResetDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println("Table: " + tableName + " " + description.getTableStatus());
        }
    }

    private void createRingTimeHistoryTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider){
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getRingTimeHistoryDBConfiguration().getEndpoint());
        final String tableName = configuration.getRingTimeHistoryDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = RingTimeHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println("Table: " + tableName + " " + description.getTableStatus());
        }
    }

    private void createSleepHmmTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        client.setEndpoint(configuration.getSleepHmmDBConfiguration().getEndpoint());
        final String tableName = configuration.getSleepHmmDBConfiguration().getTableName();

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SleepHmmDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createSleepStatsTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final DynamoDBConfiguration config = configuration.getSleepStatsDynamoConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final String version = configuration.getSleepStatsVersion();

        client.setEndpoint(config.getEndpoint());
        final String tableName = config.getTableName() + "_" + version;
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SleepStatsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTimelineLogTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        final String tableName = configuration.getTimelineLogDBConfiguration().getTableName();
        client.setEndpoint(configuration.getTimelineLogDBConfiguration().getEndpoint());

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TimelineLogDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createOTAHistoryTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getOTAHistoryDBConfiguration();
        final String tableName = configuration.getOTAHistoryDBConfiguration().getTableName();
        client.setEndpoint(config.getEndpoint());

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = OTAHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createResponseCommandsTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getResponseCommandsDBConfiguration();
        final String tableName = configuration.getResponseCommandsDBConfiguration().getTableName();
        client.setEndpoint(config.getEndpoint());

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = ResponseCommandsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createFWUpgradePathTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getFWUpgradePathDBConfiguration();
        final String tableName = configuration.getFWUpgradePathDBConfiguration().getTableName();
        client.setEndpoint(config.getEndpoint());

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = FirmwareUpgradePathDAO.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }


    private void createHmmBayesNetModelPriorTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getHmmBayesnetPriorsConfiguration();
        final String tableName = config.getTableName();
        client.setEndpoint(config.getEndpoint());

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = BayesNetHmmModelPriorsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createHmmBayesNetModelTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getHmmBayesnetModelsConfiguration();
        final String tableName = config.getTableName();
        client.setEndpoint(config.getEndpoint());

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = BayesNetHmmModelDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }


    private void createdCalibrationTable(final SuripuAppConfiguration configuration, final AWSCredentialsProvider awsCredentialsProvider) {
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);
        final DynamoDBConfiguration config = configuration.getCalibrationConfiguration();
        final String tableName = config.getTableName();
        client.setEndpoint(config.getEndpoint());

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = CalibrationDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

}
