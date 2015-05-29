package com.hello.suripu.core.util;

import java.security.Key;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benjo on 5/27/15.
 */
public class BestMatchByTypeMap<KeyType,ValueType> {


    private final Map<KeyType,TreeSet<ValueType>> map;
    private final Scorer<ValueType> scorer;
    private final Comparator<ValueType> comparator;

    public BestMatchByTypeMap(final Scorer<ValueType> scorer, final Comparator<ValueType> comparator) {
        map = new HashMap<>();
        this.scorer = scorer;
        this.comparator = comparator;
    }

    public void clear() {
        map.clear();
    }

    public void add(final KeyType key, final ValueType value) {
        if (!map.containsKey(key)) {
            map.put(key,new TreeSet<ValueType>(comparator));
        }

        map.get(key).add(value);
    }

    public ValueType getClosest(final KeyType key, final ValueType value) {
        TreeSet<ValueType> myset = map.get(key);

        if (myset == null) {
            return null;
        }

        if (myset.contains(value)) {
            return value;
        }

        final ValueType higher = myset.higher(value);
        final ValueType lower =  myset.lower(value);

        if (higher == null) {
            return lower;
        }

        if (lower == null) {
            return higher;
        }

        //assume subtraction works (maybe refactor later)

        final ValueType scoreLower = scorer.getScore(lower,value);
        final ValueType scoreHigher = scorer.getScore(higher, value);

        if (comparator.compare(scoreLower,scoreHigher) > 0) {
            return higher;
        }
        else {
            return lower;
        }
    }

    public boolean remove(final KeyType key, final ValueType value) {
        TreeSet<ValueType> myset = map.get(key);

        if (myset == null) {
            return false;
        }

        boolean result = myset.remove(value);

        return result;
    }
}
