package com.example.rest;

import com.example.models.Movie;
import com.example.realtime.SseBroadcasterService;
import com.example.repository.MovieRepository;
import com.example.service.UniqueConstraintService;
import com.example.validators.MovieValidator;
import com.example.validators.exceptions.ValidationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/movies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MoviesResource {

    @Inject private MovieRepository movieRepository;
    @Inject private SseBroadcasterService sseService;
    @Inject private UniqueConstraintService uniqueConstraintService;

    @POST
    public Response createMovie(Movie movie) {
        try {
            MovieValidator.validate(movie);
            uniqueConstraintService.validateMovieUniqueness(movie);
            Movie newMovie = movieRepository.saveOrUpdate(movie);
            sseService.broadcast("movie-created", String.valueOf(newMovie.getId()));
            return Response.status(Response.Status.CREATED).entity(newMovie).build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid input data: " + e.getMessage())
                    .build();
        }
    }

    @GET
    public List<Movie> getAllMovies(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortOrder") String sortOrder,
            @QueryParam("name") String name,
            @QueryParam("genre") String genre,
            @QueryParam("mpaa") String mpaa,
            @QueryParam("operator") String operator,
            @QueryParam("director") String director,
            @QueryParam("screenwriter") String screenwriter) {
        return movieRepository.findPagedFilteredSorted(
                page, size, sortBy, sortOrder, name, genre, mpaa, operator, director, screenwriter);
    }

    @GET
    @Path("/{id}")
    public Response getMovieById(@PathParam("id") Long id) {
        return movieRepository
                .findById(id)
                .map(movie -> Response.ok(movie).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{id}")
    public Response updateMovie(@PathParam("id") Long id, Movie movieDetails) {

        try {
            MovieValidator.validate(movieDetails);

            return movieRepository
                    .findById(id)
                    .map(
                            existingMovie -> {
                                if (movieDetails.getName() != null) {
                                    existingMovie.setName(movieDetails.getName());
                                }
                                if (movieDetails.getOscarsCount() != null) {
                                    existingMovie.setOscarsCount(movieDetails.getOscarsCount());
                                }
                                if (movieDetails.getBudget() != null) {
                                    existingMovie.setBudget(movieDetails.getBudget());
                                }
                                if (movieDetails.getTotalBoxOffice() != null) {
                                    existingMovie.setTotalBoxOffice(
                                            movieDetails.getTotalBoxOffice());
                                }
                                if (movieDetails.getLength() != null) {
                                    existingMovie.setLength(movieDetails.getLength());
                                }

                                if (movieDetails.getGoldenPalmCount() > 0) {
                                    existingMovie.setGoldenPalmCount(
                                            movieDetails.getGoldenPalmCount());
                                }

                                if (movieDetails.getMpaaRating() != null) {
                                    existingMovie.setMpaaRating(movieDetails.getMpaaRating());
                                }
                                if (movieDetails.getGenre() != null) {
                                    existingMovie.setGenre(movieDetails.getGenre());
                                }

                                if (movieDetails.getCoordinates() != null) {
                                    existingMovie
                                            .getCoordinates()
                                            .setX(movieDetails.getCoordinates().getX());
                                    existingMovie
                                            .getCoordinates()
                                            .setY(movieDetails.getCoordinates().getY());
                                }

                                if (movieDetails.getDirector() != null) {
                                    existingMovie.setDirector(movieDetails.getDirector());
                                }
                                if (movieDetails.getScreenwriter() != null) {
                                    existingMovie.setScreenwriter(movieDetails.getScreenwriter());
                                }
                                if (movieDetails.getOperator() != null) {
                                    existingMovie.setOperator(movieDetails.getOperator());
                                }

                                // Check uniqueness after all fields are updated
                                uniqueConstraintService.validateMovieUniqueness(existingMovie);

                                Movie updatedMovie = movieRepository.saveOrUpdate(existingMovie);

                                sseService.broadcast(
                                        "movie-updated", String.valueOf(updatedMovie.getId()));
                                return Response.ok(updatedMovie).build();
                            })
                    .orElse(Response.status(Response.Status.NOT_FOUND).build());
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid input data: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteMovie(@PathParam("id") Long id) {
        movieRepository.deleteById(id);
        sseService.broadcast("movie-deleted", String.valueOf(id));

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("/group-by-mpaa")
    public Response groupByMpaa() {
        return Response.ok(movieRepository.fnCountByMpaaRating()).build();
    }

    @GET
    @Path("/count-genre-gt")
    public Response countGenreGt(@QueryParam("threshold") String threshold) {
        return Response.ok(movieRepository.fnCountGenreGreaterThan(threshold)).build();
    }

    @GET
    @Path("/movies-genre-lt")
    public Response moviesGenreLt(@QueryParam("threshold") String threshold) {
        return Response.ok(movieRepository.fnMoviesGenreLessThan(threshold)).build();
    }

    @GET
    @Path("/zero-oscars")
    public Response moviesZeroOscars() {
        return Response.ok(movieRepository.fnMoviesZeroOscars()).build();
    }

    @GET
    @Path("/operators-zero-oscars")
    public Response operatorsZeroOscars() {
        return Response.ok(movieRepository.fnOperatorsWithZeroOscars()).build();
    }
}
