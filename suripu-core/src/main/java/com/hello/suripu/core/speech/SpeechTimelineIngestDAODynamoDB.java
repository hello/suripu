package com.hello.suripu.core.speech;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by ksg on 8/24/16
 */
public class SpeechTimelineIngestDAODynamoDB implements SpeechTimelineIngestDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechTimelineIngestDAODynamoDB.class);

    private Table table;
    private Vault kmsVault;

    private SpeechTimelineIngestDAODynamoDB(final Table table, final Vault kmsVault) {
        this.table = table;
        this.kmsVault = kmsVault;
    }

    public static SpeechTimelineIngestDAODynamoDB create(final AmazonDynamoDB amazonDynamoDB,
                                                         final String tableName,
                                                         final Vault kmsVault) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechTimelineIngestDAODynamoDB(table, kmsVault);
    }

    //region SpeechTimelineIngestDAO implementation
    @Override
    public Boolean putItem(final SpeechTimeline speechTimeline) {
        final Optional<Item> optionalItem = speechTimelineToItem(speechTimeline);
        if (!optionalItem.isPresent()) {
            LOGGER.error("error=fail-to-get-ddb-item account_id={} sense_id={}", speechTimeline.accountId, speechTimeline.senseId);
            return false;
        }

        final Item item = optionalItem.get();
        try {
            table.putItem(item);
        } catch (Exception e) {
            LOGGER.error("error=put-speech-timeline-item-fail error_msg={}", e.getMessage());
            return false;
        }

        return true;
    }
    //endregion

    private Optional<Item> speechTimelineToItem(SpeechTimeline speechTimeline) {
        final Map<String, String> encryptionContext = Maps.newHashMap();
        encryptionContext.put("account_id", speechTimeline.accountId.toString());

        final Optional<String> optionalEncryptedUUID = kmsVault.encrypt(speechTimeline.audioUUID, encryptionContext);
        if (!optionalEncryptedUUID.isPresent()) {
            return Optional.absent();
        }

        final String dateString = speechTimeline.dateTimeUTC.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT));
        final Item ddbItem = new Item()
                .withPrimaryKey(SpeechTimelineAttribute.ACCOUNT_ID.shortName(), speechTimeline.accountId)
                .withString(SpeechTimelineAttribute.TS.shortName(), dateString)
                .withString(SpeechTimelineAttribute.SENSE_ID.shortName(), speechTimeline.senseId)
                .withString(SpeechTimelineAttribute.ENCRYPTED_UUID.shortName(), optionalEncryptedUUID.get());

        return Optional.of(ddbItem);
    }

}
