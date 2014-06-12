package com.hello.suripu.core.util;

import com.sun.jersey.api.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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

    private static class Error {

        public final Integer code;
        public final String message;

        public Error(final Integer code, final String message) {
            this.code = code;
            this.message = message;
        }
    }

    private final static Error notFoundError = new Error(404, "Not found.");
    private final static Error notAuthorized = new Error(401, "Not authorized.");



    @Override
    public Response toResponse(Throwable throwable) {
        final Response defaultResponse = Response
                .serverError()
                .entity(new Error(500, "Server Error"))
                .type(MediaType.APPLICATION_JSON)
                .build();

        if(throwable instanceof WebApplicationException) {
            return handleWebApplicationException(throwable, defaultResponse);
        }


        if(throwable.getClass().getName().startsWith("com.fasterxml.jackson")) {

            final String message = (debug) ? throwable.getCause().getMessage() : "Bad request.";
            final Error error = new Error(Response.Status.BAD_REQUEST.getStatusCode(), message);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }


        if(throwable instanceof NotFoundException) {
            final Error error = new Error(Response.Status.BAD_REQUEST.getStatusCode(), "Not found.");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        if(throwable.getClass().getName().startsWith("com.sun.jersey.api")) {
//                    final String message = throwable.
            LOGGER.error("{}", throwable);
        }

        // Use the default
        LOGGER.error("{}: {}", throwable.getClass().getName(), throwable.getMessage());
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

        if (webAppException.getResponse().getStatus() == 401) {
            return Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(notAuthorized)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        if (webAppException.getResponse().getStatus() == 404) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(notFoundError)
                    .build();
        }

        if (webAppException.getResponse().getStatus() == 400) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new Error(400, webAppException.getResponse().getEntity().toString()))
                    .build();
        }



        LOGGER.error("WebApplicationException not caught: {} {}", webAppException.getResponse().getStatus(), webAppException.getMessage());

        return defaultResponse;
    }
}
