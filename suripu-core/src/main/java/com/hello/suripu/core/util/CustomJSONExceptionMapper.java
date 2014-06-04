package com.hello.suripu.core.util;

import com.yammer.dropwizard.validation.InvalidEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class CustomJSONExceptionMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomJSONExceptionMapper.class);

    private final Boolean debug;

    public CustomJSONExceptionMapper(final Boolean debug) {
        this.debug = debug;
    }

    public CustomJSONExceptionMapper() {
        this.debug = Boolean.FALSE;
    }

    private static class Error {
        public final Integer code;
        public final String message;

        public Error(final Integer code, final String message) {
            this.code = code;
            this.message = message;
        }
    }

    private Response handleWebApplicationException(Exception exception, Response defaultResponse) {
        WebApplicationException webAppException = (WebApplicationException) exception;

        // No logging
        if (webAppException.getResponse().getStatus() == 401) {
            return Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity("")
                    .build();
        }
        if (webAppException.getResponse().getStatus() == 404) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity("")
                    .build();
        }

        if (webAppException.getResponse().getStatus() == 400) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("")
                    .build();
        }

        // Debug logging

        // Warn logging

        // Error logging
        LOGGER.error("{}", exception.getMessage());

        return defaultResponse;
    }

    public ExceptionMapper<InvalidEntityException> invalidEntityExceptionExceptionMapper = new ExceptionMapper<InvalidEntityException>() {

        @Override
        public Response toResponse(InvalidEntityException invalid) {

            // Build default response
            final Response defaultResponse = Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(invalid.getErrors())
                    .type(MediaType.APPLICATION_JSON)
                    .build();

            LOGGER.error("{}", invalid.getErrors());
            return defaultResponse;

        }
    };


    public ExceptionMapper<RuntimeException> runtimeExceptionExceptionMapper = new ExceptionMapper<RuntimeException>() {

        @Override
        public Response toResponse(RuntimeException runtime) {

            // Build default response
            Response defaultResponse = Response
                    .serverError()
                    .entity("Runtime server error")
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .build();

            // Check for any specific handling
//            if (runtime instanceof WebApplicationException) {
//
//                return handleWebApplicationException(runtime, defaultResponse);
//            }

            // Use the default
            LOGGER.error("{}",runtime.getMessage());
            return defaultResponse;

        }
    };

    public ExceptionMapper<Throwable> throwableExceptionMapper = new ExceptionMapper<Throwable>() {

        @Override
        public Response toResponse(Throwable throwable) {

            if(throwable.getClass().getName().startsWith("com.fasterxml.jackson")) {

                final String message = (debug) ? throwable.getCause().getMessage() : "Bad request.";
                final Error error = new Error(Response.Status.BAD_REQUEST.getStatusCode(), message);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(error)
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            // Build default response
            final Response defaultResponse = Response
                    .serverError()
                    .entity(new Error(500, "Server Error"))
                    .type(MediaType.APPLICATION_JSON)
                    .build();

            // Check for any specific handling
//            if (runtime instanceof WebApplicationException) {
//
//                return handleWebApplicationException(runtime, defaultResponse);
//            }

            // Use the default
            LOGGER.error("{}",throwable.getMessage());
            return defaultResponse;

        }
    };
}
