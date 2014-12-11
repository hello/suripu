package com.hello.suripu.core.models;

/**
 * Created by zet on 12/11/14.
 */
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DeviceInactivePaginator {

    @JsonProperty("current_page")
    public final Integer currentPage;
    @JsonProperty("total_pages")
    public final Integer totalPage;
    @JsonProperty("content")
    public final List<DeviceInactive> content;

    public DeviceInactivePaginator(final Integer currentPage, final Integer totalPage, final List<DeviceInactive> content) {
        this.currentPage = currentPage;
        this.totalPage = totalPage;
        this.content = content;
    }
}