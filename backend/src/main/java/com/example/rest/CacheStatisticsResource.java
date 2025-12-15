package com.example.rest;

import com.example.config.CacheStatisticsConfig;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/cache-statistics")
@Produces(MediaType.APPLICATION_JSON)
@jakarta.enterprise.context.RequestScoped
public class CacheStatisticsResource {

    @Inject
    private CacheStatisticsConfig cacheStatisticsConfig;

    @GET
    @Path("/enabled")
    public Response isEnabled() {
        return Response.ok()
                .entity("{\"enabled\":" + cacheStatisticsConfig.isEnabled() + "}")
                .build();
    }

    @POST
    @Path("/enable")
    public Response enable() {
        cacheStatisticsConfig.setEnabled(true);
        return Response.ok()
                .entity("{\"message\":\"Cache statistics logging enabled\",\"enabled\":true}")
                .build();
    }

    @POST
    @Path("/disable")
    public Response disable() {
        cacheStatisticsConfig.setEnabled(false);
        return Response.ok()
                .entity("{\"message\":\"Cache statistics logging disabled\",\"enabled\":false}")
                .build();
    }
}

