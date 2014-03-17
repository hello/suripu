package com.hello.suripu.service.resources;

import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.service.db.EventDAO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

@Path("/in")
public class ReceiveResource {
    private final EventDAO eventDAO;

    public ReceiveResource(EventDAO eventDAO) {
        this.eventDAO = eventDAO;
    }

    @POST
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    public Response receiveData(@Valid InputProtos.SensorSampleBatch batch) {

        ArrayList<Integer> sensor_ids = new ArrayList<Integer>();
        ArrayList<DateTime> timestamps = new ArrayList<DateTime>();
        ArrayList<Integer> values = new ArrayList<Integer>();
        Long deviceId = Long.parseLong(batch.getDeviceId());

        for(InputProtos.SensorSampleBatch.SensorSample sample : batch.getSamplesList()) {
            sensor_ids.add(sample.getSensorType().getNumber());
            DateTime dt = new DateTime((long) sample.getTimestamp(), DateTimeZone.UTC);
            timestamps.add(dt);
            values.add(sample.getValue());
        }

        eventDAO.insertBatch(sensor_ids, deviceId, timestamps, values);
        return Response.ok().build();
    }
}
