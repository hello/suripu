package com.hello.suripu.app.cli;

import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.processors.TimelineProcessor;
import com.hello.suripu.core.util.DateTimeUtil;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by pangwu on 4/24/15.
 */
public class ScanInvalidNightsCommand extends ConfiguredCommand<SuripuAppConfiguration> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ScanInvalidNightsCommand.class);

    public ScanInvalidNightsCommand(){
        super("invalid_nights", "Get all invalid nights from old filters.");
    }

    @Override
    protected void run(final Bootstrap<SuripuAppConfiguration> bootstrap, final Namespace namespace, final SuripuAppConfiguration configuration) throws Exception {
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource commonDataSource = managedDataSourceFactory.build(configuration.getCommonDB());

        final DBI commonJDBI = new DBI(commonDataSource);
        commonJDBI.registerArgumentFactory(new OptionalArgumentFactory(configuration.getCommonDB().getDriverClass()));
        commonJDBI.registerContainerFactory(new ImmutableListContainerFactory());
        commonJDBI.registerContainerFactory(new ImmutableSetContainerFactory());
        commonJDBI.registerContainerFactory(new OptionalContainerFactory());
        commonJDBI.registerArgumentFactory(new JodaArgumentFactory());

        final AccountDAOImpl accountDAO = commonJDBI.onDemand(AccountDAOImpl.class);

        final ManagedDataSource sensorDataSource = managedDataSourceFactory.build(configuration.getSensorsDB());

        final DBI sensorJDBI = new DBI(sensorDataSource);
        sensorJDBI.registerArgumentFactory(new OptionalArgumentFactory(configuration.getSensorsDB().getDriverClass()));
        sensorJDBI.registerContainerFactory(new ImmutableListContainerFactory());
        sensorJDBI.registerContainerFactory(new ImmutableSetContainerFactory());
        sensorJDBI.registerContainerFactory(new OptionalContainerFactory());
        sensorJDBI.registerArgumentFactory(new JodaArgumentFactory());

        final TrackerMotionDAO trackerMotionDAO = sensorJDBI.onDemand(TrackerMotionDAO.class);
        printInvalidNights(accountDAO, trackerMotionDAO);

    }

    private static int getDataForNight(final Long accountId, final DateTime targetDateLocalUTC, final TrackerMotionDAO trackerMotionDAO){
        final DateTime startQueryTimeLocalUTC = targetDateLocalUTC.withTimeAtStartOfDay().withHourOfDay(DateTimeUtil.DAY_STARTS_AT_HOUR);
        final DateTime endQueryTimeLocalUTC = targetDateLocalUTC.withTimeAtStartOfDay().plusDays(1).withHourOfDay(DateTimeUtil.DAY_ENDS_AT_HOUR);
        return trackerMotionDAO.getDataCountBetweenLocalUTC(accountId, startQueryTimeLocalUTC, endQueryTimeLocalUTC);
    }

    private static void printInvalidNights(final AccountDAOImpl accountDAO, final TrackerMotionDAO trackerMotionDAO){
        final List<Account> allAccounts = accountDAO.getAll();
        final DateTimeZone runnerTimeZone = DateTimeZone.forID("America/Los_Angeles");
        LOGGER.info("Start to scan...");
        for(final Account account:allAccounts){
            final DateTime startSearchDateLocalUTC = new DateTime(account.created, DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(1);
            DateTime targetDateLocalUTC = startSearchDateLocalUTC;
            while(!targetDateLocalUTC.isAfter(DateTime.now().withTimeAtStartOfDay().plusDays(1))){
                final int count = getDataForNight(account.id.get(), targetDateLocalUTC, trackerMotionDAO);
                if(count < TimelineProcessor.MIN_TRACKER_MOTION_COUNT && count > 0){
                    LOGGER.info("{},{}", account.id.get(), DateTimeUtil.dateToYmdString(targetDateLocalUTC));
                }

                targetDateLocalUTC = targetDateLocalUTC.plusDays(1);
                final DateTime localToday = DateTime.now().withZone(runnerTimeZone).withTimeAtStartOfDay();
                final DateTime todayLocalUTC = new DateTime(localToday.getYear(),
                        localToday.getMonthOfYear(),
                        localToday.getDayOfMonth(),
                        0, 0, 0, DateTimeZone.UTC);
                if(!targetDateLocalUTC.isBefore(todayLocalUTC)){
                    break;
                }
            }
        }

        LOGGER.info("Scan finished.");

    }
}
