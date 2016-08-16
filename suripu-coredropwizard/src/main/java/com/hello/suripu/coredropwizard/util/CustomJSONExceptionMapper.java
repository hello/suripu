package com.hello.suripu.coredropwizard.util;

import com.hello.suripu.core.util.JsonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class CustomJSONExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomJSONExceptionMapper.class);

    private final Boolean debug;

    public CustomJSONExceptionMapper(final Boolean debug) {
        this.debug = debug;
    }

    public CustomJSONExceptionMapper() {
        this(Boolean.FALSE);
    }

    private final static JsonError notFoundError = new JsonError(404, "Not found.");
    private final static JsonError notAuthorized = new JsonError(401, "Not authorized.");



    @Override
    public Response toResponse(Throwable throwable) {
        final Response defaultResponse = Response
                .serverError()
                .entity(new JsonError(500, "Server Error"))
                .type(MediaType.APPLICATION_JSON)
                .build();

        try {
            if (throwable instanceof WebApplicationException) {
                return handleWebApplicationException(throwable, defaultResponse);
            }


            if (throwable.getClass().getName().startsWith("com.fasterxml.jackson")) {

                // TODO: there's bug in debug mode where throwable might not have a cause
                final String message = (debug) ? throwable.getCause().getMessage() : "Bad request.";
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), message))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }


            if (throwable instanceof NotFoundException) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonError(Response.Status.BAD_REQUEST.getStatusCode(), "Not found."))
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            if (throwable.getClass().getName().startsWith("com.sun.jersey.api")) {
//                    final String message = throwable.
                LOGGER.error("{}", throwable);
            }

            // To not confuse Timeout exceptions from ELB with real 500s
            // let's catch them and return a blank response
            if(throwable instanceof TimeoutException || throwable instanceof IOException) {
                return Response.status(Response.Status.REQUEST_TIMEOUT)
                        .entity("")
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .build();
            }

            // Use the default
            LOGGER.error("{}: {}", throwable.getClass().getName(), throwable.getMessage());
        } catch (Exception exception) {
            LOGGER.error("Failed to catch the following exception for: {}", throwable.getClass().getName());
            LOGGER.error(exception.getMessage());
        }
        return defaultResponse;
    }

    /**
     * If we have a Webapplication Exception, let's make our error look nice
     * @param exception
     * @param defaultResponse
     * @return
     */
    private Response handleWebApplicationException(Throwable exception, Response defaultResponse) {
        WebApplicationException webAppException = (WebApplicationException) exception;

        final int status = webAppException.getResponse().getStatus();
        final Object entity;

        switch (status) {
            case 204:
                entity = "";
                break;
            case 401:
                entity = notAuthorized;
                break;
            case 403:
                entity = new JsonError(403, "Forbidden");
                break;
            case 404:
                entity = notFoundError;
                break;
            case 400:
                entity = new JsonError(400, "Malformed request");
                break;
            case 405:
                entity = new JsonError(405, "Method not allowed");
                break;
            case 409:
                entity = new JsonError(409, "conflict.");
                break;
            case 415:
                entity = new JsonError(415, "Unsupported Media Type");
                break;
            case 429:
                entity = new JsonError(429, "Too many requests");
                break;
            default:
                LOGGER.error("WebApplicationException not caught: {} {}", status, webAppException.getMessage());
                return defaultResponse;
        }

        return Response
                .status(status)
                .entity(entity)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
