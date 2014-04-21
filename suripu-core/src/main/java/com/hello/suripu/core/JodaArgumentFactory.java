package com.hello.suripu.core;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class JodaArgumentFactory implements ArgumentFactory<DateTime>
{
    @Override public boolean accepts(final Class<?> expectedType, final Object value, final StatementContext ctx)
    {
        return value != null && DateTime.class.isAssignableFrom(value.getClass());
    }

    @Override public Argument build(final Class<?> expectedType, final DateTime value, final StatementContext ctx)
    {
        return new Argument()
        {
            @Override public void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException
            {
                statement.setTimestamp(position, new Timestamp(value.toDate().getTime()));
            }
            public String toString() {
                return value == null ? null : value.toString();
            }
        };

    }
}
