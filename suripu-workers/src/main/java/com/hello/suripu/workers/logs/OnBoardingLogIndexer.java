package com.hello.suripu.workers.logs;

import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.db.OnBoardingLogDAO;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by pangwu on 5/4/15.
 */
public class OnBoardingLogIndexer implements LogIndexer<LoggingProtos.BatchLogMessage> {

    private final OnBoardingLogDAO onBoardingLogDAO;
    private final List<LoggingProtos.RegistrationLog> logs = new LinkedList<>();
    public OnBoardingLogIndexer(final OnBoardingLogDAO onBoardingLogDAO){
        this.onBoardingLogDAO = onBoardingLogDAO;
    }

    @Override
    public void collect(final LoggingProtos.BatchLogMessage batchLogMessage) {
        synchronized (this.logs) {
            this.logs.addAll(batchLogMessage.getRegistrationLogList());
        }
    }

    @Override
    public Integer index() {
        synchronized (this.logs) {
            final Integer insertedCount = this.onBoardingLogDAO.batchInsertOnBoardingLog(this.logs);
            this.logs.clear();
            return insertedCount;
        }
    }
}
