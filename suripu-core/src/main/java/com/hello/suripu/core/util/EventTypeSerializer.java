package com.hello.suripu.core.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hello.suripu.core.models.Event;

import java.io.IOException;

public class EventTypeSerializer extends JsonSerializer<Event.Type> {
    @Override
    public void serialize(Event.Type enumValue, JsonGenerator gen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        gen.writeString(enumValue.toString());
    }
}
