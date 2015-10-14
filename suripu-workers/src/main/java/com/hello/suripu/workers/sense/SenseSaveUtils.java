package com.hello.suripu.workers.sense;


import com.amazonaws.AmazonClientException;
import com.google.common.collect.Maps;
import com.hello.suripu.api.input.DataInputProtos;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.models.UserInfo;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SenseSaveUtils {
    private final static Logger LOGGER = LoggerFactory.getLogger(SenseSaveUtils.class);

    public static Map<Long, DateTimeZone> getTimezonesByUser(final String senseExternalId, final DataInputProtos.BatchPeriodicDataWorker batchPeriodicDataWorker, final List<Long> accountsList, final Timer fetchTimezones, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB) {
        final TimerContext context = fetchTimezones.time();
        try {


            final Map<Long, DateTimeZone> map = Maps.newHashMap();
            for (final DataInputProtos.AccountMetadata accountMetadata : batchPeriodicDataWorker.getTimezonesList()) {
                map.put(accountMetadata.getAccountId(), DateTimeZone.forID(accountMetadata.getTimezone()));
            }

            for (final Long accountId : accountsList) {
                if (!map.containsKey(accountId)) {
                    LOGGER.warn("Found account_id {} in account_device_map but not in alarm_info for sense {}", accountId, senseExternalId);
                }
            }

            // Kinesis, DynamoDB and Postgres have a consistent view of accounts
            // move on
            if (!map.isEmpty() && map.size() == accountsList.size()){
                return map;
            }


            // At this point we need to go to dynamoDB
            LOGGER.warn("Querying dynamoDB. One or several timezones not found in Kinesis message for sense {}.", senseExternalId);

            int retries = 2;
            for (int i = 0; i < retries; i++) {
                try {
                    final List<UserInfo> userInfoList = mergedUserInfoDynamoDB.getInfo(senseExternalId);
                    for (UserInfo userInfo : userInfoList) {
                        if (userInfo.timeZone.isPresent()) {
                            map.put(userInfo.accountId, userInfo.timeZone.get());
                        }
                    }
                    break;
                } catch (AmazonClientException exception) {
                    LOGGER.error("Failed getting info from DynamoDB for device = {}", senseExternalId);
                }

                try {
                    LOGGER.warn("Sleeping for 1 sec");
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    LOGGER.warn("Thread sleep interrupted");
                }
                retries++;
            }

            return map;
        } finally {
            context.stop();
        }
    }
}
