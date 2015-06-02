package com.hello.suripu.core.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * Created by benjo on 5/27/15.
 */
public class BestMatchByTypeMap<K, V> {


    private final Map<K,TreeSet<V>> map;
    private final Scorer<V> scorer;
    private final Comparator<V> comparator;

    public BestMatchByTypeMap(final Scorer<V> scorer, final Comparator<V> comparator) {
        map = new HashMap<>();
        this.scorer = scorer;
        this.comparator = comparator;
    }

    public void clear() {
        map.clear();
    }

    public void add(final K key, final V value) {
        if (!map.containsKey(key)) {
            map.put(key,new TreeSet<V>(comparator));
        }

        map.get(key).add(value);
    }

    public V getClosest(final K key, final V value) {
        TreeSet<V> myset = map.get(key);

        if (myset == null) {
            return null;
        }

        if (myset.contains(value)) {
            return value;
        }

        final V higher = myset.higher(value);
        final V lower =  myset.lower(value);

        if (higher == null) {
            return lower;
        }

        if (lower == null) {
            return higher;
        }

        //assume subtraction works (maybe refactor later)

        final V scoreLower = scorer.getScore(lower,value);
        final V scoreHigher = scorer.getScore(higher, value);

        if (comparator.compare(scoreLower,scoreHigher) > 0) {
            return higher;
        }
        else {
            return lower;
        }
    }

    public boolean remove(final K key, final V value) {
        TreeSet<V> myset = map.get(key);

        if (myset == null) {
            return false;
        }

        boolean result = myset.remove(value);

        return result;
    }
}
