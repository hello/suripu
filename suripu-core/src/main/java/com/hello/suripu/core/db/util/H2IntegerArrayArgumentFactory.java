package com.hello.suripu.core.db.util;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class H2IntegerArrayArgumentFactory implements ArgumentFactory<SqlArray<Integer>>
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
                final Integer[] ints = new Integer[value.getElements().length];
                for(int i = 0; i < value.getElements().length; i++) {
                    ints[i] = (Integer) value.getElements()[i];
                }
                statement.setObject(position, ints);
            }
        };
    }
}


