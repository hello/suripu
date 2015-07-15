package com.hello.suripu.core.logging;

import com.hello.suripu.core.configuration.QueueName;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class QueueNameTest {


    @Test
    public void testKinesisQueueNames() {

        //assertThat(QueueName.BATCH_PILL_DATA.toString(), is(QueueName.fromString("batch_pill_data").toString()));

    }
}
