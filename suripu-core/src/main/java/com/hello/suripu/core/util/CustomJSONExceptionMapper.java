package com.hello.suripu.core.util;

import com.google.common.base.Optional;
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

            // Use the default
            LOGGER.error("{}: {}", throwable.getClass().getName(), throwable.getMessage());
        } catch (Exception exception) {
            LOGGER.error("Failed to catch the following exception for: {}", throwable.getClass().getName());
            LOGGER.error(exception.getMessage());
        }
        return defaultResponse;
    }

    private Optional<JsonError> getJsonError(int statusCode) {
        switch (statusCode) {
            case 401:
                return Optional.of(notAuthorized);
            case 404:
                return Optional.of(notFoundError);
            case 400:
                return Optional.of(new JsonError(statusCode, "Malformed request"));
            case 405:
                return Optional.of(new JsonError(statusCode, "Method not allowed"));
            case 409:
                return Optional.of(new JsonError(statusCode, "conflict"));
            case 415:
                return Optional.of(new JsonError(statusCode, "Unsupported Media Type"));
            case 403:
                return Optional.of(new JsonError(statusCode, "Forbidden"));
            default:
                return Optional.absent();
        }
    }

    /**
     * If we have a Webapplication Exception, let's make our error look nice
     * @param exception
     * @param defaultResponse
     * @return
     */
    private Response handleWebApplicationException(Throwable exception, Response defaultResponse) {
        WebApplicationException webAppException = (WebApplicationException) exception;

        Optional<JsonError> error = getJsonError(webAppException.getResponse().getStatus());

        if (error.isPresent()) {
            return Response
                    .status(webAppException.getResponse().getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error.get())
                    .build();
        }

        LOGGER.error("WebApplicationException not caught: {} {}", webAppException.getResponse().getStatus(), webAppException.getMessage());

        return defaultResponse;
    }
}
