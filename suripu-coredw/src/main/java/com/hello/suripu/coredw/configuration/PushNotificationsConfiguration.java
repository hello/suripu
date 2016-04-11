package com.hello.suripu.coredw.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class PushNotificationsConfiguration {

    @NotNull
    @JsonProperty("arns")
    private Map<String, String> arns;

    public Map<String, String> getArns() {
        return arns;
    }

    @NotNull
    @JsonProperty("table_name")
    private String tableName;

    public String getTableName() {
        return tableName;
    }
}
