package com.example.realtime;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import jakarta.ws.rs.sse.OutboundSseEvent;

@ApplicationScoped
public class SseBroadcasterService {

    private Sse sse;
    private SseBroadcaster broadcaster;

    @PostConstruct
    void init() {
        // Sse is created lazily via setter
    }

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
            // no-op if not initialized yet
            sink.close();
        }
    }

    public void broadcast(String eventName, String data) {
        if (sse == null || broadcaster == null) return;
        OutboundSseEvent event = sse.newEventBuilder().name(eventName).data(String.class, data).build();
        broadcaster.broadcast(event);
    }
}


