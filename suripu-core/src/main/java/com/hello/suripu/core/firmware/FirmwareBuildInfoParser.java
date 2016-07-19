package com.hello.suripu.core.firmware;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import java.util.Map;

public class FirmwareBuildInfoParser {


    public static Optional<String> parse(final String buildInfoText) {
        final Iterable<String> strings = Splitter.on("\n").split(buildInfoText);
        final Map<String, String> buildInfo = Maps.newHashMap();

        for (final String line : strings) {
            if (line.contains(":")) {
                final String[] parts = line.split(":");
                buildInfo.put(parts[0].trim(), parts[1].trim());
            }
        }

        if (!buildInfo.containsKey("version")) {
            return Optional.absent();
        }

        if (buildInfo.get("version").isEmpty()) {
            return Optional.absent();
        }

        try {
            final String versionText = buildInfo.get("version").trim();
            //TODO: Remove this hacky method of distinguishing old fw versions from new

            if (Long.parseLong(versionText, 16) > Integer.MAX_VALUE) {
                return Optional.absent();
            }

            if (versionText.length() < 6) {
                return Optional.of(versionText);
            }
            return Optional.of(Integer.toString(Integer.parseInt(versionText, 16)));
        } catch (NumberFormatException nfe) {
            return Optional.absent();
        }
    }

}
