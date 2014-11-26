package com.hello.suripu.core.resources;

import com.hello.suripu.core.ObjectGraphRoot;

public class BaseResource {

    protected BaseResource()  {
        ObjectGraphRoot.getInstance().inject(this);
    }
}
