package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Feature implements Comparable<Feature> {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("ids")
    public final Set<String> ids;

    @JsonProperty("groups")
    public final Set<String> groups;

    @JsonProperty("percentage")
    public final Float percentage;

    @JsonCreator
    public Feature(
            @JsonProperty("name") final String name,
            @JsonProperty("ids") final Collection<String> ids,
            @JsonProperty("groups") final Collection<String> groups,
            @JsonProperty("percentage") final Float percentage) {
        this.name = name.trim();
        this.ids = ImmutableSet.copyOf(trimStringsInCollection(ids));
        this.groups = ImmutableSet.copyOf(trimStringsInCollection(groups));
        this.percentage = percentage;
    }

    private static Collection<String> trimStringsInCollection(final Collection<String> strings) {
        final List<String> cleanStrings = Lists.newArrayList();
        for(String string : strings) {
            cleanStrings.add(string.trim());
        }

        return cleanStrings;
    }


    public String serialize() {
        final StringBuilder sb = new StringBuilder();
        sb.append(percentage);
        sb.append("|");
        sb.append(Joiner.on(",").join(ids));
        sb.append("|");
        sb.append(Joiner.on(",").join(groups));

        return sb.toString();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Feature.class)
                .add("name", name)
                .add("percentage", percentage)
                .add("ids", ids)
                .add("groups", groups)
                .toString();
    }


    public static Feature convertToFeature(final String featureName, final String value) {
        final Splitter splitter = Splitter.on("|");
        final String[] splitResult = Iterables.toArray(splitter.split(value), String.class);
        if (splitResult.length != 3) {
            throw new FeatureException("Invalid format for feature");
        }

        final List<String> groups = Arrays.asList(splitResult[2].split(","));
        final List<String> userIds = Arrays.asList(splitResult[1].split(","));
        final Float percentage = Float.parseFloat(splitResult[0]);
        return new Feature(featureName, userIds, groups, percentage);
    }

    public static class FeatureException extends RuntimeException {
        public FeatureException(String msg) {
            super(msg);
        }
    }


    @Override
    public int compareTo(Feature other) {
        return ComparisonChain.start()
                .compare(name, other.name)
                .result();
    }
}
