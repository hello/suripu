package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.models.Team;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/v1/teams")
public class TeamsResource {

    private final TeamStore teamStore;

    public TeamsResource(final TeamStore teamStore) {
        this.teamStore = teamStore;
    }

    @GET
    @Path("/devices")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Team> allDeviceTeams(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken) {
        return teamStore.getTeams(TeamStore.Type.DEVICES);
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Team> allUsersTeams(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken) {
        return teamStore.getTeams(TeamStore.Type.USERS);
    }

    @PUT
    @Path("/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createDeviceTeam(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken, @Valid final Team team) {
        teamStore.createTeam(team, TeamStore.Type.DEVICES);
    }

    @PUT
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    public void createUsersTeam(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken, @Valid final Team team) {
        teamStore.createTeam(team, TeamStore.Type.USERS);
    }

    @POST
    @Path("/devices/{team_name}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addToDevicesTeam(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
            @Valid final List<String> deviceIds,
            @PathParam("team_name") final String teamName) {
        teamStore.add(teamName, TeamStore.Type.DEVICES, deviceIds);
    }

    @POST
    @Path("/users/{team_name}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addToUsersTeam(@Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
                               @Valid final List<Long> userIds,
                               @PathParam("team_name") final String teamName) {
        teamStore.add(teamName, TeamStore.Type.USERS, TeamStore.longsToStrings(userIds));
    }

    @DELETE
    @Path("/devices/{team_name}")
    public void deleteDevicesTeam(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
            @PathParam("team_name") final String teamName) {

    }

    @DELETE
    @Path("/users/{team_name}")
    public void deleteUsersTeam(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
            @PathParam("team_name") final String teamName) {

    }

    @DELETE
    @Path("/devices/{team_name}/{device_id}")
    public void removeFromDevicesTeam(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
            @PathParam("team_name") final String teamName,
            @PathParam("device_id") final String deviceId) {
        final List<String> ids = new ArrayList<>();
        ids.add(deviceId);
        teamStore.add(teamName, TeamStore.Type.DEVICES, ids);
    }

    @DELETE
    @Path("/users/{team_name}/{user_id}")
    public void removeFromUsersTeam(
            @Scope(OAuthScope.ADMINISTRATION_WRITE) final AccessToken accessToken,
            @PathParam("team_name") final String teamName,
            @PathParam("user_id") final Long userId) {
        final List<String> ids = new ArrayList<>();
        ids.add(String.valueOf(userId));
        teamStore.add(teamName, TeamStore.Type.USERS, ids);
    }
}
