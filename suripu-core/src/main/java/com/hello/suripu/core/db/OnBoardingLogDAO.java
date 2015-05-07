package com.hello.suripu.core.db;

import com.hello.suripu.api.logging.LoggingProtos;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pangwu on 4/30/15.
 */
public abstract class OnBoardingLogDAO {

    private static Logger LOGGER = LoggerFactory.getLogger(OnBoardingLogDAO.class);
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO onboarding_logs (sense_id, account_id, pill_id, utc_ts, info, result, operation, ip) " +
            "VALUES (:sense_id, :account_id, :pill_id, :utc_ts, :info, :result, :operation, :ip)")
    public abstract Long insert(@Bind("sense_id") String senseId,
                     @Bind("account_id") Long accountId,
                     @Bind("pill_id") String pillId,
                     @Bind("utc_ts") DateTime timestamp,
                     @Bind("info") String info,
                     @Bind("result") String result,
                     @Bind("operation") String operation,
                     @Bind("ip") String ip
    );


    @SqlBatch("INSERT INTO onboarding_logs (sense_id, account_id, pill_id, utc_ts, info, result, operation, ip) " +
            "VALUES (:sense_id, :account_id, :pill_id, :utc_ts, :info, :result, :operation, :ip)")
    public abstract void batchInsert(@Bind("sense_id") List<String> senseId,
                            @Bind("account_id") List<Long> accountId,
                            @Bind("pill_id") List<String> pillId,
                            @Bind("utc_ts") List<DateTime> timestamp,
                            @Bind("info") List<String> info,
                            @Bind("result") List<String> result,
                            @Bind("operation") List<String> operation,
                            @Bind("ip") List<String> ip
    );


    @Timed
    public int batchInsertOnBoardingLog(final List<LoggingProtos.RegistrationLog> onBoardingLogs) {

        final List<Long> accountIds = new ArrayList<>();
        final List<String> senseIds = new ArrayList<>();
        final List<DateTime> timestamps = new ArrayList<>();
        final List<String> info = new ArrayList<>();
        final List<String> results = new ArrayList<>();
        final List<String> operations = new ArrayList<>();
        final List<String> ips = new ArrayList<>();
        final List<String> pillIds = new ArrayList<>();


        for(final LoggingProtos.RegistrationLog registrationLog:onBoardingLogs){
            accountIds.add(registrationLog.hasAccountId() ? registrationLog.getAccountId() : null);
            senseIds.add(registrationLog.getSenseId());
            timestamps.add(new DateTime(registrationLog.getTimestamp(), DateTimeZone.UTC));

            pillIds.add(registrationLog.hasPillId() ? registrationLog.getPillId() : null);
            info.add(registrationLog.hasInfo() ? registrationLog.getInfo() : null);
            results.add(registrationLog.getResult());
            operations.add(registrationLog.getAction());
            ips.add(registrationLog.getIpAddress());
            LOGGER.info("sense_id {}, account_id {}, ts {}",
                    registrationLog.getSenseId(),
                    registrationLog.getAccountId(),
                    registrationLog.getTimestamp());
        }


        try{
            this.batchInsert(senseIds, accountIds, pillIds, timestamps, info, results, operations, ips);
            return accountIds.size();
        } catch (UnableToExecuteStatementException ex){
            LOGGER.error("Unable to insert batch onboarding log, duplication.");
            final int logInserted = this.batchInsertBySingleOperation(onBoardingLogs);
            LOGGER.info("Insert {} records using single record insert", logInserted);
            return logInserted;
        } catch (Exception exp){
            LOGGER.error("Unable to perform batch insert, error {}", exp.getMessage());
            return 0;
        }
    }

    private int batchInsertBySingleOperation(final List<LoggingProtos.RegistrationLog> registrationLogs) {
        int inserted = 0;
        for (final LoggingProtos.RegistrationLog registrationLog : registrationLogs) {
            try {
                LOGGER.debug("individual insert {}", registrationLog);
                final Long id = this.insert(registrationLog.getSenseId(),
                        registrationLog.getAccountId(),
                        registrationLog.hasPillId() ? registrationLog.getPillId() : null,
                        new DateTime(registrationLog.getTimestamp(), DateTimeZone.UTC),
                        registrationLog.getInfo(),
                        registrationLog.getResult(),
                        registrationLog.getAction(),
                        registrationLog.getIpAddress()
                );

                inserted++;

                if (id == null) {
                    LOGGER.warn("id is null");
                    continue;
                }
            } catch (UnableToExecuteStatementException exception) {
                final Matcher matcher = PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (matcher.find()) {
                    LOGGER.debug("Dupe: Account {}, sense Id {}, action {}, time {}",
                            registrationLog.getAccountId(),
                            registrationLog.getSenseId(),
                            registrationLog.getAction(),
                            registrationLog.getTimestamp());
                }
                LOGGER.error("Insert onboarding log for account {}, sense {}, ts {} failed, sql error {}",
                        registrationLog.getAccountId(),
                        registrationLog.getSenseId(),
                        registrationLog.getTimestamp(),
                        exception.getMessage());
            } catch (Exception exp){
                LOGGER.error("Insert onboarding log for account {}, sense {}, ts {} failed, error {}",
                        registrationLog.getAccountId(),
                        registrationLog.getSenseId(),
                        registrationLog.getTimestamp(),
                        exp.getMessage());
            }
        }
        return inserted;
    }

}
