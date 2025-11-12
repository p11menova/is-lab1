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

    @Inject private MovieRepository movieRepository;

    @Inject private PersonRepository personRepository;

    @Inject private ImportHistoryRepository importHistoryRepository;

    @Inject private ImportHistoryService importHistoryService;

    @Inject private UniqueConstraintService uniqueConstraintService;

    private final XmlMapper xmlMapper;

    public ImportService() {
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
    public ImportHistory importMoviesFromXml(
            InputStream inputStream, String username, String fileName) {
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportHistory.ImportStatus.IN_PROGRESS);
        importHistory.setFileName(fileName);
        importHistory = importHistoryService.saveNew(importHistory);

        try {

            MoviesWrapper wrapper = xmlMapper.readValue(inputStream, MoviesWrapper.class);
            List<Movie> movies =
                    wrapper.getMovies() != null ? wrapper.getMovies() : new ArrayList<>();

            List<Movie> validMovies = new ArrayList<>();
            int importedCount = 0;

            for (Movie movie : movies) {
                try {
                    MovieValidator.validate(movie);

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
                    if (movie.getScreenwriter() != null
                            && movie.getScreenwriter().getId() == null) {
                        PersonValidator.validate(movie.getScreenwriter());
                        uniqueConstraintService.validatePersonUniqueness(movie.getScreenwriter());
                        movie.setScreenwriter(
                                personRepository.saveOrUpdate(movie.getScreenwriter()));
                    }

                    uniqueConstraintService.validateMovieUniqueness(movie);

                    validMovies.add(movie);
                } catch (ValidationException e) {
                    throw new ValidationException("Validation failed for movie: " + e.getMessage());
                }
            }

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

                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "Unknown error occurred during import.";
                } else {
                    errorMsg = errorMsg.length() > 5000 ? errorMsg.substring(0, 5000) : errorMsg;
                }
                importHistory.setImportDate(java.time.LocalDateTime.now());
                importHistory.setErrorMessage(errorMsg);
                System.out.println("Import failed: " + errorMsg);
                importHistoryService.saveNew(importHistory);
            } catch (Exception saveError) {
                System.err.println("Failed to save import history: " + saveError.getMessage());
                saveError.printStackTrace();
            }

            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    @Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
    public ImportHistory importPersonsFromXml(
            InputStream inputStream, String username, String fileName) {
        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportHistory.ImportStatus.IN_PROGRESS);
        importHistory.setFileName(fileName);
        importHistory = importHistoryService.saveNew(importHistory);

        try {
            PersonsWrapper wrapper = xmlMapper.readValue(inputStream, PersonsWrapper.class);
            List<Person> persons =
                    wrapper.getPersons() != null ? wrapper.getPersons() : new ArrayList<>();

            int importedCount = 0;

            for (Person person : persons) {
                try {
                    PersonValidator.validate(person);
                    uniqueConstraintService.validatePersonUniqueness(person);
                } catch (ValidationException e) {
                    throw new ValidationException(
                            "Validation failed for person: " + e.getMessage());
                }
            }

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

                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = "Unknown error occurred during person import.";
                } else {
                    errorMsg = errorMsg.length() > 5000 ? errorMsg.substring(0, 5000) : errorMsg;
                }
                importHistory.setErrorMessage(errorMsg);
                importHistory.setImportDate(java.time.LocalDateTime.now());
                importHistoryService.saveNew(importHistory);
            } catch (Exception saveError) {
                System.err.println("Failed to save import history: " + saveError.getMessage());
                saveError.printStackTrace();
            }

            throw new RuntimeException("Person import failed: " + e.getMessage(), e);
        }
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void updateImportHistoryStatus(ImportHistory importHistory) {
        importHistoryRepository.save(importHistory);
    }
}
