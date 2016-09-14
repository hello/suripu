package com.hello.suripu.core.roomstate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class RoomStateSerDerTest {


    final ObjectMapper mapper = new ObjectMapper();
    final TypeFactory typeFactory = mapper.getTypeFactory();
    final MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, Object.class);

    @Test
    public void testSerializationWithDust() throws IOException {
        final CurrentRoomState state = CurrentRoomState.empty(true);

        final String json = mapper.writeValueAsString(state);
        assertNotNull(json);

        final Map<String, Object> map = mapper.readValue(json, mapType);
        assertFalse(map.isEmpty());

        for(final String sensorName : Sets.newHashSet("temperature", "humidity", "particulates", "sound", "light")) {
            assertThat(sensorName, map.containsKey(sensorName), is(true));
            assertNotNull(sensorName, map.get(sensorName));
        }
    }

    @Test
    public void testSerializationWithoutDust() throws IOException {
        final CurrentRoomState state = CurrentRoomState.empty(false);

        final String json = mapper.writeValueAsString(state);
        assertNotNull(json);

        final Map<String, Object> map = mapper.readValue(json, mapType);
        assertFalse("empty", map.isEmpty());
        assertFalse("particulates", map.containsKey("particulates"));
    }
}
