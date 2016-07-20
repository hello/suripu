package com.hello.suripu.core.speech;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.collect.ImmutableSet;
import com.hello.suripu.core.db.dynamo.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Created by ksg on 7/19/16
 */
public class SpeechToTextDAODynamoDB {

    private final static Logger LOGGER = LoggerFactory.getLogger(SpeechToTextDAODynamoDB.class);

    public enum SpeechToTextAttribute implements Attribute {
        ACCOUNT_ID("aid", "N"),
        RANGE_KEY("ts|dev", "S"),
        AUDIO_FILE_ID("file_id", "S"),  // uuid of saved audio in S3
        TEXT("text", "S"),              // transribed text
        SERVICE("service", "N"),        // service used -- google
        CONFIDENCE("conf", "N"),        // transcription confidence
        INTENT("intent", "S"),          // the next 4 attributes describes the parsed command
        ACTION("action", "S"),
        INTENT_CATEGORY("intent_cat", "S"),
        COMMAND("cmd", "S"),
        WAKE_ID("wake_id", "N"),        // wake-word ID
        WAKE_CONFIDENCE("wake_conf", "M"),  // confidence of all wake-words
        RESULT("result", "S");          // result of speech command (OK, REJECT, TRY_AGAIN, FAILURE)

        private final String name;
        private final String type;

        SpeechToTextAttribute(String name, String type) {
            this.name = name;
            this.type = type;
        }

        public String sanitizedName() {
            return toString();
        }

        public String shortName() {
            return name;
        }

        @Override
        public String type() {
            return type;
        }

        private AttributeValue get(final Map<String, AttributeValue> item) {
            return item.get(this.name);
        }
    }

    private static final Set<SpeechToTextAttribute> TARGET_ATTRIBUTES = new ImmutableSet.Builder<SpeechToTextAttribute>()
            .add(SpeechToTextAttribute.ACCOUNT_ID)
            .add(SpeechToTextAttribute.RANGE_KEY)
            .add(SpeechToTextAttribute.AUDIO_FILE_ID)
            .add(SpeechToTextAttribute.TEXT)
            .add(SpeechToTextAttribute.SERVICE)
            .add(SpeechToTextAttribute.CONFIDENCE)
            .add(SpeechToTextAttribute.INTENT)
            .add(SpeechToTextAttribute.ACTION)
            .add(SpeechToTextAttribute.INTENT_CATEGORY)
            .add(SpeechToTextAttribute.COMMAND)
            .add(SpeechToTextAttribute.WAKE_ID)
            .add(SpeechToTextAttribute.WAKE_CONFIDENCE)
            .add(SpeechToTextAttribute.RESULT)
            .build();
}
