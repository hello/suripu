package com.hello.suripu.coredw8.timeline;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.algorithmintegration.AlgorithmConfiguration;
import com.hello.suripu.core.algorithmintegration.NeuralNetAlgorithmOutput;
import com.hello.suripu.core.algorithmintegration.NeuralNetEndpoint;
import com.hello.suripu.core.db.AccountReadDAO;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.DeviceDataReadAllSensorsDAO;
import com.hello.suripu.core.db.DeviceReadDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.FeedbackReadDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.PillDataReadDAO;
import com.hello.suripu.core.db.RingTimeHistoryReadDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.db.SleepScoreParametersDAO;
import com.hello.suripu.core.db.SleepStatsDAO;
import com.hello.suripu.core.db.UserTimelineTestGroupDAO;
import com.hello.suripu.core.db.colors.SenseColorDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.RingTime;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepScore;
import com.hello.suripu.core.models.SleepScoreParameters;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jarredheinrich on 8/10/16.
 */
public class InstrumentedTimelineProcessorHelpers {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedTimelineProcessorHelpers.class);

    final public PillDataReadDAO pillDataReadDAO = new PillDataReadDAO() {
        @Override
        public ImmutableList<TrackerMotion> getBetweenLocalUTC(long accountId, DateTime startLocalTime, DateTime endLocalTime) {
            // from onlineHmmtest getTypicalDayofPill
            final int tzOffset = 0;
            final List<TrackerMotion> trackerMotions = Lists.newArrayList();
            final long tstart = startLocalTime.withZone(DateTimeZone.UTC).getMillis();
            final long tstop = endLocalTime.withZone(DateTimeZone.UTC).getMillis();

            for (long t = tstart; t < tstop; t += 60000L) {
                final double percent = ((double) (t - tstart) / (double) (tstop - tstart));

                if (percent < 0.39 && percent > 0.25) {
                    if (t % 60000L * 30L == 0) {
                        trackerMotions.add(new TrackerMotion(0, 0, 0L, t, 5000, tzOffset, 5000L, 1L, 2L));
                    }
                }
                else if (percent >= 0.33 && percent < 0.66) {
                    if (t % 60000L * 10L == 0) {
                        trackerMotions.add(new TrackerMotion(0, 0, 0L, t, 5000, tzOffset, 5000L, 1L, 1L));
                    }
                }
                else{

                }

            }

            return ImmutableList.copyOf(trackerMotions);
        }
    };


    final public DeviceDataReadAllSensorsDAO deviceDataReadAllSensorsDAO = new DeviceDataReadAllSensorsDAO() {
        @Override
        public AllSensorSampleList generateTimeSeriesByUTCTimeAllSensors(
                Long queryStartTimestampInUTC, Long queryEndTimestampInUTC, Long accountId, String externalDeviceId,
                int slotDurationInMinutes, Integer missingDataDefaultValue, com.google.common.base.Optional<Device.Color> color,
                com.google.common.base.Optional<Calibration> calibrationOptional, final Boolean useAudioPeakEnergy) {

            // from OnlineHmmTest getTypicalDayOfSense
            final DateTime startLocalTime = new DateTime(queryStartTimestampInUTC).withZone(DateTimeZone.UTC);
            final DateTime endLocalTime  = new DateTime(queryEndTimestampInUTC).withZone(DateTimeZone.UTC);
            final int tzOffset = 0;

            final AllSensorSampleList allSensorSampleList = new AllSensorSampleList();

            final List<Sample> light = Lists.newArrayList();
            final List<Sample> soundCount = Lists.newArrayList();
            final List<Sample> waves = Lists.newArrayList();

            final long tstart = startLocalTime.withZone(DateTimeZone.UTC).getMillis();
            final long tstop = endLocalTime.withZone(DateTimeZone.UTC).getMillis();



            for (long t = tstart; t < tstop; t += 60000L) {
                final double percent =  ((double)(t - tstart) / (double)(tstop - tstart));

                if (percent < 0.33) {
                    light.add(new Sample(t,20.0f,tzOffset));
                    waves.add(new Sample(t,0.0f,tzOffset));
                    soundCount.add(new Sample(t,0.0f,tzOffset));
                }
                else if (percent >= 0.33 && percent < 0.66) {
                    light.add(new Sample(t,0.05f,tzOffset));
                    waves.add(new Sample(t,0.0f,tzOffset));
                    soundCount.add(new Sample(t,0.0f,tzOffset));
                }
                else {
                    light.add(new Sample(t,20.0f,tzOffset));
                    waves.add(new Sample(t,0.0f,tzOffset));
                    soundCount.add(new Sample(t,0.0f,tzOffset));
                }
            }

            allSensorSampleList.add(Sensor.LIGHT,light);
            allSensorSampleList.add(Sensor.SOUND_NUM_DISTURBANCES,soundCount);
            allSensorSampleList.add(Sensor.WAVE_COUNT,waves);

            return allSensorSampleList;
        }
    };

    final public RingTimeHistoryReadDAO ringTimeHistoryDAODynamoDB = new RingTimeHistoryReadDAO() {
        @Override
        public List<RingTime> getRingTimesBetween(String senseId, Long accountId, DateTime startTime, DateTime endTime) {
            return null;
        }
    };

    final public FeedbackReadDAO feedbackDAO = new FeedbackReadDAO() {
        @Override
        public ImmutableList<TimelineFeedback> getCorrectedForNight(Long accountId,DateTime dateOfNight) {
            return null;
        }

        @Override
        public ImmutableList<TimelineFeedback> getForTimeRange(Long tstartUTC, Long tstopUTC) {
            return null;
        }
    };

    final public SleepHmmDAO sleepHmmDAO = new SleepHmmDAO() {
        @Override
        public com.google.common.base.Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis) {
            return com.google.common.base.Optional.absent();
        }
    };

    final public AccountReadDAO accountDAO = new AccountReadDAO() {
        @Override
        public com.google.common.base.Optional<Account> getById(Long id) {
            final Account account = new Account.Builder().withDOB("1980-01-01").build();
            return com.google.common.base.Optional.of(account);
        }

        @Override
        public com.google.common.base.Optional<Account> getByEmail(String email) {
            return null;
        }

        @Override
        public List<Account> getRecent(Integer limit) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Account> exists(String email, String password) {
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


    final public SleepStatsDAO sleepStatsDAO = new SleepStatsDAO() {
        @Override
        public Boolean updateStat(Long accountId, DateTime date, Integer overallSleepScore, SleepScore sleepScore, SleepStats stats, Integer offsetMillis) {
            return Boolean.TRUE;
        }

        @Override
        public com.google.common.base.Optional<Integer> getTimeZoneOffset(Long accountId) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Integer> getTimeZoneOffset(Long accountId, DateTime queryDate) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<AggregateSleepStats> getSingleStat(Long accountId, String date) {
            return null;
        }

        @Override
        public ImmutableList<AggregateSleepStats> getBatchStats(Long accountId, String startDate, String endDate) {
            return null;
        }
    };

    final public SenseColorDAO senseColorDAO = new SenseColorDAO() {
        @Override
        public com.google.common.base.Optional<Device.Color> getColorForSense(String senseId) {
            return com.google.common.base.Optional.absent();
        }

        @Override
        public com.google.common.base.Optional<Sense.Color> get(String senseId) {
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

    final public OnlineHmmModelsDAO priorsDAO = new OnlineHmmModelsDAO() {
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

    final public FeatureExtractionModelsDAO featureExtractionModelsDAO = new FeatureExtractionModelsDAO() {
        @Override
        public FeatureExtractionModelData getLatestModelForDate(Long accountId, DateTime dateTimeLocalUTC, com.google.common.base.Optional<UUID> uuidForLogger) {
            return null;
        }
    };

    final public CalibrationDAO calibrationDAO = new CalibrationDAO() {
        @Override
        public com.google.common.base.Optional<Calibration> get(String senseId) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Calibration> getStrict(String senseId) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Boolean> putForce(Calibration calibration) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Boolean> put(Calibration calibration) {
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
        public Map<String, com.google.common.base.Optional<Boolean>> putBatchForce(List<Calibration> calibrations) {
            return null;
        }

        @Override
        public Map<String, com.google.common.base.Optional<Boolean>> putBatch(List<Calibration> calibration) {
            return null;
        }
    };

    final public DefaultModelEnsembleDAO defaultModelEnsembleDAO = new DefaultModelEnsembleDAO() {
        @Override
        public OnlineHmmPriors getDefaultModelEnsemble() {
            return OnlineHmmPriors.createEmpty();
        }

        @Override
        public OnlineHmmPriors getSeedModel() {
            return OnlineHmmPriors.createEmpty();
        }
    };

    final public UserTimelineTestGroupDAO userTimelineTestGroupDAO = new UserTimelineTestGroupDAO() {
        @Override
        public com.google.common.base.Optional<Long> getUserGestGroup(Long accountId, DateTime timeToQueryUTC) {
            return com.google.common.base.Optional.absent();
        }

        @Override
        public void setUserTestGroup(Long accountId, Long groupId) {

        }
    };

    final public MetricRegistry metric = new MetricRegistry();

    /*
    final DeviceReadDAO deviceReadForTimelineDAO = new DeviceReadForTimelineDAO() {
        @Override
        public ImmutableList<DeviceAccountPair> getSensesForAccountId(Long accountId) {
            return null;
        }

        @Override
        public Optional<DeviceAccountPair> getMostRecentSensePairByAccountId(Long accountId) {
            return Optional.of(new DeviceAccountPair(0L,0L,"foobars",new DateTime(0L)));
        }

        @Override
        public Optional<Long> getPartnerAccountId(Long accountId) {
            return Optional.absent();
        }
    };
    */

    final public DeviceReadDAO deviceReadDAO = new DeviceReadDAO() {
        @Override
        public com.google.common.base.Optional<Long> getDeviceForAccountId(@Bind("account_id") Long accountId, @Bind("device_id") String deviceName) {
            return null;
        }

        @Override
        public ImmutableList<DeviceAccountPair> getSensesForAccountId(@Bind("account_id") Long accountId) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Long> getMostRecentSenseByAccountId(@Bind("account_id") Long accountId) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<DeviceAccountPair> getMostRecentSensePairByAccountId(@Bind("account_id") Long accountId) {
            return com.google.common.base.Optional.of(new DeviceAccountPair(0L,0L,"foobars",new DateTime(0L)));
        }

        @Override
        public ImmutableList<DeviceAccountPair> getAccountIdsForDeviceId(@Bind("device_name") String deviceName) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Long> getIdForAccountIdDeviceId(@Bind("account_id") Long accountId, @Bind("device_name") String deviceName) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<Long> getPartnerAccountId(@Bind("account_id") Long accountId) {
            return com.google.common.base.Optional.absent();
        }

        @Override
        public ImmutableList<DeviceAccountPair> getPillsForAccountId(@Bind("account_id") Long accountId) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        @Override
        public ImmutableList<DeviceAccountPair> getLinkedAccountFromPillId(@Bind("pill_id") String deviceId) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        @Override
        public ImmutableList<DeviceAccountPair> getAllPills(@Bind("is_active") Boolean isActive) {
            return null;
        }

        @Override
        public com.google.common.base.Optional<DeviceAccountPair> getInternalPillId(@Bind("pill_id") String pillId) {
            return null;
        }
    };

    final public AlgorithmConfiguration algorithmConfiguration = new AlgorithmConfiguration() {
        @Override
        public int getArtificalLightStartMinuteOfDay() {
            return 21*60 + 30;
        }

        @Override
        public int getArtificalLightStopMinuteOfDay() {
            return 5*60;
        }
    };

    final public SleepScoreParametersDAO sleepScoreParametersDAO = new SleepScoreParametersDAO() {

        @Override
        public SleepScoreParameters getSleepScoreParametersByDate(Long accountId, DateTime nightDate) {
            return new SleepScoreParameters(accountId, nightDate, 480);
        }

        @Override
        public Boolean upsertSleepScoreParameters(Long accountId, SleepScoreParameters parameters) {
            return null;
        }
    };


    public void clear() {
    }

    final public NeuralNetEndpoint neuralNetEndpoint = new NeuralNetEndpoint() {

        @Override
        public com.google.common.base.Optional<NeuralNetAlgorithmOutput> getNetOutput(String netId, double[][] sensorData) {
            return com.google.common.base.Optional.absent();
        }
    };
}