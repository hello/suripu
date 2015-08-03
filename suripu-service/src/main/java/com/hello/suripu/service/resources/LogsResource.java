package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.logging.LogProtos;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.service.SignedMessage;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.io.IOException;

@Path("/logs")
public class LogsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogsResource.class);
    private final KeyStore senseKeyStore;
    private final DataLogger dataLogger;
    private final Boolean isProd;

    @Context
    HttpServletRequest request;

    public LogsResource(final Boolean isProd, final KeyStore senseKeyStore, final DataLogger dataLogger) {
        this.isProd = isProd;
        this.senseKeyStore = senseKeyStore;
        this.dataLogger = dataLogger;

    }

    @Timed
    @POST
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public void saveLogs(byte[] body) {

        final SignedMessage signedMessage = SignedMessage.parse(body);
        LogProtos.sense_log log;

        String debugSenseId = this.request.getHeader(HelloHttpHeader.SENSE_ID);
        if(debugSenseId == null){
            debugSenseId = "";
        }

        LOGGER.info("DebugSenseId device_id = {}", debugSenseId);

        try {
            log = LogProtos.sense_log.parseFrom(signedMessage.body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf for deviceId = %s: %s", debugSenseId, exception.getMessage());
            LOGGER.error(errorMessage);
            return;
        }

        // get MAC address of morpheus

        if(!log.hasDeviceId()){
            LOGGER.error("Cannot get morpheus id (debugSenseId = {})", debugSenseId);
            return;
        }

        final Optional<byte[]> keyBytes = senseKeyStore.get(log.getDeviceId());
        if(!keyBytes.isPresent()) {
            LOGGER.warn("No AES key found for device = {} (debugSenseId = {})", log.getDeviceId(), debugSenseId);
            return;
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytes.get());

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            return;
        }

        final String middleFWVersion = (request.getHeader(HelloHttpHeader.MIDDLE_FW_VERSION) == null) ? "" : request.getHeader(HelloHttpHeader.MIDDLE_FW_VERSION);
        final String topFWVersion = (request.getHeader(HelloHttpHeader.TOP_FW_VERSION) == null) ? "" : request.getHeader(HelloHttpHeader.TOP_FW_VERSION);

        final LoggingProtos.LogMessage logMessage = LoggingProtos.LogMessage.newBuilder()
                .setMessage(log.getText())
                .setOrigin("sense")
                .setTs(log.getUnixTime())
                .setDeviceId(log.getDeviceId())
                .setProduction(isProd)
                .setMiddleFwVersion(middleFWVersion)
                .setTopFwVersion(topFWVersion)
                .build();


        // This is confusing, but somewhat required to avoid bringing too much code into FW
        // We manually map one type of log to another
        final LoggingProtos.BatchLogMessage.LogType logType = (LogProtos.LogType.KEY_VALUE.equals(log.getProperty()))
                ? LoggingProtos.BatchLogMessage.LogType.STRUCTURED_SENSE_LOG
                : LoggingProtos.BatchLogMessage.LogType.SENSE_LOG;

        final LoggingProtos.BatchLogMessage batch = LoggingProtos.BatchLogMessage.newBuilder()
                .addMessages(logMessage)
                .setLogType(logType)
                .setReceivedAt(DateTime.now(DateTimeZone.UTC).getMillis())
                .build();
        try {
            dataLogger.put(log.getDeviceId(), batch.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Failed saving logs to kinesis stream: {}", e.getMessage());
        }
    }
}
