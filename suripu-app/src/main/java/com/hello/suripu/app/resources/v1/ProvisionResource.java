package com.hello.suripu.app.resources.v1;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.provision.PillBlobProvision;
import com.hello.suripu.core.provision.PillProvision;
import com.hello.suripu.core.provision.PillProvisionDAO;
import com.hello.suripu.core.util.KeyStoreUtils;
import com.hello.suripu.core.util.SenseProvision;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;

import static com.google.common.base.Preconditions.checkNotNull;

@Path("/v1/provision")
public class ProvisionResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResources.class);

    private final KeyStore senseKeyStore;
    private final KeyStore pillKeyStore;
    private final KeyStoreUtils keyStoreUtils;
    private final PillProvisionDAO pillProvisionDAO;
    private final AmazonS3 s3;

    @Context
    HttpServletRequest request;

    public ProvisionResource(final KeyStore senseKeyStore,
                             final KeyStore pillKeyStore,
                             final KeyStoreUtils keyStoreUtils,
                             final PillProvisionDAO pillProvisionDAO,
                             final AmazonS3 s3) {
        this.senseKeyStore = senseKeyStore;
        this.pillKeyStore = pillKeyStore;
        this.keyStoreUtils = keyStoreUtils;
        this.pillProvisionDAO = pillProvisionDAO;
        this.s3 = s3;
    }


    @POST
    @Path("{serial_number}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response decrypt(@PathParam("serial_number") final String serialNumber, final byte[] body) throws Exception {

        // we don't try catch this because we want the whole thing to fail if we can't persist this
        final String ipAddress = (request.getHeader("X-Forwarded-For") == null) ? "" : request.getHeader("X-Forwarded-For");
        final Long nowInMillis = DateTime.now(DateTimeZone.UTC).getMillis();
        final LoggingProtos.ProvisionRequest pr = LoggingProtos.ProvisionRequest.newBuilder()
                .setBody(ByteString.copyFrom(body))
                .setSerialNumber(serialNumber)
                .setTs(nowInMillis)
                .setIpAddress(ipAddress)
                .build();


        byte[] buffer = pr.toByteArray();
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
        final String key = String.format("%s-%s", serialNumber, nowInMillis);
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(MediaType.TEXT_PLAIN);
        metadata.setContentLength(buffer.length);
        s3.putObject("hello-provision-blobs", key, byteArrayInputStream, metadata);
        byteArrayInputStream.close();


        try{
            final Optional<SenseProvision> sense = keyStoreUtils.decrypt(body);
            if(!sense.isPresent()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("KO").type(MediaType.TEXT_PLAIN).build();
            }

            if(sense.isPresent()) {
                final SenseProvision senseProvision = sense.get();
                senseKeyStore.put(senseProvision.deviceIdHex, senseProvision.aesKeyHex, serialNumber);
                final StringBuilder sb = new StringBuilder();
                sb.append("OK\n");
                sb.append(sense.get().deviceIdHex + "\n");
                return Response.ok().entity(sb.toString()).type(MediaType.TEXT_PLAIN).build();
            }
        } catch (Exception e) {
            LOGGER.error("Exception while provisioning Sense: {}", e.getMessage());
            LOGGER.error("Body was : {}", body);
        }
        return Response.serverError().entity("KO").type(MediaType.TEXT_PLAIN).build();
    }


    @POST
    @Path("/blob/pill/{serial_number}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response decryptPillBlob(@PathParam("serial_number") final String serialNumber, final byte[] body) throws Exception {
        checkNotNull(serialNumber, "Serial Number can not be null");

        try{
            final Optional<PillBlobProvision> pillBlobProvisionOptional = keyStoreUtils.decryptPill(body, serialNumber);
            if(!pillBlobProvisionOptional.isPresent()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("KO").type(MediaType.TEXT_PLAIN).build();
            }

            final PillBlobProvision pillBlobProvision = pillBlobProvisionOptional.get();
            pillKeyStore.put(pillBlobProvision.pillId, pillBlobProvision.key, serialNumber);
            final StringBuilder sb = new StringBuilder();
            sb.append("OK\n");
            sb.append(pillBlobProvision.pillId + "\n");
            sb.append(pillBlobProvision.serialNumber + "\n");
            return Response.ok().entity(sb.toString()).type(MediaType.TEXT_PLAIN).build();

        } catch (Exception e) {
            LOGGER.error("Exception while provisioning Sense: {}", e.getMessage());
            LOGGER.error("Body was : {}", body);
        }
        return Response.serverError().entity("KO").type(MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("/check/p/{serial_number}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response checkPillBySerialNumber(@PathParam("serial_number") final String serialNumber) {
        final Optional<PillProvision> pillProvisionOptional = pillProvisionDAO.getBySN(serialNumber);
        if(!pillProvisionOptional.isPresent()) {
            return Response.status(Response.Status.NOT_FOUND).entity("KO not found").build();
        }
        final String message = String.format("OK SN created on %s\n", pillProvisionOptional.get().created.toString());
        return Response.ok().entity(message).build();
    }
}
