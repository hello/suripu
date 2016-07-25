package com.hello.suripu.core.firmware;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.protobuf.ByteString;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hello.suripu.api.output.OutputProtos.SyncResponse;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.FirmwareVersionMappingDAO;
import com.hello.suripu.core.db.OTAHistoryDAO;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.firmware.db.OTAFileSettingsDAO;
import com.hello.suripu.core.models.OTAHistory;
import com.hello.suripu.core.ota.Status;
import com.hello.suripu.core.util.FeatureUtils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.math3.util.Pair;
import org.bouncycastle.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class FirmwareUpdateStore implements FirmwareUpdateStoreInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirmwareUpdateStore.class);

    private final OTAHistoryDAO otaHistoryDAO;
    private final OTAFileSettingsDAO fileSettingsDAO;
    private final FirmwareVersionMappingDAO firmwareVersionMappingDAO;
    private final FirmwareUpgradePathDAO firmwareUpgradePathDAO;
    private final FirmwareHelper helper;
    private final static String S3_CACHE_CLEAR_GROUP_NAME = "clear_cache";
    private final static String S3_CACHE_CLEAR_VERSION_NUMBER = "666666";

    final Cache<FirmwareCacheKey, FirmwareUpdate> s3FWCache;
    final Cache<GenericFirmwareUpdateQuery, GroupNameRollout> s3ObjectKeyCache;

    public FirmwareUpdateStore(final OTAHistoryDAODynamoDB otaHistoryDAO,
                               final OTAFileSettingsDAO fileSettingsDAO,
                               final FirmwareHelper helper,
                               final Cache<FirmwareCacheKey, FirmwareUpdate> s3FWCache,
                               final FirmwareVersionMappingDAO firmwareVersionMappingDAO,
                               final FirmwareUpgradePathDAO firmwareUpgradePathDAO,
                               final Cache<GenericFirmwareUpdateQuery, GroupNameRollout> s3ObjectKeyCache) {
        this.otaHistoryDAO = otaHistoryDAO;
        this.fileSettingsDAO = fileSettingsDAO;
        this.helper = helper;
        this.s3FWCache = s3FWCache;
        this.firmwareVersionMappingDAO = firmwareVersionMappingDAO;
        this.firmwareUpgradePathDAO = firmwareUpgradePathDAO;
        this.s3ObjectKeyCache = s3ObjectKeyCache;
    }

    public static FirmwareUpdateStore create(final OTAHistoryDAODynamoDB otaHistoryDAO,
                                             final OTAFileSettingsDAO fileSettingsDAO,
                                             final AmazonS3 s3,
                                             final String bucketName,
                                             final AmazonS3 s3Signer,
                                             final Cache<FirmwareCacheKey, FirmwareUpdate> s3Cache,
                                             final FirmwareVersionMappingDAO firmwareVersionMappingDAO,
                                             final FirmwareUpgradePathDAO firmwareUpgradePathDAO,
                                             final Cache<GenericFirmwareUpdateQuery, GroupNameRollout> s3ObjectKeyCache) {

        final FirmwareS3Helper helper = FirmwareS3Helper.create(s3, s3Signer, bucketName);
        return new FirmwareUpdateStore(otaHistoryDAO, fileSettingsDAO, helper, s3Cache, firmwareVersionMappingDAO, firmwareUpgradePathDAO, s3ObjectKeyCache);
    }

    public static FirmwareUpdateStore create(final OTAHistoryDAODynamoDB otaHistoryDAO,
                                             final OTAFileSettingsDAO fileSettingsDAO,
                                             final AmazonS3 s3,
                                             final String bucketName,
                                             final AmazonS3 s3Signer,
                                             final Integer s3CacheExpireMinutes,
                                             final FirmwareVersionMappingDAO firmwareVersionMappingDAO,
                                             final FirmwareUpgradePathDAO firmwareUpgradePathDAO) {
        final Cache<FirmwareCacheKey, FirmwareUpdate> s3Cache = CacheBuilder.newBuilder()
                .expireAfterWrite(s3CacheExpireMinutes, TimeUnit.MINUTES)
                .build();

        final Cache<GenericFirmwareUpdateQuery, GroupNameRollout> s3ObjectKeyCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
        final FirmwareS3Helper helper = FirmwareS3Helper.create(s3, s3Signer, bucketName);
        return new FirmwareUpdateStore(otaHistoryDAO, fileSettingsDAO, helper, s3Cache, firmwareVersionMappingDAO, firmwareUpgradePathDAO, s3ObjectKeyCache);
    }



    public Optional<SyncResponse.FileDownload> makeFileDownload(final String s3Key, final URL signedURL,
            final ImmutableMap<String, FirmwareFile> downloadableFiles) {

        final SyncResponse.FileDownload.Builder fileDownloadBuilder = SyncResponse.FileDownload.newBuilder()
                .setUrl(signedURL.getPath() + "?" + signedURL.getQuery())
                .setHost(signedURL.getHost()); // TODO: replace with hello s3 proxy

        final String normalizedFilename = normalizeFilename(s3Key);
        if(!downloadableFiles.containsKey(normalizedFilename)) {
            return Optional.absent();
        }


        final FirmwareFile fileInfo = downloadableFiles.get(normalizedFilename);
        if (fileInfo.sha1.length() > 0) {
            try {
                final byte[] metaDataSHA = Hex.decodeHex(fileInfo.sha1.toCharArray());
                fileDownloadBuilder.setSha1(ByteString.copyFrom(metaDataSHA));
            } catch (DecoderException de){
                LOGGER.error("error=invalid-metadata-sha filename={}", s3Key);
            }
        } else {
            fileDownloadBuilder.setSha1(ByteString.copyFrom(helper.computeSha1ForS3File(s3Key)));
        }

        final boolean copyToSerialFlash = fileInfo.copyToSerialFlash;
        final boolean resetApplicationProcessor = fileInfo.resetApplicationProcessor;
        final String serialFlashFilename = fileInfo.serialFlashFilename;
        final String serialFlashPath = fileInfo.serialFlashPath;
        final String sdCardFilename = fileInfo.sdCardFilename;
        final String sdCardPath = fileInfo.sdCardPath;

        fileDownloadBuilder.setCopyToSerialFlash(copyToSerialFlash);
        fileDownloadBuilder.setResetApplicationProcessor(resetApplicationProcessor);
        fileDownloadBuilder.setSerialFlashFilename(serialFlashFilename);
        fileDownloadBuilder.setSerialFlashPath(serialFlashPath);
        fileDownloadBuilder.setSdCardFilename(sdCardFilename);
        fileDownloadBuilder.setSdCardPath(sdCardPath);

        return Optional.of(fileDownloadBuilder.build());
    }


    public List<SyncResponse.FileDownload> generateSignedListForFilesInBucket(final SenseFirmwareUpdateQuery query, final Map<String, String> filenameSHAMap, final int expiresInMinutes) {
        final List<SyncResponse.FileDownload> fileDownloadList = new ArrayList<>();
        final ImmutableMap<String, FirmwareFile> availableFiles = fileSettingsDAO.mappingForHardwareVersion(query.hwVersion);

        for(final Entry<String, String> f : filenameSHAMap.entrySet()) {
            final String filename = f.getKey();
            final String fileSHA = f.getValue(); // Optimization not implemented yet

            final URL signedURL = helper.signUrl(filename, expiresInMinutes);

            final SyncResponse.FileDownload.Builder fileDownloadBuilder = SyncResponse.FileDownload.newBuilder()
                    .setUrl(signedURL.getPath() + "?" + signedURL.getQuery())
                    .setHost(signedURL.getHost()); // TODO: replace with hello s3 proxy

            final Optional<SyncResponse.FileDownload> fileDownloadOptional = makeFileDownload(filename, signedURL, availableFiles);
            if(fileDownloadOptional.isPresent()) {
                fileDownloadList.add(fileDownloadOptional.get());
            }
        }

        return  fileDownloadList;
    }

    @Override
    public FirmwareUpdate getFirmwareFilesForCacheKey(final FirmwareCacheKey cacheKey, final SenseFirmwareUpdateQuery query) {

        final int expiresInMinutes = 60;
        final List<S3ObjectSummary> summaryList = helper.summaries(cacheKey, query);
        final Optional<String> fwVersionText = helper.getTextForFirmwareVersion(summaryList);

        if(!fwVersionText.isPresent()) {
            LOGGER.warn("action=return-empty-pair s3_key={}", cacheKey.humanReadableGroupName);
            return FirmwareUpdate.missing();
        }

        final Optional<String> fwVersion = FirmwareBuildInfoParser.parse(fwVersionText.get());
        if(!fwVersion.isPresent()) {
            LOGGER.error("error=failed-to-parse-firmware-vesion-from-text s3_key={}", cacheKey.humanReadableGroupName);
            LOGGER.warn("action=return-empty-pair s3_key={}", cacheKey.humanReadableGroupName);
            return FirmwareUpdate.missing();
        }

        //a map of filename->SHA1 value from S3 metaData
        final Map<String, String> filenameSHAMap = helper.computeFilenameSHAMap(summaryList);

        final List<SyncResponse.FileDownload> fileDownloadList = generateSignedListForFilesInBucket(query, filenameSHAMap, expiresInMinutes);


        final Ordering<SyncResponse.FileDownload> byResetApplicationProcessor = new Ordering<SyncResponse.FileDownload>() {
            public int compare(SyncResponse.FileDownload left, SyncResponse.FileDownload right) {
                return Boolean.compare(left.getResetApplicationProcessor(), right.getResetApplicationProcessor());
            }
        };

        final List<SyncResponse.FileDownload> sortedFiles = byResetApplicationProcessor.sortedCopy(fileDownloadList);

        final FirmwareUpdate fwUpdate = FirmwareUpdate.create(fwVersion.get(), sortedFiles);
        return fwUpdate;
    }

    /**
     * Attempts retrieval of file list for group from S3 cache and compares fw version number to see if update is needed
     * @return
     */
    public FirmwareUpdate getFirmwareUpdate(final SenseFirmwareUpdateQuery senseQuery) {

        final GenericFirmwareUpdateQuery query = GenericFirmwareUpdateQuery.from(senseQuery);

        // Mutable state, here be dragons
        FirmwareUpdate firmwareUpdate = FirmwareUpdate.missing();

        try {
            final Optional<FirmwareCacheKey> firmwareCacheKeyOptional = getCachedS3ObjectForDeviceInGroup(senseQuery);
            if(!firmwareCacheKeyOptional.isPresent()){
                return FirmwareUpdate.missing();
            }

            final FirmwareCacheKey cacheKey = firmwareCacheKeyOptional.get();
//            final GenericFirmwareUpdateQuery humanQuery = GenericFirmwareUpdateQuery.from(senseQuery);
            // This overrides value declared above.
            try {
                firmwareUpdate = s3FWCache.get(cacheKey, new Callable<FirmwareUpdate>() {
                    @Override
                    public FirmwareUpdate call() throws Exception {
                        LOGGER.info("Nothing in cache found for S3 Object Key: [{}]. Grabbing info from S3.", cacheKey.humanReadableGroupName);
                        return getFirmwareFilesForCacheKey(cacheKey, senseQuery);
                    }
                });

            } catch (ExecutionException e) {
                LOGGER.error("Exception while retrieving S3 file list.");
            }

            if(firmwareUpdate.files.isEmpty()) {
                return firmwareUpdate;
            }

            if (isValidFirmwareUpdate(firmwareUpdate, query.firmwareVersion)) {

                if (!isExpiredPresignedUrl(firmwareUpdate.files.get(0).getUrl(), new Date())) {
                    //Store OTA Data
                    final List<String> urlList = new ArrayList<>();
                    for (final SyncResponse.FileDownload fileDL : firmwareUpdate.files) {
                        urlList.add(fileDL.getHost() + fileDL.getUrl());
                    }
                    final DateTime eventTime = new DateTime().toDateTime(DateTimeZone.UTC);
                    final OTAHistory newHistoryEntry = new OTAHistory(senseQuery.senseId, eventTime, senseQuery.currentFirmwareVersion, firmwareUpdate.firmwareVersion, urlList, Status.RESPONSE_SENT);

                    final Optional<OTAHistory> insertedEntry = otaHistoryDAO.insertOTAEvent(newHistoryEntry);
                    if (!insertedEntry.isPresent()) {
                        LOGGER.error("OTA History Insertion Failed: {} => {} for {} at {}", senseQuery.currentFirmwareVersion, query.firmwareVersion, senseQuery.senseId, eventTime);
                    }
                    // TODO refactor to key=value and update papertrail
                    LOGGER.info("{} files added to syncResponse for OTA of '{}' to DeviceId {}", firmwareUpdate.files.size(), cacheKey, senseQuery.senseId);
                    return firmwareUpdate;
                }

                //Cache returned a valid update with an expired URL
                LOGGER.info("Expired URL in S3 Cache. Forcing Cleanup.");
                s3FWCache.cleanUp();
            }

        } catch (Exception ex) {
            LOGGER.error("OTA attempt failed for Device Id: {} on FW version: {}. {}", senseQuery.senseId, senseQuery.currentFirmwareVersion, ex.getMessage());
        }

        return FirmwareUpdate.missing();
    }


    public static boolean isValidFirmwareUpdate(final FirmwareUpdate firmwareUpdate, final String currentFirmwareVersion) {


        if (firmwareUpdate.firmwareVersion.equals("0")) {
            LOGGER.error("Failed to retrieve S3 file list.");
            return false;
        }

        if (firmwareUpdate.firmwareVersion.equals(currentFirmwareVersion)) {
            LOGGER.info("Versions match: {}, current version = {}", firmwareUpdate.firmwareVersion, currentFirmwareVersion);
            return false;
        }

        return true;
    }

    public static boolean isExpiredPresignedUrl(final String presignedUrl, final Date now) {
        final Map<String, String> urlMap = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(presignedUrl.split("\\?")[1]);

        if (now == null) {
            LOGGER.error("Invalid date parameter in pre-signed URL check.");
            return true;
        }

        try {
            final Long expiration = Long.parseLong(urlMap.get("Expires"));
            final Long nowSecs = now.getTime() / 1000L;
            return (nowSecs > expiration);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid pre-signed URL.");
            return true;
        }
    }

    private Optional<FirmwareCacheKey> getCachedS3ObjectForDeviceInGroup(final SenseFirmwareUpdateQuery senseFirmwareUpdateQuery) {

        //TODO: Refactor the way cache clearing is called. 
        if (senseFirmwareUpdateQuery.group.equals(S3_CACHE_CLEAR_GROUP_NAME) && senseFirmwareUpdateQuery.currentFirmwareVersion.equals(S3_CACHE_CLEAR_VERSION_NUMBER)) {
            LOGGER.info("Received command to clear S3 ObjectKey Cache.");
            s3ObjectKeyCache.invalidateAll();
        }

        final GenericFirmwareUpdateQuery query = GenericFirmwareUpdateQuery.from(senseFirmwareUpdateQuery);

        final GroupNameRollout rollout;

        try {
            rollout = s3ObjectKeyCache.get(query, new Callable<GroupNameRollout>() {
                @Override
                public GroupNameRollout call() throws Exception {
                    LOGGER.info("msg=s3-empty group={} fw_version={} hw_version={} action=fetching-new-value.",
                            query.groupName, query.firmwareVersion, query.hardwareVersion);
                    return getS3ObjectAndRolloutPercentForGroup(query);
                }
            });

        } catch (ExecutionException e) {
            LOGGER.error("error=exception-retrieving-s3-file-list group={} fw_version={} hw_version={}",
                    query.groupName, query.firmwareVersion, query.hardwareVersion);
            return Optional.absent();
        }


        if (!FeatureUtils.entityIdHashInPercentRange(senseFirmwareUpdateQuery.senseId, 0.0f, rollout.rolloutPercent)) {
            LOGGER.debug("msg=device-outside-rollout-percentage sense_id={} pct={}.", senseFirmwareUpdateQuery.senseId, rollout.rolloutPercent);
            return Optional.absent();
        }

        final FirmwareCacheKey cacheKey = FirmwareCacheKey.create(rollout.groupName, query.hardwareVersion);
        return Optional.of(cacheKey);
    }

    private GroupNameRollout getS3ObjectAndRolloutPercentForGroup(final GenericFirmwareUpdateQuery query) {


        //Retrieve destination fw version
        final Optional<Pair<String, Float>> nextFirmwareVersion = firmwareUpgradePathDAO.getNextFWVersionForGroup(
                query.groupName,
                query.firmwareVersion
        );

        final List<String> humanNames = new ArrayList<>();

        if (nextFirmwareVersion.isPresent()) {
            //Get human-readable name(s) from firmwareVersionMappingDAO & use this for the S3 FW object
            humanNames.addAll(firmwareVersionMappingDAO.get(nextFirmwareVersion.get().getKey()));
        }

        if (humanNames.isEmpty()) {
            LOGGER.debug("No non-hashed fw version exists, defaulting to '{}'.", query.groupName);
            return GroupNameRollout.defaultValue(query.groupName);
        }

        final Float rolloutPercent = nextFirmwareVersion.get().getValue();
        final GroupNameRollout rollout = GroupNameRollout.create(humanNames.get(0), rolloutPercent);

        LOGGER.info("Found upgrade path {} => {}({}) for group: {}", query.firmwareVersion, nextFirmwareVersion.get().getKey(), rollout.groupName, query.groupName);
        return rollout;
    }


    private static String normalizeFilename(final String filename) {
        if(!filename.endsWith(".raw")) {
            String[] parts = Strings.split(filename,'/');
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }

            LOGGER.warn("action=normalize_filename invalid_filename={}", filename);
            return filename;

        }
        // Trimming filename down to 8.3 format for *.raw files with original filenames
        // of the format SENSE_SLEEPTONES_<foo>.raw (e.g. "SENSE_SLEEPTONES_OUTERSPACE.raw")
        final String filenamePre = filename.substring(filename.lastIndexOf("_") + 1)
                .replace(".raw", "");
        return filenamePre.substring(0, Math.min(8, filenamePre.length()));
    }

    private static class DeviceGroupFW {
        public final String deviceId;
        public final String groupName;
        public final Integer fwVersion;

        public DeviceGroupFW(final String deviceId, final String groupName, final Integer fwVersion) {
            this.fwVersion = fwVersion;
            this.groupName = groupName;
            this.deviceId = deviceId;
        }
    }


}