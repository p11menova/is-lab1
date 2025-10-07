package com.example.realtime;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

@ApplicationScoped
public class SseBroadcasterService {

    private Sse sse;
    private SseBroadcaster broadcaster;

    @PostConstruct
    void init() {}

    public synchronized void ensureSse(Sse sse) {
        if (this.sse == null) {
            this.sse = sse;
            this.broadcaster = sse.newBroadcaster();
        }
    }

    public void register(SseEventSink sink) {
        if (broadcaster != null) {
            broadcaster.register(sink);
        } else {

            sink.close();
        }
    }

    public void broadcast(String eventName, String data) {
        if (sse == null || broadcaster == null) return;
        OutboundSseEvent event =
                sse.newEventBuilder().name(eventName).data(String.class, data).build();
        broadcaster.broadcast(event);
    }
}
