package com.hello.suripu.service.resources;

import com.hello.suripu.service.ObjectGraphRoot;

public class BaseResource {

    protected BaseResource()  {
        ObjectGraphRoot.getInstance().inject(this);
    }
}
