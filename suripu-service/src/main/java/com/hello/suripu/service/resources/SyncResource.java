package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.crypto.CryptoHelper;
import com.hello.suripu.core.db.PublicKeyStore;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/sync")
public class SyncResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncResource.class);

    private final PublicKeyStore publicKeyStore;
    private final CryptoHelper cryptoHelper;

    public SyncResource(final PublicKeyStore publicKeyStore) {
        this.publicKeyStore = publicKeyStore;
        this.cryptoHelper = new CryptoHelper(); // TODO: make this injected by controller
    }


    @POST
    @Timed
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response sync(@Valid InputProtos.SyncRequest syncRequest) {

        final Optional<byte[]> optionalPublicKeyBase64Encoded = publicKeyStore.get(syncRequest.getDeviceId());
        if(!optionalPublicKeyBase64Encoded.isPresent()) {
            LOGGER.warn("Public key wasn't found for device id: {}", syncRequest.getDeviceId());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final boolean verified = cryptoHelper.validate(
                syncRequest.getDeviceId().getBytes(),
                syncRequest.getSignature().toByteArray(),
                optionalPublicKeyBase64Encoded.get()
        );

        if(!verified) {
            LOGGER.warn("Signature did not match for device_id = {}", syncRequest.getDeviceId());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }


        // TODO: FETCH DATA FROM DYNAMODB OR SIMILAR STORE

        final InputProtos.SyncResponse.Alarm alarm = InputProtos.SyncResponse.Alarm.newBuilder()
                .setStartTime((int) DateTime.now().getMillis() / 1000)
                .setEndTime((int) DateTime.now().plusMinutes(1).getMillis() / 1000)
                .build();


        final int ledFlashStartTime = (int) DateTime.now().getMillis() / 1000;
        final int ledFlashEndTime = (int) DateTime.now().plusSeconds(10).getMillis() / 1000;

        final InputProtos.SyncResponse.FlashAction.LEDAction ledAction = InputProtos.SyncResponse.FlashAction.LEDAction.newBuilder()
                .setColor(0xFFCC66)
                .setStartTime(ledFlashStartTime)
                .setEndTime(ledFlashEndTime)
                .build();

        final InputProtos.SyncResponse.FlashAction flashAction = InputProtos.SyncResponse.FlashAction.newBuilder()
                .setLed1(ledAction)
                .setLed2(ledAction)
                .setLed3(ledAction)
                .setLed4(ledAction)
                .setLed5(ledAction)
                .build();

        final InputProtos.SyncResponse response = InputProtos.SyncResponse.newBuilder()
                .setAlarm(alarm)
                .setFlashAction(flashAction)
                .build();

        final byte[] responseBytes = response.toByteArray();

        final Optional<byte[]> encryptedContent = CryptoHelper.encrypt(responseBytes, optionalPublicKeyBase64Encoded.get());
        if(!encryptedContent.isPresent()) {
            return Response.serverError().build();
        }

        return Response.ok().entity(encryptedContent.get()).build();
    }
}
