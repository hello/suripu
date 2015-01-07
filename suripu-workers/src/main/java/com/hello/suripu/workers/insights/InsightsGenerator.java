package com.hello.suripu.workers.insights;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason;
import com.amazonaws.services.kinesis.model.Record;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.processors.InsightProcessor;
import com.hello.suripu.core.processors.insights.LightData;
import com.hello.suripu.workers.framework.HelloBaseRecordProcessor;
import com.yammer.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by kingshy on 1/6/15.
 */
public class InsightsGenerator extends HelloBaseRecordProcessor {
    private final static Logger LOGGER = LoggerFactory.getLogger(InsightsGenerator.class);

    private final InsightProcessor insightProcessor;
    private final AccountDAO accountDAO;

    public InsightsGenerator(final AccountDAO accountDAO,
                             final DeviceDataDAO deviceDataDAO,
                             final DeviceDAO deviceDAO,
                             final TrackerMotionDAO trackerMotionDAO,
                             final AggregateSleepScoreDAODynamoDB scoreDAODynamoDB,
                             final InsightsDAODynamoDB insightsDAODynamoDB,
                             final LightData lightData) {
        this.accountDAO = accountDAO;
        this.insightProcessor = new InsightProcessor(deviceDataDAO, deviceDAO, trackerMotionDAO, scoreDAODynamoDB, insightsDAODynamoDB, lightData);
    }

    @Override
    public void initialize(String s) {

    }

    @Timed
    @Override
    public void processRecords(List<Record> records, IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
    }

    @Override
    public void shutdown(IRecordProcessorCheckpointer iRecordProcessorCheckpointer, ShutdownReason shutdownReason) {
        LOGGER.warn("SHUTDOWN: {}", shutdownReason.toString());
    }

}
