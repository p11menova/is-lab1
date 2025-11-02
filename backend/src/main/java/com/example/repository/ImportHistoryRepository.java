package com.example.repository;

import com.example.models.ImportHistory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ImportHistoryRepository {

    @PersistenceContext(unitName = "my-pu")
    private EntityManager em;

    @Transactional
    public ImportHistory save(ImportHistory importHistory) {
        return em.merge(importHistory);
    }

    public Optional<ImportHistory> findById(Long id) {
        return Optional.ofNullable(em.find(ImportHistory.class, id));
    }

    public List<ImportHistory> findAll() {
        return em.createQuery("SELECT ih FROM ImportHistory ih ORDER BY ih.importDate DESC", ImportHistory.class)
                .getResultList();
    }

    public List<ImportHistory> findByUsername(String username) {
        return em.createQuery("SELECT ih FROM ImportHistory ih WHERE ih.username = :username ORDER BY ih.importDate DESC", ImportHistory.class)
                .setParameter("username", username)
                .getResultList();
    }
}

