package com.example.rest;

import com.example.realtime.SseBroadcasterService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

@Path("/events")
public class EventsResource {

    @Inject private SseBroadcasterService sseService;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@jakarta.ws.rs.core.Context Sse sse, @jakarta.ws.rs.core.Context SseEventSink sink) {
        sseService.ensureSse(sse);
        sseService.register(sink);
    }
}


