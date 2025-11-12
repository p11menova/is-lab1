package com.example.service;

import com.example.models.ImportHistory;
import com.example.repository.ImportHistoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class ImportHistoryService {

    @Inject ImportHistoryRepository repository;

    @Transactional(TxType.REQUIRES_NEW)
    public ImportHistory saveNew(ImportHistory history) {
        return repository.save(history);
    }
}
