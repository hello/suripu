package com.hello.suripu.search;


import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class ElasticSearchTransportClient {
    private static final String DEFAULT_CLUSTER = "elasticsearch";
    private static final Boolean DEFAULT_SNIFF_ENABLED = true;
    private static final String DEFFAULT_TIMEOUT = "7s";

    private final String host;
    private final Integer port;
    private final String cluster;
    private final Boolean sniffEnabled;
    private final String timeout;

    public ElasticSearchTransportClient(final String host, final Integer port, final String cluster, final Boolean sniffEnabled, final String timeout) {
        this.host = host;
        this.port = port;
        this.cluster = cluster;
        this.sniffEnabled = sniffEnabled;
        this.timeout = timeout;
    }

    public static ElasticSearchTransportClient createWithDefaulSettings(final String host, final Integer port) {
        return new ElasticSearchTransportClient(host, port, DEFAULT_CLUSTER, DEFAULT_SNIFF_ENABLED, DEFFAULT_TIMEOUT);
    }

    public TransportClient generateClient() {
        final Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", cluster)
                .put("client.transport.sniff", sniffEnabled)
                .put("client.transport.ping_timeout", timeout)
                .build();
        return new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(host, port));
    }
}
