import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by pangwu on 4/10/15.
 */
public class JodaTimeTest {
    @Test
    public void testUsingTheRightJodaTime(){
        final DateTime dateTimeFromYmd = new DateTime(2015, 4, 11, 10, 30, 0, 0, DateTimeZone.forID("Europe/Moscow"));
        final DateTime dateTimeFromMillis = new DateTime(1428733800000L, DateTimeZone.forID("Europe/Moscow"));
        assertThat(dateTimeFromYmd.equals(dateTimeFromMillis), is(false));
    }
}
