package com.example.service;

import com.example.models.ImportHistory;
import com.example.models.Movie;
import com.example.models.Person;
import com.example.repository.ImportHistoryRepository;
import com.example.repository.MovieRepository;
import com.example.repository.PersonRepository;
import com.example.validators.MovieValidator;
import com.example.validators.PersonValidator;
import com.example.validators.exceptions.ValidationException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ImportService {

    @Inject
    private MovieRepository movieRepository;

    @Inject
    private PersonRepository personRepository;

    @Inject
    private ImportHistoryRepository importHistoryRepository;

    @Inject
    private UniqueConstraintService uniqueConstraintService;

    private final XmlMapper xmlMapper;

    public ImportService() {
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
    public ImportHistory importMoviesFromXml(InputStream inputStream, String username, String fileName) {
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportHistory.ImportStatus.IN_PROGRESS);
        importHistory.setFileName(fileName);
        importHistory = importHistoryRepository.save(importHistory);

        try {
            // Parse XML to list of movies
            MoviesWrapper wrapper = xmlMapper.readValue(inputStream, MoviesWrapper.class);
            List<Movie> movies = wrapper.getMovies() != null ? wrapper.getMovies() : new ArrayList<>();
            
            List<Movie> validMovies = new ArrayList<>();
            int importedCount = 0;

            // Validate all movies first
            for (Movie movie : movies) {
                try {
                    MovieValidator.validate(movie);
                    
                    // Ensure related persons exist or create them first
                    if (movie.getOperator() != null && movie.getOperator().getId() == null) {
                        PersonValidator.validate(movie.getOperator());
                        uniqueConstraintService.validatePersonUniqueness(movie.getOperator());
                        movie.setOperator(personRepository.saveOrUpdate(movie.getOperator()));
                    }
                    if (movie.getDirector() != null && movie.getDirector().getId() == null) {
                        PersonValidator.validate(movie.getDirector());
                        uniqueConstraintService.validatePersonUniqueness(movie.getDirector());
                        movie.setDirector(personRepository.saveOrUpdate(movie.getDirector()));
                    }
                    if (movie.getScreenwriter() != null && movie.getScreenwriter().getId() == null) {
                        PersonValidator.validate(movie.getScreenwriter());
                        uniqueConstraintService.validatePersonUniqueness(movie.getScreenwriter());
                        movie.setScreenwriter(personRepository.saveOrUpdate(movie.getScreenwriter()));
                    }

                    // Check movie uniqueness after persons are created/found
                    uniqueConstraintService.validateMovieUniqueness(movie);

                    validMovies.add(movie);
                } catch (ValidationException e) {
                    throw new ValidationException("Validation failed for movie: " + e.getMessage());
                }
            }

            // Save all movies in transaction
            for (Movie movie : validMovies) {
                movieRepository.saveOrUpdate(movie);
                importedCount++;
            }

            importHistory.setStatus(ImportHistory.ImportStatus.SUCCESS);
            importHistory.setObjectsCount(importedCount);
            importHistoryRepository.save(importHistory);

            return importHistory;
        } catch (Exception e) {
            try {
                importHistory.setStatus(ImportHistory.ImportStatus.FAILED);
                importHistory.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(2000, e.getMessage().length())) : "Unknown error");
                importHistoryRepository.save(importHistory);
            } catch (Exception saveError) {
                // Ignore save error, log if needed
                System.err.println("Failed to save import history: " + saveError.getMessage());
            }
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    @Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
    public ImportHistory importPersonsFromXml(InputStream inputStream, String username, String fileName) {
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportHistory.ImportStatus.IN_PROGRESS);
        importHistory.setFileName(fileName);
        importHistory = importHistoryRepository.save(importHistory);

        try {
            // Parse XML to list of persons
            PersonsWrapper wrapper = xmlMapper.readValue(inputStream, PersonsWrapper.class);
            List<Person> persons = wrapper.getPersons() != null ? wrapper.getPersons() : new ArrayList<>();
            
            int importedCount = 0;

            // Validate all persons first
            for (Person person : persons) {
                try {
                    PersonValidator.validate(person);
                    uniqueConstraintService.validatePersonUniqueness(person);
                } catch (ValidationException e) {
                    throw new ValidationException("Validation failed for person: " + e.getMessage());
                }
            }

            // Save all persons in transaction
            for (Person person : persons) {
                personRepository.saveOrUpdate(person);
                importedCount++;
            }

            importHistory.setStatus(ImportHistory.ImportStatus.SUCCESS);
            importHistory.setObjectsCount(importedCount);
            importHistoryRepository.save(importHistory);

            return importHistory;
        } catch (Exception e) {
            try {
                importHistory.setStatus(ImportHistory.ImportStatus.FAILED);
                importHistory.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(2000, e.getMessage().length())) : "Unknown error");
                importHistoryRepository.save(importHistory);
            } catch (Exception saveError) {
                // Ignore save error, log if needed
                System.err.println("Failed to save import history: " + saveError.getMessage());
            }
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }
}

