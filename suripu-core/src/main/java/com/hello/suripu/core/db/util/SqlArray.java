package com.hello.suripu.core.db.util;

import com.google.common.collect.Iterables;

import java.util.Collection;

import static java.util.Arrays.asList;

public class SqlArray<T>
{
    private final Object[] elements;
    private final Class<T> type;

    public SqlArray(Class<T> type, Collection<T> elements) {
        this.elements = Iterables.toArray(elements, Object.class);
        this.type = type;
    }

    public static <T> SqlArray<T> arrayOf(Class<T> type, T... elements) {
        return new SqlArray<T>(type, asList(elements));
    }

    public static <T> SqlArray<T> arrayOf(Class<T> type, Iterable<T> elements) {
        return new SqlArray<T>(type, (Collection<T>) elements);
    }

    public Object[] getElements()
    {
        return elements;
    }

    public Class<T> getType()
    {
        return type;
    }
}

