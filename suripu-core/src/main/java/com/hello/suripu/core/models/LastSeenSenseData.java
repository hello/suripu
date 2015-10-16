package com.hello.suripu.core.models;

import com.hello.suripu.api.input.DataInputProtos;
import org.joda.time.DateTime;


public class LastSeenSenseData {
    public final String senseExternalId;
    public final DataInputProtos.periodic_data periodicData;
    public final DateTime updatedAtUTC;
    public final Integer firmwareVersion;

    public LastSeenSenseData(final String senseExternalId, final DataInputProtos.periodic_data periodicData, final DateTime updatedAtUTC, final Integer firmwareVersion) {
        this.senseExternalId = senseExternalId;
        this.periodicData = periodicData;
        this.updatedAtUTC = updatedAtUTC;
        this.firmwareVersion = firmwareVersion;
    }
}
