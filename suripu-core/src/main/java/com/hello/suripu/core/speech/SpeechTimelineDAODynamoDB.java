package com.hello.suripu.core.speech;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ksg on 8/22/16
 */
public class SpeechTimelineDAODynamoDB implements SpeechTimelineIngestDAO, SpeechTimelineReadDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeechTimelineDAODynamoDB.class);

    private static final DateTimeFormatter DATE_TIME_WRITE_FORMATTER = DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT);
    private static final String KMS_KEY_SPEC = "AES_256";

    private static final Charset DEFAULT_CHARSET = Charset.forName("ASCII");

    private enum SpeechTimelineAttribute implements Attribute {
        ACCOUNT_ID("account_id", "N", ":aid"),  // Hash Key (account-id)
        TS("ts", "S", ":ts"),                    // Range Key (timestamp in UTC)
        SENSE_ID("sense_id", "S", ":sid"),      // sense-id
        ENCRYPTED_UUID("e_uuid", "S", ":euuid");        // encrypted UUID to audio identifier

        private final String name;
        private final String type;
        private final String queryHolder;

        SpeechTimelineAttribute(String name, String type, String queryHolder) {
            this.name = name;
            this.type = type;
            this.queryHolder = queryHolder;
        }

        public String sanitizedName() {
            return toString();
        }
        public String shortName() {
            return name;
        }
        public String type() {
            return type;
        }

        public String queryHolder() { return queryHolder; }
    }

    private Table table;
    private final AWSKMSClient kmsClient;
    private final String kmsKeyId;

    private SpeechTimelineDAODynamoDB(final Table table, final AWSKMSClient kmsClient, final String kmsKeyId) {
        this.table = table;
        this.kmsClient = kmsClient;
        this.kmsKeyId = kmsKeyId;
    }

    public static SpeechTimelineDAODynamoDB create(final AmazonDynamoDB amazonDynamoDB,
                                                   final String tableName,
                                                   final AWSKMSClient kmsClient,
                                                   final String kmsKeyId) {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.getTable(tableName);
        return new SpeechTimelineDAODynamoDB(table, kmsClient, kmsKeyId);
    }


    //region SpeechTimelineIngestDAO implementation
    @Override
    public Boolean putItem(final SpeechTimeline speechTimeline) {
        final Optional<Item> optionalItem = speechTimelineToItem(speechTimeline);
        if (!optionalItem.isPresent()) {
            LOGGER.error("error=fail-to-encrypt-uuid account_id={} sense_id={}", speechTimeline.accountId, speechTimeline.senseId);
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

    //region SpeechTimeReadDAO implementation
    @Override
    public Optional<SpeechTimeline> getItem(final Long accountId, final DateTime dateTime) {
        final Item item = table.getItem(SpeechTimelineAttribute.ACCOUNT_ID.shortName(), accountId,
                SpeechTimelineAttribute.TS.shortName(), dateTime.toString(DATE_TIME_WRITE_FORMATTER));

        if (item != null) {
            return DDBItemToSpeechTimeline(item);
        }
        return Optional.absent();
    }

    @Override
    public Optional<SpeechTimeline> getLatest(final Long accountId, final int lookBackMinutes) {
        final DateTime queryTime = DateTime.now(DateTimeZone.UTC).minusMinutes(lookBackMinutes);

        final String keyCondition = getExpression(SpeechTimelineAttribute.ACCOUNT_ID, "=") + " AND " +
                getExpression(SpeechTimelineAttribute.TS, ">=");

        final ValueMap valueMap = new ValueMap()
                .withNumber(SpeechTimelineAttribute.ACCOUNT_ID.queryHolder(), accountId)
                .withString(SpeechTimelineAttribute.TS.queryHolder(), queryTime.toString(DATE_TIME_WRITE_FORMATTER));

        final List<SpeechTimeline> results = query(keyCondition, valueMap, false, 1);
        if (!results.isEmpty()) {
            return Optional.of(results.get(0)); // latest only
        }
        return Optional.absent();

    }

    @Override
    public List<SpeechTimeline> getItemsByDate(final Long accountId, final DateTime startDate, final DateTime endDate, final int limit) {
        final String keyCondition = getExpression(SpeechTimelineAttribute.ACCOUNT_ID, "=") + " AND " +
                getBetweenExpression(SpeechTimelineAttribute.TS);

        final ValueMap valueMap = new ValueMap()
                .withNumber(SpeechTimelineAttribute.ACCOUNT_ID.queryHolder(), accountId)
                .withString(String.format("%s1", SpeechTimelineAttribute.TS.queryHolder()), startDate.toString(DATE_TIME_WRITE_FORMATTER))
                .withString(String.format("%s2", SpeechTimelineAttribute.TS.queryHolder()), endDate.toString(DATE_TIME_WRITE_FORMATTER));

        return query(keyCondition, valueMap, false, limit);
    }
    //endregion


    private List<SpeechTimeline> query(final String keyCondition, final ValueMap valueMap, final Boolean scanForward, final int limit) {

        final QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression(keyCondition)
                .withValueMap(valueMap)
                .withScanIndexForward(false)
                .withMaxResultSize(limit);

        ItemCollection<QueryOutcome> items = table.query(querySpec);
        LOGGER.debug("action=get-speech-timeline-by-date query_result_size={}", items.getTotalCount());

        Iterator<Item> iterator = items.iterator();
        final List<SpeechTimeline> results = Lists.newArrayList();
        while (iterator.hasNext()) {
            final Optional<SpeechTimeline> speechTimelineOptional = DDBItemToSpeechTimeline(iterator.next());
            if (speechTimelineOptional.isPresent()) {
                results.add(speechTimelineOptional.get());
            }
        }

        return results;
    }


    private Optional<String> encryptUUID (final String uuid, final Long accountId) {
        final ByteBuffer plainText = ByteBuffer.wrap(uuid.getBytes());

        final Map<String, String> encryptionContext = Maps.newHashMap();
        encryptionContext.put("account_id", accountId.toString());

        final EncryptRequest encryptRequest = new EncryptRequest()
                .withKeyId(kmsKeyId)
                .withEncryptionContext(encryptionContext)
                .withPlaintext(plainText);

        final ByteBuffer cipherText = kmsClient.encrypt(encryptRequest).getCiphertextBlob();

        // copy to String
        if (cipherText.hasArray()) {
            return Optional.of(new String(Base64.encodeBase64(cipherText.array())));
        }
        return Optional.absent();
    }


    private Optional<String> decryptUUID(final String encryptedUUID, final Long accountId) {
        final ByteBuffer cipherTextBlob = ByteBuffer.wrap(Base64.decodeBase64(encryptedUUID));

        final Map<String, String> encryptionContext = Maps.newHashMap();
        encryptionContext.put("account_id", accountId.toString());

        final DecryptRequest decryptRequest = new DecryptRequest()
                .withEncryptionContext(encryptionContext)
                .withCiphertextBlob(cipherTextBlob);

        final ByteBuffer plainText =  kmsClient.decrypt(decryptRequest).getPlaintext();
        if (plainText.hasArray()) {
            return Optional.of(new String(plainText.array(), DEFAULT_CHARSET));
        }
        return Optional.absent();
    }


    private Optional<SpeechTimeline> DDBItemToSpeechTimeline(Item item) {
        final Long accountId = item.getLong(SpeechTimelineAttribute.ACCOUNT_ID.shortName());
        final Optional<String> optionalUUID = decryptUUID(item.getString(SpeechTimelineAttribute.ENCRYPTED_UUID.shortName()), accountId);
        if (!optionalUUID.isPresent()) {
            return Optional.absent();
        }

        return Optional.of(new SpeechTimeline(
                accountId,
                DateTimeUtil.datetimeStringToDateTime(item.getString(SpeechTimelineAttribute.TS.shortName())),
                item.getString(SpeechTimelineAttribute.SENSE_ID.shortName()),
                optionalUUID.get()));
    }

    private Optional<Item> speechTimelineToItem(SpeechTimeline speechTimeline) {
        final Optional<String> optionalEncryptedUUID = encryptUUID(speechTimeline.audioUUID, speechTimeline.accountId);
        if (!optionalEncryptedUUID.isPresent()) {
            return Optional.absent();
        }

        return Optional.of(new Item()
                .withPrimaryKey(SpeechTimelineAttribute.ACCOUNT_ID.shortName(), speechTimeline.accountId)
                .withString(SpeechTimelineAttribute.TS.shortName(), speechTimeline.dateTimeUTC.toString(DATE_TIME_WRITE_FORMATTER))
                .withString(SpeechTimelineAttribute.SENSE_ID.shortName(), speechTimeline.senseId)
                .withString(SpeechTimelineAttribute.ENCRYPTED_UUID.shortName(), optionalEncryptedUUID.get()));

    }

    private String getExpression(final SpeechTimelineAttribute attribute, final String comparator) {
        return String.format("%s %s %s", attribute.shortName(), comparator, attribute.queryHolder());
    }

    private String getBetweenExpression(final SpeechTimelineAttribute attribute) {
        return String.format("%s BETWEEN %s AND %s", attribute.shortName(),
                String.format("%s1", attribute.queryHolder()), String.format("%s2", attribute.queryHolder()));
    }

    public static void createTable(final AmazonDynamoDB amazonDynamoDB, final String tableName) throws InterruptedException {
        final DynamoDB dynamoDB = new DynamoDB(amazonDynamoDB);
        final Table table = dynamoDB.createTable(
                tableName,
                Lists.newArrayList(
                        new KeySchemaElement().withAttributeName(SpeechTimelineAttribute.ACCOUNT_ID.shortName()).withKeyType(KeyType.HASH),
                        new KeySchemaElement().withAttributeName(SpeechTimelineAttribute.TS.shortName()).withKeyType(KeyType.RANGE)
                ),
                Lists.newArrayList(
                        new AttributeDefinition().withAttributeName(SpeechTimelineAttribute.ACCOUNT_ID.shortName()).withAttributeType(ScalarAttributeType.N),
                        new AttributeDefinition().withAttributeName(SpeechTimelineAttribute.TS.shortName()).withAttributeType(ScalarAttributeType.S)
                ),
                new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
        );

        table.waitForActive();
    }

}
