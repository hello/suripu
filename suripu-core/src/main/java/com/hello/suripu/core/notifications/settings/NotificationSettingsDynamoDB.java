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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NotificationSettingsDynamoDB implements NotificationSettingsDAO {

    private final DynamoDB dynamoDB;
    private final String tableName;

    private final Map<NotificationSetting.Type, String> names = ImmutableMap.of(
            NotificationSetting.Type.SLEEP_SCORE, "Sleep Score",
            NotificationSetting.Type.SLEEP_REMINDER, "Sleep Reminder",
            NotificationSetting.Type.SYSTEM, "System"
    );

    public NotificationSettingsDynamoDB(DynamoDB dynamoDB, String tableName) {
        this.dynamoDB = dynamoDB;
        this.tableName = tableName;
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

        for(final Page<Item, QueryOutcome> page : outcomes.pages()) {
            final Iterator<Item> itemIterator = page.iterator();
            while(itemIterator.hasNext()) {
              settings.add(fromItem(itemIterator.next()));
            }
        }

        return settings;
    }


    Item toItem(final NotificationSetting setting) {

        final PrimaryKey primaryKey = new PrimaryKey()
                .addComponent("account_id", setting.accountId)
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

        final NotificationSetting setting = new NotificationSetting(
                item.getLong("account_id"),
                names.getOrDefault(type, ""),
                type,
                item.getBOOL("enabled"),
                NotificationSchedule.fromString(item.getString("schedule"))
        );

        return setting;
    }
}
