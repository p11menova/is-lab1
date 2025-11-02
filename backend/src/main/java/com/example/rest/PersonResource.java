package com.example.rest;

import com.example.models.Person;
import com.example.repository.PersonRepository;
import com.example.service.UniqueConstraintService;
import com.example.validators.PersonValidator;
import com.example.validators.exceptions.ValidationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/persons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonResource {
    @Inject private PersonRepository personRepository;
    @Inject private UniqueConstraintService uniqueConstraintService;

    @GET
    public Response getAllPersons() {
        List<Person> persons = personRepository.findAll();

        return Response.ok(persons).build();
    }

    @GET
    @Path("/{id}")
    public Response getPersonById(@PathParam("id") Long id) {

        return personRepository
                .findById(id)
                .map(person -> Response.ok(person).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response createPerson(Person person) {
        try {
            PersonValidator.validate(person);
            uniqueConstraintService.validatePersonUniqueness(person);
            Person savedPerson = personRepository.saveOrUpdate(person);
            return Response.status(Response.Status.CREATED).entity(savedPerson).build();
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid input data: " + e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response updatePerson(@PathParam("id") Long id, Person updatedPerson) {
        try {
            PersonValidator.validate(updatedPerson);
            return personRepository
                    .findById(id)
                    .map(
                            existingPerson -> {
                                existingPerson.setName(updatedPerson.getName());
                                existingPerson.setEyeColor(updatedPerson.getEyeColor());
                                existingPerson.setHairColor(updatedPerson.getHairColor());
                                existingPerson.setLocation(updatedPerson.getLocation());
                                existingPerson.setBirthday(updatedPerson.getBirthday());
                                existingPerson.setNationality(updatedPerson.getNationality());

                                // Check uniqueness if name or birthday changed
                                uniqueConstraintService.validatePersonUniqueness(existingPerson);

                                Person result = personRepository.saveOrUpdate(existingPerson);

                                return Response.ok(result).build();
                            })
                    .orElseGet(
                            () -> {
                                updatedPerson.setId(null);
                                Person newPerson = personRepository.saveOrUpdate(updatedPerson);
                                return Response.status(Response.Status.CREATED)
                                        .entity(newPerson)
                                        .build();
                            });
        } catch (ValidationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid input data: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletePerson(@PathParam("id") Long id) {
        personRepository.deleteById(id);

        return Response.noContent().build();
    }

    @GET
    @Path("/operators-zero-oscars")
    public Response operatorsZeroOscars() {

        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
