package com.hello.suripu.service.configuration;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Set;

public class OTAConfiguration extends Configuration {
    private static final Integer DEFAULT_START_UPDATE_WINDOW_HOUR = 11;  // non peak periods start at 11:00:00
    private static final Integer DEFAULT_END_UPDATE_WINDOW_HOUR = 22;  // non peak periods end at 22:59:59
    private static final Integer DEFAULT_DEVICE_UPTIME_DELAY = 20;  // How long to wait (in minutes) after device boot before allowing updates
    private static final Integer DEFAULT_S3_CACHE_EXPIRE_MINUTES = 60;  // How long to wait (in minutes) after device boot before allowing updates

    @Valid
    @Max(10)
    @Min(0)
    @JsonProperty("start_update_window_hour")
    private Integer startUpdateWindowHour = DEFAULT_START_UPDATE_WINDOW_HOUR;

    @JsonProperty("end_update_window_hour")
    private Integer endUpdateWindowHour = DEFAULT_END_UPDATE_WINDOW_HOUR;
    
    @JsonProperty("device_uptime_delay")
    private Integer deviceUptimeDelay = DEFAULT_DEVICE_UPTIME_DELAY;

    @JsonProperty("always_ota_groups")
    private Set<String> alwaysOTAGroups;
    
    @JsonProperty("s3cache_expire_minutes")
    private Integer s3CacheExpireMinutes = DEFAULT_S3_CACHE_EXPIRE_MINUTES;

    public Integer getStartUpdateWindowHour() {
        return this.startUpdateWindowHour;
    }

    public Integer getEndUpdateWindowHour() {
        return this.endUpdateWindowHour;
    }
    
    public Integer getDeviceUptimeDelay() {
        return this.deviceUptimeDelay;
    }

    public Set<String> getAlwaysOTAGroups() {
        return this.alwaysOTAGroups;
    }

    public Integer getS3CacheExpireMinutes() {
        return this.s3CacheExpireMinutes;
    }
}
