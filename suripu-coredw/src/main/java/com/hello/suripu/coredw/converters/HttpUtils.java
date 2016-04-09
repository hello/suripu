package com.hello.suripu.coredw.converters;


import com.google.common.base.Joiner;
import com.hello.suripu.api.logging.LoggingProtos;
import com.sun.jersey.api.core.HttpRequestContext;
import org.joda.time.DateTime;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

public class HttpUtils {


    public static LoggingProtos.HttpRequest.Builder httpRequestToProtobuf(HttpRequestContext requestContext) {
        final LoggingProtos.HttpRequest.Builder builder = LoggingProtos.HttpRequest.newBuilder()
                .setPath(requestContext.getPath());

        final MultivaluedMap<String, String> headers = requestContext.getRequestHeaders();

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
