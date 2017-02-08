package com.hello.suripu.core.actions;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseAsync;
import com.amazonaws.services.kinesisfirehose.model.Record;
import com.hello.suripu.core.db.FirehoseDAO;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ksg on 1/23/17
 */
public class ActionFirehoseDAO extends FirehoseDAO<Action> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ActionFirehoseDAO.class);

    public ActionFirehoseDAO(final String deliveryStreamName, final AmazonKinesisFirehoseAsync firehose) {
        super(deliveryStreamName, firehose);
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }

    @Override
    public Record toRecord(final Action model) {
        final String resultString = model.result.isPresent() ? model.result.get() : FirehoseDAO.NULL_STRING;
        final String offsetMillisString = model.offsetMillis.isPresent() ? toString(model.offsetMillis.get()) : FirehoseDAO.NULL_STRING;
        return toPipeDelimitedRecord(
                toString(model.accountId),
                model.action.string(),
                resultString,
                toString(model.ts),
                offsetMillisString
        );
    }

    @Override
    protected String toString(DateTime value) {
        return value.toString(DateTimeFormat.forPattern(DateTimeUtil.DYNAMO_DB_DATETIME_FORMAT));
    }
}
