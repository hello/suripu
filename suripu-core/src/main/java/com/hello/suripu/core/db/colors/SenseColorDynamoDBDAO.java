package com.hello.suripu.core.db.colors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.sense.metadata.SenseMetadata;
import com.hello.suripu.core.sense.metadata.SenseMetadataDAO;

import java.util.ArrayList;

public class SenseColorDynamoDBDAO implements SenseColorDAO {

    private final SenseMetadataDAO metadataDAO;

    public SenseColorDynamoDBDAO(final SenseMetadataDAO senseMetadataDAO) {
        this.metadataDAO = senseMetadataDAO;
    }

    @Override
    public Optional<Device.Color> getColorForSense(String senseId) {
        final SenseMetadata senseMetadata = metadataDAO.get(senseId);
        final Sense.Color color = senseMetadata.color();
        switch(color) {
            case WHITE:
                return Optional.of(Device.Color.WHITE);
            case BLACK:
                return Optional.of(Device.Color.BLACK);
        }

        return Optional.of(Device.DEFAULT_COLOR);
    }

    @Override
    public Optional<Sense.Color> get(String senseId) {
        final SenseMetadata senseMetadata = metadataDAO.get(senseId);
        return Optional.of(senseMetadata.color());
    }

    @Override
    public int saveColorForSense(String senseId, String color) {
        return 0;
    }

    @Override
    public int update(String senseId, String color) {
        return 0;
    }

    @Override
    public ImmutableList<String> missing() {
        return ImmutableList.copyOf(new ArrayList<String>());
    }
}
