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

    public enum SettingAttribute{
        ACCOUNT_ID ("account_id"),
        ENABLED ("enabled"),
        TYPE("type"),
        SCHEDULE("schedule");

        private final String name;
        SettingAttribute(final String name) {
            this.name = name;
        }
    }

    private final static boolean DEFAULT_ENABLED = true;
    final private static List<NotificationSetting.Type> ORDERING = Lists.newArrayList(
            NotificationSetting.Type.SLEEP_SCORE,
            NotificationSetting.Type.SYSTEM,
            NotificationSetting.Type.SLEEP_REMINDER);

    private final DynamoDB dynamoDB;
    private final Table table;

    private NotificationSettingsDynamoDB(final DynamoDB dynamoDB, final Table table) {
        this.dynamoDB = dynamoDB;
        this.table = table;

    }

    public static NotificationSettingsDynamoDB create(final DynamoDB dynamoDB, final String tableName) {
        final Table table = dynamoDB.getTable(tableName);
        return new NotificationSettingsDynamoDB(dynamoDB, table);
    }

    private NotificationSetting createDefault(final Long accountId, final NotificationSetting.Type type) {
        return new NotificationSetting(accountId, type, DEFAULT_ENABLED, Optional.absent());
    }

    @Override
    public void save(final List<NotificationSetting> settings) {
        final List<Item> items = settings.stream().map(s -> toItem(s)).collect(Collectors.toList());
        final TableWriteItems settingsWriteItems = new TableWriteItems(table.getTableName())
                .withItemsToPut(items);
        dynamoDB.batchWriteItem(settingsWriteItems);
    }

    @Override
    public List<NotificationSetting> get(final Long accountId) {

        final KeyAttribute key = new KeyAttribute(SettingAttribute.ACCOUNT_ID.name, accountId);
        final ItemCollection<QueryOutcome> outcomes = table.query(key);
        final Map<NotificationSetting.Type, NotificationSetting> defaults = Maps.newHashMap();

        for(final NotificationSetting.Type type : NotificationSetting.Type.values()) {
            defaults.put(type, createDefault(accountId, type));
        }

        for(final Page<Item, QueryOutcome> page : outcomes.pages()) {
            final Iterator<Item> itemIterator = page.iterator();
            while(itemIterator.hasNext()) {
                final NotificationSetting setting = fromItem(itemIterator.next());
                defaults.put(setting.type, setting);
            }
        }

        // Return notifications or defaults sorted as client expects it
        return ORDERING.stream().map(t -> defaults.get(t)).collect(Collectors.toList());
    }

    @Override
    public boolean isOn(Long accountId, NotificationSetting.Type type) {
        final List<NotificationSetting> settings = get(accountId);
        for(NotificationSetting setting : settings) {
            if(type.equals(setting.type)) {
                return setting.enabled();
            }
        }

        return false;
    }


    Item toItem(final NotificationSetting setting) {
        if(!setting.accountId.isPresent()) {
            throw new IllegalArgumentException("account id is absent");
        }
        final PrimaryKey primaryKey = new PrimaryKey()
                .addComponent(SettingAttribute.ACCOUNT_ID.name, setting.accountId.get())
                .addComponent(SettingAttribute.TYPE.name, setting.type());

        final Item item = new Item()
                .withPrimaryKey(primaryKey)
                .withBoolean(SettingAttribute.ENABLED.name, setting.enabled());
        if(setting.schedule.isPresent()) {
            item.withString(SettingAttribute.SCHEDULE.name, setting.schedule.get().toString());
        }

        return item;
    }

    NotificationSetting fromItem(final Item item) {
        final NotificationSetting.Type type = NotificationSetting.Type.valueOf(item.getString(SettingAttribute.TYPE.name));
        final String schedule = item.hasAttribute(SettingAttribute.SCHEDULE.name) ? item.getString(SettingAttribute.SCHEDULE.name) : "";
        final NotificationSetting setting = new NotificationSetting(
                item.getLong(SettingAttribute.ACCOUNT_ID.name),
                type,
                item.getBOOL(SettingAttribute.ENABLED.name),
                NotificationSchedule.fromString(schedule)
        );

        return setting;
    }
}
