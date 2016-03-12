package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.base.Optional;
import com.hello.suripu.api.input.State;
import com.hello.suripu.core.models.SenseStateAtTime;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by jakepiccolo on 2/19/16.
 */
public class SenseStateDynamoDBIT extends DynamoDBIT<SenseStateDynamoDB> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SenseStateDynamoDB.class);

    @Override
    protected CreateTableResult createTable() {
        return dao.createTable(2L, 2L);
    }

    @Override
    protected SenseStateDynamoDB createDAO() {
        return new SenseStateDynamoDB(amazonDynamoDBClient, TABLE_NAME);
    }

    private State.SenseState newState(final String senseId) {
        return State.SenseState.newBuilder()
                .setAudioState(State.AudioState.newBuilder().setPlayingAudio(false).build())
                .setSenseId(senseId)
                .build();
    }

    private State.SenseState newState(final String senseId, final int duration, final String filePath) {
        return State.SenseState.newBuilder()
                .setSenseId(senseId)
                .setAudioState(State.AudioState.newBuilder()
                        .setPlayingAudio(true)
                        .setFilePath(filePath)
                        .setDurationSeconds(duration)
                        .build())
                .build();
    }

    @Test
    public void testUpdateState() throws Exception {
        final String senseId = "sense";
        final DateTime origTime = new DateTime(2016, 1, 1, 0, 0, 0, DateTimeZone.UTC);

        final Optional<SenseStateAtTime> missingStateOptional = dao.getState(senseId);
        assertThat(missingStateOptional.isPresent(), is(false));

        final SenseStateAtTime state1 = new SenseStateAtTime(newState(senseId), origTime);
        dao.updateState(state1);
        final Optional<SenseStateAtTime> stateOptional1 = dao.getState(senseId);
        assertThat(stateOptional1.isPresent(), is(true));
        assertThat(stateOptional1.get().timestamp, is(origTime));

        final String state2Path = "path";
        final int state2Duration = 300;
        final SenseStateAtTime state2 = new SenseStateAtTime(newState(senseId, state2Duration, state2Path), origTime.plusSeconds(10));
        dao.updateState(state2);
        final Optional<SenseStateAtTime> stateOptional2 = dao.getState(senseId);
        assertThat(stateOptional2.isPresent(), is(true));
        assertThat(stateOptional2.get(), is(state2));
    }

    @Test
    public void testUpdateStateDifferentSenses() throws Exception {
        final String senseId1 = "sense1";
        final String senseId2 = "sense2";

        final DateTime origTime = new DateTime(2016, 1, 1, 0, 0, 0, DateTimeZone.UTC);

        final SenseStateAtTime state1 = new SenseStateAtTime(newState(senseId1), origTime);
        final SenseStateAtTime state2 = new SenseStateAtTime(newState(senseId2), origTime);

        dao.updateState(state1);
        dao.updateState(state2);

        assertThat(dao.getState(senseId1).get(), is(state1));
        assertThat(dao.getState(senseId2).get(), is(state2));
    }

    @Test
    public void testUpdateStateOnlyUpdateAudioWhenPresent() throws Exception {
        final String senseId = "sense";
        final DateTime origTime = new DateTime(2016, 1, 1, 0, 0, 0, DateTimeZone.UTC);

        final SenseStateAtTime state1 = new SenseStateAtTime(newState(senseId, 300, "file"), origTime);
        dao.updateState(state1);

        assertThat(dao.getState(senseId).get(), is(state1));

        final SenseStateAtTime state2 = new SenseStateAtTime(State.SenseState.newBuilder().setSenseId(senseId).build(), origTime.plusSeconds(10));
        dao.updateState(state2);

        final SenseStateAtTime currentState = dao.getState(senseId).get();
        assertThat(currentState.timestamp, is(state2.timestamp));
        assertThat(currentState.state.getAudioState(), is(state1.state.getAudioState()));
    }


}