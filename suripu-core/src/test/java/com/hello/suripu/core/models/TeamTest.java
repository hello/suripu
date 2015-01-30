package com.hello.suripu.core.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class TeamTest {


    @Test
    public void testIdsWithSpaces() {
        final List<String> ids = Lists.newArrayList(" A ", "B ", " C");
        final Team team = new Team("test", ids);
        assertThat(team.ids, containsInAnyOrder("A", "B", "C"));
    }

    @Test
    public void testIdsWithSpacesStatic() {
        final List<String> ids = Lists.newArrayList(" A ", "B ", " C");
        final Team team = Team.create("test", Sets.newHashSet(ids));
        assertThat(team.ids, containsInAnyOrder("A", "B", "C"));
        assertThat(team.ids.contains(" A "), is(false));
    }

    @Test
    public void spaceInTeamName() {
        final List<String> ids = Lists.newArrayList();
        for(String name : Lists.newArrayList("test "," test", " test ")) {
            final Team team = new Team(name, ids);
            assertThat(team.name, is(name.trim()));
        }


    }
}
