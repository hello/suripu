package com.hello.suripu.core.insights;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jarredheinrich on 7/21/16.
 */
public class InsightsLastSeen {
    public final Long accountId;
    public final InsightCard.Category seenCategory;
    public final DateTime updatedUTC;

    public InsightsLastSeen(final Long accountId, final InsightCard.Category seenCategory, final DateTime updatedUTC) {
        this.accountId = accountId;
        this.seenCategory = seenCategory;
        this.updatedUTC = updatedUTC;
    }
    public InsightsLastSeen(final Long accountId, final InsightCard.Category seenCategory, final Long timestampUTC) {
        this.accountId = accountId;
        this.seenCategory = seenCategory;
        this.updatedUTC = new DateTime(timestampUTC, DateTimeZone.UTC);
    }

    public static Map<InsightCard.Category, DateTime> getLastSeenInsights(final List<InsightsLastSeen> insightsLastSeenList) {
        Map<InsightCard.Category, DateTime> insightsLastSeenMap = new HashMap<>();
        for ( final InsightsLastSeen insightlastseen : insightsLastSeenList){
            insightsLastSeenMap.put(insightlastseen.seenCategory, insightlastseen.updatedUTC);
        }

        return insightsLastSeenMap;
    }


    public static boolean checkQualifiedInsight(final Map<InsightCard.Category, DateTime> insightsLastSeenMap, InsightCard.Category category, final int timeWindowDays) {
        //checks if user may be qualified for insight based on time window
        final DateTime startDate = DateTime.now(DateTimeZone.UTC).minusDays(timeWindowDays);
        if (insightsLastSeenMap.containsKey(category)){
            if (insightsLastSeenMap.get(category).isAfter(startDate)){
                return false;
            }
        }

        return true;
    }

    public static int getNumRecentInsights(final Map<InsightCard.Category, DateTime> insightsLastSeenMap, final int timeWindowDays) {
        Collection<DateTime> lastSeenDateTimes = insightsLastSeenMap.values();
        final DateTime startDate = DateTime.now(DateTimeZone.UTC).minusDays(timeWindowDays);
        int numInsights = 0;
        for (final DateTime lastSeenDateTime : lastSeenDateTimes){
            if(lastSeenDateTime.isAfter(startDate)){
                numInsights +=1;
            }
        }

        return numInsights;
    }
}


