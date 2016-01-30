package com.hello.suripu.workers.pillstatus;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.PillClassificationDAO;
import com.hello.suripu.core.db.PillHeartBeatDAO;

/**
 * Created by pangwu on 6/23/15.
 */
public class PillStatusRecordProcessorFactory implements IRecordProcessorFactory {
    private final PillHeartBeatDAO pillHeartBeatDAO;
    private final PillClassificationDAO pillClassificationDAO;
    private final DeviceDAO deviceDAO;


    public PillStatusRecordProcessorFactory(final DeviceDAO deviceDAO,
                                            final PillClassificationDAO pillClassificationDAO,
                                            final PillHeartBeatDAO pillHeartBeatDAO){
        this.pillHeartBeatDAO = pillHeartBeatDAO;
        this.pillClassificationDAO = pillClassificationDAO;
        this.deviceDAO = deviceDAO;
    }

    @Override
    public IRecordProcessor createProcessor() {
        final IRecordProcessor recordProcessor = new PillStatusRecordProcessor(this.deviceDAO,
                this.pillHeartBeatDAO,
                this.pillClassificationDAO);
        return recordProcessor;
    }
}
