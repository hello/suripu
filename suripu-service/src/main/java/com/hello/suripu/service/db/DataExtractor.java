package com.hello.suripu.service.db;

import com.hello.suripu.api.input.InputProtos;
import com.hello.suripu.core.TrackerMotion;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.service.Util;
import org.joda.time.DateTime;

/**
 * Created by pangwu on 5/8/14.
 */
public class DataExtractor {
    public static TrackerMotion normalizeAndSave(InputProtos.TrackerDataBatch.TrackerData sample,
                                                       AccessToken accessToken,
                                                       TrackerMotionDAO dao){

        DateTime roundedDateTimeUTC = Util.roundTimestampToMinuteUTC(sample.getTimestamp());



        Long id = dao.insertTrackerMotion(accessToken.accountId,
                sample.getTrackerId(),
                sample.getSvmNoGravity(),
                roundedDateTimeUTC,
                sample.getOffsetMillis());

        return new TrackerMotion(id, accessToken.accountId,
                sample.getTrackerId(),
                roundedDateTimeUTC,
                sample.getSvmNoGravity(),
                sample.getOffsetMillis()
                );
    }

    
}
