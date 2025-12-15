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
import java.io.ByteArrayInputStream;
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

    @Inject private DistributedTransactionService distributedTransactionService;

    private final XmlMapper xmlMapper;

    public ImportService() {
        this.xmlMapper = new XmlMapper();
        this.xmlMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Transactional(value = TxType.REQUIRED, rollbackOn = Exception.class)
    public ImportHistory importMoviesFromXml(
            InputStream inputStream, String username, String fileName) {
        byte[] fileData;
        try {
            fileData = inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream", e);
        }

        DistributedTransactionService.TransactionContext transaction = 
                distributedTransactionService.createTransaction();

        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportHistory.ImportStatus.IN_PROGRESS);
        importHistory.setFileName(fileName);

        try {
            distributedTransactionService.prepareMinIO(
                    transaction,
                    new ByteArrayInputStream(fileData),
                    fileName,
                    "application/xml"
            );

            importHistory = importHistoryService.saveNew(importHistory);
            final ImportHistory finalImportHistory = importHistory;

            MoviesWrapper wrapper = xmlMapper.readValue(new ByteArrayInputStream(fileData), MoviesWrapper.class);
            List<Movie> movies =
                    wrapper.getMovies() != null ? wrapper.getMovies() : new ArrayList<>();

            List<Movie> validMovies = new ArrayList<>();

            for (Movie movie : movies) {
                try {
                    MovieValidator.validate(movie);

                    if (movie.getOperator() != null && movie.getOperator().getId() == null) {
                        PersonValidator.validate(movie.getOperator());
                        uniqueConstraintService.validatePersonUniqueness(movie.getOperator());
                    }
                    if (movie.getDirector() != null && movie.getDirector().getId() == null) {
                        PersonValidator.validate(movie.getDirector());
                        uniqueConstraintService.validatePersonUniqueness(movie.getDirector());
                    }
                    if (movie.getScreenwriter() != null
                            && movie.getScreenwriter().getId() == null) {
                        PersonValidator.validate(movie.getScreenwriter());
                        uniqueConstraintService.validatePersonUniqueness(movie.getScreenwriter());
                    }

                    uniqueConstraintService.validateMovieUniqueness(movie);

                    validMovies.add(movie);
                } catch (ValidationException e) {
                    throw new ValidationException("Validation failed for movie: " + e.getMessage());
                }
            }

            // Фаза 2: Commit (DB операции выполняются здесь)
            final int[] importedCount = {0};
            distributedTransactionService.prepareDB(transaction, () -> {
                for (Movie movie : validMovies) {
                    if (movie.getOperator() != null && movie.getOperator().getId() == null) {
                        movie.setOperator(personRepository.saveOrUpdate(movie.getOperator()));
                    }
                    if (movie.getDirector() != null && movie.getDirector().getId() == null) {
                        movie.setDirector(personRepository.saveOrUpdate(movie.getDirector()));
                    }
                    if (movie.getScreenwriter() != null && movie.getScreenwriter().getId() == null) {
                        movie.setScreenwriter(personRepository.saveOrUpdate(movie.getScreenwriter()));
                    }
                    movieRepository.saveOrUpdate(movie);
                    importedCount[0]++;
                }

                finalImportHistory.setStatus(ImportHistory.ImportStatus.SUCCESS);
                finalImportHistory.setObjectsCount(importedCount[0]);
                finalImportHistory.setFileStorageKey(transaction.getMinioObjectKey());
                importHistoryRepository.save(finalImportHistory);
            });
            distributedTransactionService.commit(transaction);

            return finalImportHistory;
        } catch (Exception e) {
            // Rollback транзакции
            distributedTransactionService.rollback(transaction);

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
        // Читаем InputStream в память для повторного использования
        byte[] fileData;
        try {
            fileData = inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input stream", e);
        }

        DistributedTransactionService.TransactionContext transaction = 
                distributedTransactionService.createTransaction();

        ImportHistory importHistory = new ImportHistory();
        importHistory.setUsername(username);
        importHistory.setStatus(ImportHistory.ImportStatus.IN_PROGRESS);
        importHistory.setFileName(fileName);

        try {
            distributedTransactionService.prepareMinIO(
                    transaction,
                    new ByteArrayInputStream(fileData),
                    fileName,
                    "application/xml"
            );

            importHistory = importHistoryService.saveNew(importHistory);
            final ImportHistory finalImportHistory = importHistory;

            PersonsWrapper wrapper = xmlMapper.readValue(new ByteArrayInputStream(fileData), PersonsWrapper.class);
            List<Person> persons =
                    wrapper.getPersons() != null ? wrapper.getPersons() : new ArrayList<>();

            for (Person person : persons) {
                try {
                    PersonValidator.validate(person);
                    uniqueConstraintService.validatePersonUniqueness(person);
                } catch (ValidationException e) {
                    throw new ValidationException(
                            "Validation failed for person: " + e.getMessage());
                }
            }

            final int[] importedCount = {0};
            distributedTransactionService.prepareDB(transaction, () -> {
                for (Person person : persons) {
                    personRepository.saveOrUpdate(person);
                    importedCount[0]++;
                }

                finalImportHistory.setStatus(ImportHistory.ImportStatus.SUCCESS);
                finalImportHistory.setObjectsCount(importedCount[0]);
                finalImportHistory.setFileStorageKey(transaction.getMinioObjectKey());
                importHistoryRepository.save(finalImportHistory);
            });
            distributedTransactionService.commit(transaction);

            return finalImportHistory;
        } catch (Exception e) {
            distributedTransactionService.rollback(transaction);

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
