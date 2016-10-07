package com.hello.suripu.core.roomstate.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.hello.suripu.core.roomstate.CurrentRoomState;

import java.io.IOException;

public class CurrentRoomStateSerializer extends StdSerializer<CurrentRoomState> {

    public CurrentRoomStateSerializer() {
        this(null);
    }

    public CurrentRoomStateSerializer(Class<CurrentRoomState> t) {
        super(t);
    }

    @Override
    public void serialize(CurrentRoomState currentRoomState, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField("temperature", currentRoomState.temperature());
        jsonGenerator.writeObjectField("humidity", currentRoomState.humidity());
        jsonGenerator.writeObjectField("light", currentRoomState.light());
        jsonGenerator.writeObjectField("sound", currentRoomState.sound());
        if(currentRoomState.particulates() != null ) {
            jsonGenerator.writeObjectField("particulates", currentRoomState.particulates());
        }
        jsonGenerator.writeEndObject();
    }
}
