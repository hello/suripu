package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public class Tag {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("ids")
    public final Set<String> ids;

    @JsonCreator
    public Tag(
            @JsonProperty("name") final String name,
            @JsonProperty("ids") final List<String> ids) {
        this.name = name.trim();
        final Set<String> cleanIds = Sets.newHashSet();
        for(final String id : ids) {
            cleanIds.add(id.trim());
        }
        this.ids = ImmutableSet.copyOf(cleanIds);
    }

    public static Tag create(final String name, final Set<String> ids) {
        return new Tag(name, Lists.newArrayList(ids));
    }
}
