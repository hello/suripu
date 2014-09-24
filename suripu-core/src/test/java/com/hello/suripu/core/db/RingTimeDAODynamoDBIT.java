package com.hello.suripu.core.db;

import com.hello.suripu.core.models.Alarm;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Created by pangwu on 9/19/14.
 */
public class RingTimeDAODynamoDBIT {
    private AlarmDAODynamoDB alarmDAODynamoDB = mock(AlarmDAODynamoDB.class);
    private TimeZoneHistoryDAODynamoDB timeZoneHistoryDAODynamoDB = mock(TimeZoneHistoryDAODynamoDB.class);
    private List<Alarm> alarms = new ArrayList<Alarm>();

    @Before
    public void setUp(){

    }


    @After
    public void cleanUp(){

    }

}
