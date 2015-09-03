package com.hello.suripu.service.resources;

import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.service.registration.FailedToSignException;

public interface ResponseSigner {

    byte[] signAndSend(final String senseId, final SenseCommandProtos.MorpheusCommand command) throws FailedToSignException;
}
