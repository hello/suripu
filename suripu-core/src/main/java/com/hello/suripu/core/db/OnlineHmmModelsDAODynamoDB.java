package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.api.datascience.OnlineHmmProtos;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 8/18/15.
 */
public class OnlineHmmModelsDAODynamoDB implements OnlineHmmModelsDAO {
    private final static String HASH_KEY = "account_id";
    private final static String RANGE_KEY = "date";
    private final static String PAYLOAD_KEY_FOR_PARAMS = "model_params";
    private final static String PAYLOAD_KEY_FOR_SCRATCHPAD = "scratchpad";
    private final static Logger LOGGER = LoggerFactory.getLogger(OnlineHmmModelsDAODynamoDB.class);
    private static final List<String> payloadColumnNames;

    static  {
        payloadColumnNames = Lists.newArrayList();
        payloadColumnNames.add(PAYLOAD_KEY_FOR_PARAMS);
        payloadColumnNames.add(PAYLOAD_KEY_FOR_SCRATCHPAD);
    }

    private final GeneralProtobufDAODynamoDB dbDAO;


    public  static OnlineHmmModelsDAODynamoDB create(final AmazonDynamoDB dynamoDBClient, final String tableName) {
        final GeneralProtobufDAODynamoDB dbDAO = new GeneralProtobufDAODynamoDB(LOGGER, dynamoDBClient, tableName, HASH_KEY, Optional.of(RANGE_KEY), payloadColumnNames);

        return new OnlineHmmModelsDAODynamoDB(dbDAO);
    }

    private OnlineHmmModelsDAODynamoDB(final GeneralProtobufDAODynamoDB dbDAO) {
        this.dbDAO = dbDAO;
    }


    @Override
    public OnlineHmmData getModelDataByAccountId(Long accountId,final DateTime date) {
        final String dateString = DateTimeUtil.dateToYmdString(date);

        final GeneralProtobufDAODynamoDB.GeneralQueryResult results = dbDAO.getBySingleKeyLTERangeKey(accountId.toString(), dateString, 1);

        LOGGER.info("getModelDataByAccountId for user {}",accountId);

        final Map<String,byte[]> allColumns = results.payloadsByKey.get(accountId.toString());

        Optional<OnlineHmmPriors> priors = Optional.absent();
        Optional<OnlineHmmScratchPad> scratchPad = Optional.absent();

        if (allColumns == null) {
            return new OnlineHmmData(priors,scratchPad);
        }

        final byte [] priorProtobufData = allColumns.get(PAYLOAD_KEY_FOR_PARAMS);

        if (priorProtobufData != null) {
            priors = OnlineHmmPriors.createFromProtoBuf(priorProtobufData);
        }

        final byte [] scratchPadProtobufData = allColumns.get(PAYLOAD_KEY_FOR_SCRATCHPAD);

        if (scratchPadProtobufData != null) {
            scratchPad = OnlineHmmScratchPad.createFromProtobuf(scratchPadProtobufData);
        }

        return new OnlineHmmData(priors,scratchPad);
    }

    @Override
    public boolean updateModelPriors(final Long accountId,final DateTime date, final  OnlineHmmPriors priors) {
        final Map<String,byte []> payloads = Maps.newHashMap();
        final String dateString = DateTimeUtil.dateToYmdString(date);

        LOGGER.info("updateModelPriors for user {}",accountId);

        payloads.put(PAYLOAD_KEY_FOR_PARAMS,priors.serializeToProtobuf());

        return dbDAO.update(accountId.toString(), dateString, payloads);
    }

    @Override
    public boolean updateScratchpad(final Long accountId,final DateTime date, final OnlineHmmScratchPad scratchPad) {
        final Map<String,byte []> payloads = Maps.newHashMap();
        final String dateString = DateTimeUtil.dateToYmdString(date);

        LOGGER.info("updateScratchpad for user {}",accountId);

        payloads.put(PAYLOAD_KEY_FOR_SCRATCHPAD,scratchPad.serializeToProtobuf());

        return dbDAO.updateLatest(accountId.toString(), dateString, payloads);

    }

    @Override
    public boolean updateModelPriorsAndZeroOutScratchpad(final Long accountId,final DateTime date, final OnlineHmmPriors priors) {
        final Map<String,byte []> payloads = Maps.newHashMap();
        final String dateString = DateTimeUtil.dateToYmdString(date);

        LOGGER.info("updateModelPriorsAndZeroOutScratchpad for user {}",accountId);

        payloads.put(PAYLOAD_KEY_FOR_PARAMS,priors.serializeToProtobuf());
        payloads.put(PAYLOAD_KEY_FOR_SCRATCHPAD,OnlineHmmProtos.AlphabetHmmScratchPad.newBuilder().setLastDateUpdatedUtc(0).build().toByteArray());

        return dbDAO.update(accountId.toString(),dateString,payloads);
    }



    public static CreateTableResult createTable(final String tableName, final AmazonDynamoDBClient dynamoDBClient) {
        return GeneralProtobufDAODynamoDB.createTable(tableName, HASH_KEY, Optional.of(RANGE_KEY), dynamoDBClient, 1, 1);
    }
}
