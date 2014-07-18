package com.hello.suripu.core.db.util;

import java.util.regex.Pattern;

public class MatcherPatternsDB {

    public static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");
}
