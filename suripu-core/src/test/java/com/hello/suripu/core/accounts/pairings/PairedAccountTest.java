package com.hello.suripu.core.accounts.pairings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PairedAccountTest {

    final ObjectMapper mapper = new ObjectMapper();
    
    @Test
    public void testSerialization() throws JsonProcessingException {
        final PairedAccount pairedAccount = PairedAccount.fromExtId("jane","40c66598-f05f-4db9-b12c-0339099309b2", false);
        final String jsonString = mapper.writeValueAsString(pairedAccount);
        assertTrue("should contain name", jsonString.contains("jane"));
        assertTrue("should contain id", jsonString.contains("40c66598-f05f-4db9-b12c-0339099309b2"));
        assertTrue("should contain is_self", jsonString.contains("is_self"));
    }

    @Test
    public void testDeserialization() throws IOException {

        final String json = "{\"name\": \"jane\", \"id\": \"40c66598-f05f-4db9-b12c-0339099309b2\", \"is_self\": false}";
        final PairedAccount pairedAccount = mapper.readValue(json, PairedAccount.class);
        assertTrue("name should match", "jane".equals(pairedAccount.name()));
        assertTrue("id should match", "40c66598-f05f-4db9-b12c-0339099309b2".equals(pairedAccount.id()));
        assertFalse("is_self should match", pairedAccount.isSelf());
    }

    @Test
    public void testDeserializationMissingName() throws IOException {
        final String json = "{\"id\": \"40c66598-f05f-4db9-b12c-0339099309b2\", \"is_self\": false}";
        final PairedAccount pairedAccount = mapper.readValue(json, PairedAccount.class);
        assertTrue("name should be empty", pairedAccount.name().isEmpty());
    }

    @Test(expected = JsonMappingException.class)
    public void testDeserializationMissingId() throws IOException {
        final String json = "{\"name\": \"jane\", \"is_self\": false}";
        mapper.readValue(json, PairedAccount.class);
    }

    @Test(expected = JsonMappingException.class)
    public void testDeserializationInvalidId() throws IOException {
        final String json = "{\"name\": \"jane\", \"id\": \"foo\", \"is_self\": false}";
        mapper.readValue(json, PairedAccount.class); //should throw since Id is invalid UUID
    }
}
