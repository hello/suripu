package com.hello.suripu.core.db.util;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PostgresIntegerArrayArgumentFactory implements ArgumentFactory<SqlArray<Integer>>
{
    public boolean accepts(Class<?> expectedType, Object value, StatementContext ctx)
    {

        return value instanceof SqlArray
                && ((SqlArray)value).getType().isAssignableFrom(Integer.class);
    }

    public Argument build(Class<?> expectedType,
                          final SqlArray<Integer> value,
                          StatementContext ctx)
    {
        return new Argument()
        {
            public void apply(int position,
                              PreparedStatement statement,
                              StatementContext ctx) throws SQLException
            {
                // in postgres no need to (and in fact cannot) free arrays
                Array ary = ctx.getConnection()
                        .createArrayOf("integer", value.getElements());
                statement.setArray(position, ary);
            }
        };
    }
}


