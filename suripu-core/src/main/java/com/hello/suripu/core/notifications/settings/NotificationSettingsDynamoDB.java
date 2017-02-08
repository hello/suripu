package com.hello.suripu.core.notifications.settings;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableWriteItems;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationSettingsDynamoDB implements NotificationSettingsDAO {

    private final static boolean DEFAULT_ENABLED = false;
    final private static List<NotificationSetting.Type> ORDERING = Lists.newArrayList(
            NotificationSetting.Type.SLEEP_SCORE,
            NotificationSetting.Type.SYSTEM,
            NotificationSetting.Type.SLEEP_REMINDER);

    private final DynamoDB dynamoDB;
    private final String tableName;

    public NotificationSettingsDynamoDB(DynamoDB dynamoDB, String tableName) {
        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
    }

    private NotificationSetting createDefault(final Long accountId, final NotificationSetting.Type type) {
        return new NotificationSetting(accountId, type, DEFAULT_ENABLED, Optional.absent());
    }

    @Override
    public void save(final List<NotificationSetting> settings) {
        final List<Item> items = settings.stream().map(s -> toItem(s)).collect(Collectors.toList());
        final TableWriteItems settingsWriteItems = new TableWriteItems(tableName)
                .withItemsToPut(items);
        dynamoDB.batchWriteItem(settingsWriteItems);
    }

    @Override
    public List<NotificationSetting> get(final Long accountId) {
        final Table table = dynamoDB.getTable(tableName);
        final KeyAttribute key = new KeyAttribute("account_id", accountId);
        final ItemCollection<QueryOutcome> outcomes = table.query(key);
        final List<NotificationSetting> settings = Lists.newArrayList();
        final Map<NotificationSetting.Type, NotificationSetting> defaults = Maps.newHashMap();

        for(final NotificationSetting.Type type : NotificationSetting.Type.values()) {
            defaults.put(type, createDefault(accountId, type));
        }

        for(final Page<Item, QueryOutcome> page : outcomes.pages()) {
            final Iterator<Item> itemIterator = page.iterator();
            while(itemIterator.hasNext()) {
                final NotificationSetting setting = fromItem(itemIterator.next());
                defaults.put(setting.type(), setting);
            }
        }

        // Return notifications or defaults sorted as client expects it
        return ORDERING.stream().map(t -> defaults.get(t)).collect(Collectors.toList());
    }


    Item toItem(final NotificationSetting setting) {
        if(!setting.accountId.isPresent()) {
            throw new IllegalArgumentException("account id is absent");
        }
        final PrimaryKey primaryKey = new PrimaryKey()
                .addComponent("account_id", setting.accountId.get())
                .addComponent("type", setting.type());

        final Item item = new Item()
                .withPrimaryKey(primaryKey)
                .withBoolean("enabled", setting.enabled());
        if(setting.schedule.isPresent()) {
            item.withString(
                    "schedule",
                    setting.schedule.get().toString());
        }

        return item;
    }

    NotificationSetting fromItem(final Item item) {
        final NotificationSetting.Type type = NotificationSetting.Type.valueOf(item.getString("type"));
        final String schedule = item.hasAttribute("schedule") ? item.getString("schedule") : "";
        final NotificationSetting setting = new NotificationSetting(
                item.getLong("account_id"),
                type,
                item.getBOOL("enabled"),
                NotificationSchedule.fromString(schedule)
        );

        return setting;
    }
}
