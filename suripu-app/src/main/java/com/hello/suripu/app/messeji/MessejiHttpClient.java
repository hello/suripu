package com.hello.suripu.app.messeji;

import com.google.common.base.Optional;
import com.hello.messeji.api.Messeji;
import com.hello.suripu.core.util.HelloHttpHeader;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by jakepiccolo on 2/22/16.
 */
public class MessejiHttpClient extends MessejiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessejiHttpClient.class);

    private final HttpClient httpClient;
    private final HttpHost host;

    private MessejiHttpClient(final HttpClient httpClient, final HttpHost host) {
        this.httpClient = httpClient;
        this.host = host;
    }

    public static MessejiHttpClient create(final HttpClient httpClient, final String scheme, final String hostName, final int port) {
        return new MessejiHttpClient(httpClient, new HttpHost(hostName, port, scheme));
    }

    public static MessejiHttpClient create(final HttpClient httpClient, final String hostString) throws URISyntaxException {
        final URI uri = new URI(hostString);
        return MessejiHttpClient.create(httpClient, uri.getScheme(), uri.getHost(), uri.getPort());
    }

    public Optional<Long> sendMessage(final String senseId, final Messeji.Message message) {
        final HttpPost request = new HttpPost("/send");
        request.setHeader(HelloHttpHeader.SENSE_ID, senseId);
        request.setEntity(new ByteArrayEntity(message.toByteArray()));
        final HttpResponse response;
        try {
            response = httpClient.execute(host, request);
        } catch (IOException e) {
            LOGGER.error("error=IOException senseId={} on=http-request-messeji exception={}", senseId, e);
            return Optional.absent();
        }

        final int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode >= 500) {
            // Service exception
            LOGGER.error("error=messeji-service-error status-code={} sense-id={}",
                    statusCode, senseId);
            return Optional.absent();
        } else if (statusCode >= 400) {
            // client exception
            LOGGER.error("error=messeji-client-error status-code={} sense-id={}",
                    statusCode, senseId);
            return Optional.absent();
        }

        try {
            final InputStream contentStream = response.getEntity().getContent();
            final Messeji.Message responseMessage = Messeji.Message.parseFrom(contentStream);
            contentStream.close();
            return Optional.of(responseMessage.getMessageId());
        } catch (IOException e) {
            LOGGER.error("error=IOException on=parse-messeji-protobuf-response senseId={} exception={}", senseId, e);
            return Optional.absent();
        }
    }

}
