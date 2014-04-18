package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.service.db.SleepLabel;
import com.hello.suripu.service.db.SleepLabelDAO;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by pangwu on 4/16/14.
 */
@Path("/label")
public class UserLabelResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveResource.class);

    private final SleepLabelDAO sleepLabelDAO;
    public UserLabelResource(final SleepLabelDAO sleepLabelDAO){
        this.sleepLabelDAO = sleepLabelDAO;
    }


    @Path("/save")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveLabel(@Valid SleepLabel sleepLabel, @Scope({OAuthScope.SLEEP_LABEL_WRITE})AccessToken accessToken){


        try{
            Optional<SleepLabel> sleepLabelOptional = this.sleepLabelDAO.getLabelByAccountAndDate(
                    accessToken.accountId,
                    sleepLabel.dateUTC,
                    sleepLabel.timeZoneOffset
            );

            if(sleepLabelOptional.isPresent()){
                LOGGER.warn("Sleep label at {}, timezone {} found, label will be updated",
                        sleepLabelOptional.get().dateUTC,
                        sleepLabelOptional.get().timeZoneOffset);
                this.sleepLabelDAO.updateBySleepLabelId(sleepLabelOptional.get().id,
                        sleepLabelOptional.get().rating.ordinal(),
                        sleepLabelOptional.get().sleepTimeUTC,
                        sleepLabelOptional.get().wakeUpTimeUTC);

            }else{
                this.sleepLabelDAO.insertLabel(accessToken.accountId,
                        sleepLabel.dateUTC,
                        sleepLabel.rating.ordinal(),
                        sleepLabel.sleepTimeUTC,
                        sleepLabel.wakeUpTimeUTC,
                        sleepLabel.timeZoneOffset
                );
                LOGGER.debug("Sleep label at {}, timezone {} created, ",
                        sleepLabel.dateUTC,
                        sleepLabel.timeZoneOffset);
            }
        }catch (UnableToExecuteStatementException ex){
            LOGGER.error(ex.getMessage());
            return Response.serverError().build();
        }

        return Response.ok().build();


    }
}
