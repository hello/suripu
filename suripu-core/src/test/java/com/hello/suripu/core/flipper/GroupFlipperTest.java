package com.hello.suripu.core.flipper;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Team;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GroupFlipperTest {



    @Test
    public void testDenormalize() {

        final List<Team> userTeams = new ArrayList<>();
        final Team team = new Team("test1", Lists.newArrayList("one", "two"));
        final Team team2 = new Team("test2", Lists.newArrayList("one", "three", "four"));

        userTeams.add(team);
        userTeams.add(team2);

        final List<Team> deviceTeams = new ArrayList<>();
        final Team team3 = new Team("test3", Lists.newArrayList("five", "six"));
        final Team team4 = new Team("test4", Lists.newArrayList("seven", "eight"));
        deviceTeams.add(team3);
        deviceTeams.add(team4);

        final Map<String, Set<String>> map = GroupFlipper.denormalizeGroups(deviceTeams, userTeams);

        for(final String key : Lists.newArrayList("one", "two", "three", "four")) {
            assertThat(map.containsKey(key), is(true));
        }

        assertThat(map.get("one").contains("test1"), is(true));
        assertThat(map.get("one").contains("test2"), is(true));
        assertThat(map.get("one").size(), is(2));

    }


    @Test
    public void testPercentage() {
        final List<Long> accountIds = Lists.newArrayList(1000L, 1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L);
        final Integer percentage = 40;
        final List<Long> correctIds = new ArrayList<>();
        for (final Long hashId : accountIds) {
            if (hashId % 10 < percentage / 10) {
                correctIds.add(hashId);
            }
        }
        assertThat(correctIds.size(), is(percentage/10));

    }
}
