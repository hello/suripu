package com.hello.suripu.core.flipper;

import com.google.common.collect.Lists;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.models.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class GroupFlipper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDBAdapter.class);
    private final AtomicReference<Map<String, Set<String>>> groups = new AtomicReference<>();
    private ScheduledFuture scheduledFuture;
    private final Integer pollingIntervalInSeconds;
    private final TeamStore teamStore;

    public GroupFlipper(final TeamStore teamStore, final Integer pollingIntervalInSeconds) {
        this.teamStore = teamStore;
        this.pollingIntervalInSeconds = pollingIntervalInSeconds;
        start();
    }

    final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    private void startPolling() {
        scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                final Map<String, Set<String>> temp = getData();
                groups.set(temp);
            }
        } , pollingIntervalInSeconds, pollingIntervalInSeconds, TimeUnit.SECONDS);
    }


    public void start() {
        final Map<String, Set<String>> temp = getData();
        groups.set(temp);
        LOGGER.info("Starting polling");
        startPolling();
    }

    public void stop() {
        scheduledFuture.cancel(true);
        LOGGER.info("Stopped polling");
        executorService.shutdown();
        LOGGER.info("ThreadPool shutdown");
    }

    synchronized private Map<String, Set<String>> getData() {
        LOGGER.trace("Calling getData");

        final List<Team> deviceTeams = teamStore.getTeams(TeamStore.Type.DEVICES);
        final List<Team> userTeams = teamStore.getTeams(TeamStore.Type.USERS);


        return denormalizeGroups(deviceTeams, userTeams);
    }

    /**
     * Denormalizes team structure for efficient lookup
     * @param deviceTeams
     * @param userTeams
     * @return
     */
    private Map<String, Set<String>> denormalizeGroups(List<Team> deviceTeams, List<Team> userTeams) {
        final Map<String, Set<String>> finalMap = new HashMap<>();

        for(final Team team: deviceTeams) {
            final Set<String> s = new HashSet<>();
            for(final String deviceId : team.ids) {
                s.add(team.name);
                finalMap.put(deviceId, s);
                s.clear();
            }
        }

        for(final Team team: userTeams) {
            final Set<String> s = new HashSet<>();
            for(final String userId : team.ids) {
                if(finalMap.containsKey(userId)) {
                    finalMap.get(userId).add(team.name);
                } else {
                    s.add(team.name);
                    finalMap.put(userId, s);
                }
                s.clear();
            }
        }

        return finalMap;
    }
    public List<String> getGroups(String deviceId) {
        final Set<String> deviceGroups = groups.get().get(deviceId);
        if(deviceGroups == null) {
            return Collections.EMPTY_LIST;
        }

        return Lists.newArrayList(deviceGroups);
    }

    public List<String> getGroups(Long userId) {
        final Set<String> deviceGroups = groups.get().get(String.valueOf(userId));
        if(deviceGroups == null) {
            return Collections.EMPTY_LIST;
        }

        return Lists.newArrayList(deviceGroups);
    }

    private class RolloutException extends RuntimeException {
        public RolloutException(final String message) {
            super(message);
        }
    }
}

