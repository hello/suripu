package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Team;

import java.util.List;

public interface TeamStoreDAO {
    void createTeam(Team team, TeamStore.Type type);

    Optional<Team> getTeam(String teamName, TeamStore.Type type);

    List<Team> getTeams(TeamStore.Type type);

    void add(String teamName, TeamStore.Type type, List<String> ids);

    void remove(String teamName, TeamStore.Type type, List<String> ids);

    void delete(Team team, TeamStore.Type type);
}
