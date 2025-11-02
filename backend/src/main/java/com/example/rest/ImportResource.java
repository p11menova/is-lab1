package com.example.rest;

import com.example.models.ImportHistory;
import com.example.repository.ImportHistoryRepository;
import com.example.service.ImportService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
public class ImportResource {

    @Inject
    private ImportService importService;

    @Inject
    private ImportHistoryRepository importHistoryRepository;

    @POST
    @Path("/movies")
    @Consumes({MediaType.APPLICATION_XML, "application/xml", "text/xml"})
    public Response importMovies(java.io.InputStream inputStream, @QueryParam("filename") @DefaultValue("movies_import.xml") String fileName) {
        try {
            String username = "user";
            ImportHistory result = importService.importMoviesFromXml(inputStream, username, fileName);

            return Response.status(Response.Status.CREATED)
                    .entity(result)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Import failed: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/persons")
    @Consumes({MediaType.APPLICATION_XML, "application/xml", "text/xml"})
    public Response importPersons(java.io.InputStream inputStream, @QueryParam("filename") @DefaultValue("persons_import.xml") String fileName) {
        try {
            String username = "user";
            ImportHistory result = importService.importPersonsFromXml(inputStream, username, fileName);

            return Response.status(Response.Status.CREATED)
                    .entity(result)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Import failed: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/history")
    public Response getImportHistory(@QueryParam("username") String username, @QueryParam("admin") @DefaultValue("false") boolean admin) {
        List<ImportHistory> history;
        if (admin || username == null) {
            // Admin sees all imports
            history = importHistoryRepository.findAll();
        } else {
            // Regular user sees only their imports
            history = importHistoryRepository.findByUsername(username);
        }
        return Response.ok(history).build();
    }

    @GET
    @Path("/history/{id}")
    public Response getImportHistoryById(@PathParam("id") Long id) {
        return importHistoryRepository.findById(id)
                .map(history -> Response.ok(history).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

}

