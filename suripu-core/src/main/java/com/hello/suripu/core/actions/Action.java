package com.hello.suripu.core.actions;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

/**
 * Created by ksg on 1/23/17
 */
public class Action {
    public final Long accountId;
    public final ActionType action;
    public final Optional<String> result;
    public final DateTime ts;
    public final Optional<Integer> offsetMillis;


    public Action(final Long accountId, final ActionType action, final Optional<String> result, final DateTime ts, final Optional<Integer> offsetMillis) {
        this.accountId = accountId;
        this.action = action;
        this.result = result;
        this.ts = ts;
        this.offsetMillis = offsetMillis;
    }

}
