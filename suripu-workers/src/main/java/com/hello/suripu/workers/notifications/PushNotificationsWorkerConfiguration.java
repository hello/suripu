package com.hello.suripu.workers.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import com.hello.suripu.coredw.configuration.DynamoDBConfiguration;
import com.hello.suripu.core.configuration.PushNotificationsConfiguration;
import com.hello.suripu.workers.framework.WorkerConfiguration;

import com.yammer.dropwizard.db.DatabaseConfiguration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Set;

public class PushNotificationsWorkerConfiguration extends WorkerConfiguration {
    @Valid
    @NotNull
    @JsonProperty("common_db")
    private DatabaseConfiguration commonDB = new DatabaseConfiguration();

    public DatabaseConfiguration getCommonDB() {
        return commonDB;
    }

    @Valid
    @NotNull
    @JsonProperty("max_records")
    private Integer maxRecords;

    public Integer getMaxRecords() {
        return maxRecords;
    }

    @Valid
    @NotNull
    @JsonProperty("push_notifications")
    private PushNotificationsConfiguration pushNotificationsConfiguration;

    public PushNotificationsConfiguration getPushNotificationsConfiguration() {
        return pushNotificationsConfiguration;
    }


    @Valid
    @NotNull
    @JsonProperty("user_info_db")
    private DynamoDBConfiguration alarmInfoDynamoDBConfiguration;
    public DynamoDBConfiguration getAlarmInfoDynamoDBConfiguration(){
        return this.alarmInfoDynamoDBConfiguration;
    }



    @Valid
    @NotNull
    @JsonProperty("preferences_db")
    private DynamoDBConfiguration accountPreferences;
    public DynamoDBConfiguration getAccountPreferences(){
        return this.accountPreferences;
    }

    @Valid
    @NotNull
    @JsonProperty("features_db")
    private DynamoDBConfiguration featuresDynamoDBConfiguration;
    public DynamoDBConfiguration getFeaturesDynamoDBConfiguration(){
        return this.featuresDynamoDBConfiguration;
    }

    @Valid
    @NotEmpty
    @JsonProperty("active_hours")
    private Set<Integer> activeHours = Sets.newHashSet();
    public Set<Integer> getActiveHours() {return activeHours;}
}
