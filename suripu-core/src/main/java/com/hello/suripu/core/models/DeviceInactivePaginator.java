package com.hello.suripu.core.models;

/**
 * Created by zet on 12/11/14.
 */
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DeviceInactivePaginator {

    @JsonProperty("current_page")
    public final Long currentPage;
    @JsonProperty("total_page")
    public final Long totalPage;
    @JsonProperty("content")
    public final List<DeviceInactive> content;

    public DeviceInactivePaginator(final Long currentPage, final Long totalPage, final List<DeviceInactive> content) {
        this.currentPage = currentPage;
        this.totalPage = totalPage;
        this.content = content;
    }
}