package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;

public interface FirmwareVersionMappingDAO {
    void put(String fwHash, String humanVersion);

    List<String> get(String fwHash);

    Map<String, List<String>> getBatch(ImmutableSet<String> fwHashSet);
}
