package com.hello.suripu.core.speech.models;

import com.hello.suripu.core.db.dynamo.Attribute;

/**
 * Created by ksg on 8/30/16
 */
public enum SpeechToTextAttribute  implements Attribute {
    UUID("uuid", "S", ":uuid"),                // uuid of saved audio in S3
    CREATED_UTC("created_utc", "S", ":ts"),    // timestamp  y-m-d h:m:s
    TEXT("text", "S", ":t"),                   // transcribed text
    SERVICE("service", "S", ":s"),             // service used -- google
    CONFIDENCE("conf", "N", ":c"),             // transcription confidence
    S3_KEYNAME("s3_key", "S", ":sk"),          // s3 keyname for response audio
    HANDLER_TYPE("handler_type", "S", ":h"),   // handler used to process command
    COMMAND("cmd", "S", ":cmd"),
    WAKE_ID("wake_id", "N", ":wid"),           // wake-word ID
    WAKE_CONFIDENCE("wake_conf", "SS", ":wc"), // confidence of all wake-words
    RESULT("cmd_result", "S", ":res"),         // result of speech command (OK, REJECT, TRY_AGAIN, FAILURE)
    RESPONSE_TEXT("resp_text", "S", ":rt"),
    UPDATED_UTC("updated", "S", ":up"),
    FIRMWARE_VERSION("fw", "N", ":fw");

    private final String name;
    private final String type;
    private final String query;

    SpeechToTextAttribute(String name, String type, String query) {
        this.name = name;
        this.type = type;
        this.query = query;
    }

    public String sanitizedName() {
        return toString();
    }
    public String shortName() {
        return name;
    }

    public String query() { return query; }

    @Override
    public String type() {
        return type;
    }

}
