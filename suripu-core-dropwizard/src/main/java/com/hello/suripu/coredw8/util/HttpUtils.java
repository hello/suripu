package com.hello.suripu.coredw8.util;


import com.google.common.base.Joiner;
import com.hello.suripu.api.logging.LoggingProtos;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import org.joda.time.DateTime;

public class HttpUtils {


    public static LoggingProtos.HttpRequest.Builder httpRequestToProtobuf(ContainerRequestContext requestContext) {
        final LoggingProtos.HttpRequest.Builder builder = LoggingProtos.HttpRequest.newBuilder()
                .setPath(requestContext.getUriInfo().getPath(true));

        final MultivaluedMap<String, String> headers = requestContext.getHeaders();

        for(Map.Entry<String, List<String>> entry : headers.entrySet()) {

            final LoggingProtos.HttpRequest.Header header = LoggingProtos.HttpRequest.Header.newBuilder()
                    .setName(entry.getKey())
                    .setValue(Joiner.on(" ").join(entry.getValue()))
                    .build();
            builder.addHeaders(header);
        }

        builder.setTimestampUtc(DateTime.now().getMillis());

        return builder;
    }
}
