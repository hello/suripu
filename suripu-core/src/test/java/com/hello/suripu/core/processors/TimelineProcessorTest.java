package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DeviceDataReadAllSensorsDAO;
import com.hello.suripu.core.db.DeviceReadForTimelineDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.PillDataReadDAO;
import com.hello.suripu.core.db.RingTimeHistoryReadDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepStatsDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.MotionScore;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TimelineResult;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 1/21/16.
 */
public class TimelineProcessorTest {

    final PillDataReadDAO pillDataReadDAO = new PillDataReadDAO() {
        @Override
        public ImmutableList<TrackerMotion> getBetweenLocalUTC(long accountId, DateTime startLocalTime, DateTime endLocalTime) {
            return null;
        }
    };


    final DeviceDataReadAllSensorsDAO deviceDataReadAllSensorsDAO = new DeviceDataReadAllSensorsDAO() {
        @Override
        public AllSensorSampleList generateTimeSeriesByUTCTimeAllSensors(Long queryStartTimestampInUTC, Long queryEndTimestampInUTC, Long accountId, String externalDeviceId, int slotDurationInMinutes, Integer missingDataDefaultValue, Optional<Device.Color> color, Optional<Calibration> calibrationOptional) {
            return null;
        }
    };

    final RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB = new RingTimeHistoryReadDAO() {
        @Override
        public List<RingTime> getRingTimesBetween(String senseId, Long accountId, DateTime startTime, DateTime endTime) {
            return null;
        }
    };

    final FeedbackReadDAO feedbackDAO = new FeedbackReadDAO() {
        @Override
        public ImmutableList<TimelineFeedback> getCorrectedForNight(@Bind("account_id") Long accountId, @Bind("date_of_night") DateTime dateOfNight) {
            return null;
        }

        @Override
        public ImmutableList<TimelineFeedback> getForTimeRange(@Bind("start_time") Long tstartUTC, @Bind("stop_time") Long tstopUTC) {
            return null;
        }
    };

    final SleepHmmDAO sleepHmmDAO = new SleepHmmDAO() {
        @Override
        public Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis) {
            return null;
        }
    };

    final AccountReadDAO accountDAO = new AccountReadDAO() {
        @Override
        public Optional<Account> getById(Long id) {
            return null;
        }

        @Override
        public Optional<Account> getByEmail(String email) {
            return null;
        }

        @Override
        public List<Account> getRecent(Integer limit) {
            return null;
        }

        @Override
        public Optional<Account> exists(String email, String password) {
            return null;
        }

        @Override
        public List<Account> getByNamePartial(String namePartial) {
            return null;
        }

        @Override
        public List<Account> getByEmailPartial(String emailPartial) {
            return null;
        }
    };


    final SleepStatsDAO sleepStatsDAO = new SleepStatsDAO() {
        @Override
        public Boolean updateStat(Long accountId, DateTime date, Integer sleepScore, MotionScore motionScore, SleepStats stats, Integer offsetMillis) {
            return null;
        }

        @Override
        public Optional<Integer> getTimeZoneOffset(Long accountId) {
            return null;
        }

        @Override
        public Optional<Integer> getTimeZoneOffset(Long accountId, DateTime queryDate) {
            return null;
        }

        @Override
        public Optional<AggregateSleepStats> getSingleStat(Long accountId, String date) {
            return null;
        }

        @Override
        public ImmutableList<AggregateSleepStats> getBatchStats(Long accountId, String startDate, String endDate) {
            return null;
        }
    };

    final SenseColorDAO senseColorDAO = new SenseColorDAO() {
        @Override
        public Optional<Device.Color> getColorForSense(String senseId) {
            return null;
        }

        @Override
        public Optional<Sense.Color> get(String senseId) {
            return null;
        }

        @Override
        public int saveColorForSense(String senseId, String color) {
            return 0;
        }

        @Override
        public int update(String senseId, String color) {
            return 0;
        }

        @Override
        public ImmutableList<String> missing() {
            return null;
        }
    };

    final OnlineHmmModelsDAO priorsDAO = new OnlineHmmModelsDAO() {
        @Override
        public OnlineHmmData getModelDataByAccountId(Long accountId, DateTime date) {
            return null;
        }

        @Override
        public boolean updateModelPriorsAndZeroOutScratchpad(Long accountId, DateTime date, OnlineHmmPriors priors) {
            return false;
        }

        @Override
        public boolean updateScratchpad(Long accountId, DateTime date, OnlineHmmScratchPad scratchPad) {
            return false;
        }
    };

    final FeatureExtractionModelsDAO featureExtractionModelsDAO = new FeatureExtractionModelsDAO() {
        @Override
        public FeatureExtractionModelData getLatestModelForDate(Long accountId, DateTime dateTimeLocalUTC, Optional<UUID> uuidForLogger) {
            return null;
        }
    };

    final CalibrationDAO calibrationDAO = new CalibrationDAO() {
        @Override
        public Optional<Calibration> get(String senseId) {
            return null;
        }

        @Override
        public Optional<Calibration> getStrict(String senseId) {
            return null;
        }

        @Override
        public Optional<Boolean> putForce(Calibration calibration) {
            return null;
        }

        @Override
        public Optional<Boolean> put(Calibration calibration) {
            return null;
        }

        @Override
        public Map<String, Calibration> getBatch(Set<String> senseIds) {
            return null;
        }

        @Override
        public Map<String, Calibration> getBatchStrict(Set<String> senseIds) {
            return null;
        }

        @Override
        public Boolean delete(String senseId) {
            return null;
        }

        @Override
        public Map<String, Optional<Boolean>> putBatchForce(List<Calibration> calibrations) {
            return null;
        }

        @Override
        public Map<String, Optional<Boolean>> putBatch(List<Calibration> calibration) {
            return null;
        }
    };

    final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new DefaultModelEnsembleDAO() {
        @Override
        public OnlineHmmPriors getDefaultModelEnsemble() {
            return null;
        }

        @Override
        public OnlineHmmPriors getSeedModel() {
            return null;
        }
    };

    final UserTimelineTestGroupDAO userTimelineTestGroupDAO = new UserTimelineTestGroupDAO() {
        @Override
        public Optional<Long> getUserGestGroup(Long accountId, DateTime timeToQueryUTC) {
            return null;
        }

        @Override
        public void setUserTestGroup(Long accountId, Long groupId) {

        }
    };

    final DeviceReadForTimelineDAO deviceReadForTimelineDAO = new DeviceReadForTimelineDAO() {
        @Override
        public ImmutableList<DeviceAccountPair> getSensesForAccountId(@Bind("account_id") Long accountId) {
            return null;
        }

        @Override
        public Optional<DeviceAccountPair> getMostRecentSensePairByAccountId(@Bind("account_id") Long accountId) {
            return null;
        }

        @Override
        public Optional<Long> getPartnerAccountId(@Bind("account_id") Long accountId) {
            return null;
        }
    };

    @Test
    public void testTimelineProcessorSimple() {

        final TimelineProcessor timelineProcessor = TimelineProcessor.createTimelineProcessor(
                pillDataReadDAO,deviceReadForTimelineDAO,deviceDataReadAllSensorsDAO,
                ringTimeHistoryDAODynamoDB,feedbackDAO,sleepHmmDAO,accountDAO,sleepStatsDAO,
                senseColorDAO,priorsDAO,featureExtractionModelsDAO,calibrationDAO,
                defaultModelEnsembleDAO,userTimelineTestGroupDAO);


        final TimelineResult timelineResult = timelineProcessor.retrieveTimelinesFast(0L,DateTime.now(),Optional.<TimelineFeedback>absent());
    }
}
