package com.hello.suripu.workers.framework;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.model.Record;
import com.aphyr.riemann.Proto;
import com.aphyr.riemann.client.RiemannClient;

import javax.inject.Inject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class InstrumentedRecordProcessor extends HelloBaseRecordProcessor {
    @Inject
    protected RiemannClient riemannClient;

    protected String hostName = "";
    private AtomicInteger errorCount = new AtomicInteger();

    @Override
    public void initialize(final String s) {
        // TODO: do something with S?
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // no host should be surfaced in the metrics dashboard
        }
    }

    final public static Integer RIEMANN_EVENTS_DEFAULT_TTL = 60;

    private enum MetricState {
        OK("ok"),
        WARNING("ok"),
        ALERT("alert"),
        UNKNOWN("");

        private String value;
        private MetricState(String alert) {
            this.value = alert;
        }

    }

    protected void ok(final String service, final Integer value) {
        track(service, value, RIEMANN_EVENTS_DEFAULT_TTL, MetricState.OK);
    }

    protected void warn(final String service, final Integer value) {
        track(service, value, RIEMANN_EVENTS_DEFAULT_TTL, MetricState.WARNING);
    }

    protected void track(final String service, final Integer value) {
        track(service, value, RIEMANN_EVENTS_DEFAULT_TTL, MetricState.UNKNOWN);
    }

    protected void markError() {
        errorCount.incrementAndGet();
    }

    protected void markError(int value) {
        errorCount.set(value);
    }

    public abstract void processKinesisRecords(final List<Record> record, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer);

    public void messagesProcessed(final Integer errors, final Integer total) {
        MetricState state;
        if(errors == 0) {
            state = MetricState.OK;
        } else if (errors < 5) {
            state = MetricState.WARNING;
        } else {
            state = MetricState.ALERT;
        }

        final int success = total - errors;
        track("messages success", success, 10, state);
        track("messages errors", errors, RIEMANN_EVENTS_DEFAULT_TTL, state);
        errorCount.set(0);
    }

    public final void processRecords(final List<Record> records, final IRecordProcessorCheckpointer iRecordProcessorCheckpointer) {
        this.processKinesisRecords(records, iRecordProcessorCheckpointer);
        messagesProcessed(errorCount.get(), records.size());
    }


    protected void track(final String service, final Integer value, final Integer ttl, final MetricState state) {


        if(!riemannClient.isConnected()) {
            try {
                riemannClient.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // TODO: be smart and batch maybe
        final Proto.Event event = Proto.Event.newBuilder()
                .setService(service)
                .setMetricSint64(value)
                .addTags(this.getClass().getSimpleName())
                .setTtl(ttl)
                .setHost(hostName)
                .setState(state.value)
                .build();
        riemannClient.sendEvent(event);
    }

}
