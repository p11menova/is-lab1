package com.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

@ApplicationScoped
public class DistributedTransactionService {

    private static final Logger logger = Logger.getLogger(DistributedTransactionService.class.getName());

    @Inject
    private MinIOService minIOService;

    public enum TransactionState {
        PREPARING,
        PREPARED,
        COMMITTING,
        COMMITTED,
        ROLLING_BACK,
        ROLLED_BACK
    }

    public static class TransactionContext {
        private TransactionState state = TransactionState.PREPARING;
        private String minioObjectKey;
        private List<Runnable> dbOperations = new ArrayList<>();
        private List<Runnable> minioOperations = new ArrayList<>();
        private Exception error;

        public TransactionState getState() {
            return state;
        }

        public void setState(TransactionState state) {
            this.state = state;
        }

        public String getMinioObjectKey() {
            return minioObjectKey;
        }

        public void setMinioObjectKey(String minioObjectKey) {
            this.minioObjectKey = minioObjectKey;
        }

        public List<Runnable> getDbOperations() {
            return dbOperations;
        }

        public List<Runnable> getMinioOperations() {
            return minioOperations;
        }

        public Exception getError() {
            return error;
        }

        public void setError(Exception error) {
            this.error = error;
        }
    }

    public TransactionContext createTransaction() {
        return new TransactionContext();
    }

    public void prepareMinIO(TransactionContext context, InputStream inputStream, String fileName, String contentType) throws Exception {
        if (context.getState() != TransactionState.PREPARING) {
            throw new IllegalStateException("Transaction is not in PREPARING state");
        }

        logger.info("Phase 1 (Prepare): Uploading file to MinIO - " + fileName);
        try {
            String objectKey = minIOService.uploadFile(inputStream, fileName, contentType);
            context.setMinioObjectKey(objectKey);
            context.getMinioOperations().add(() -> {
                try {
                    logger.info("Rollback: Deleting file from MinIO - " + objectKey);
                    minIOService.deleteFile(objectKey);
                    logger.info("Rollback: File deleted from MinIO - " + objectKey);
                } catch (Exception e) {
                    logger.severe("Failed to rollback MinIO operation: " + e.getMessage());
                }
            });
            logger.info("Phase 1 (Prepare): MinIO prepared successfully - " + objectKey);
        } catch (Exception e) {
            logger.severe("Phase 1 (Prepare): MinIO preparation failed - " + e.getMessage());
            context.setError(e);
            context.setState(TransactionState.ROLLING_BACK);
            rollback(context);
            throw e;
        }
    }

    public void prepareDB(TransactionContext context, Runnable dbOperation) {
        if (context.getState() != TransactionState.PREPARING) {
            throw new IllegalStateException("Transaction is not in PREPARING state");
        }

        context.getDbOperations().add(dbOperation);
    }

    public void commit(TransactionContext context) throws Exception {
        if (context.getState() != TransactionState.PREPARING && context.getState() != TransactionState.PREPARED) {
            throw new IllegalStateException("Transaction is not ready for commit");
        }

        context.setState(TransactionState.COMMITTING);
        logger.info("Phase 2 (Commit): Starting commit. DB operations to execute: " + context.getDbOperations().size());

        try {
            // Выполняем DB операции (они уже в транзакции JTA)
            int dbOpCount = 0;
            for (Runnable dbOp : context.getDbOperations()) {
                dbOpCount++;
                logger.info("Phase 2 (Commit): Executing DB operation " + dbOpCount + "/" + context.getDbOperations().size());
                dbOp.run();
            }

            // MinIO уже загружен в prepare, просто подтверждаем
            context.setState(TransactionState.COMMITTED);
            logger.info("Phase 2 (Commit): Transaction committed successfully. MinIO file: " + context.getMinioObjectKey());
        } catch (Exception e) {
            logger.severe("Phase 2 (Commit): Commit failed - " + e.getMessage());
            context.setError(e);
            context.setState(TransactionState.ROLLING_BACK);
            rollback(context);
            throw e;
        }
    }

    public void rollback(TransactionContext context) {
        if (context.getState() == TransactionState.ROLLED_BACK) {
            logger.info("Transaction already rolled back");
            return;
        }

        context.setState(TransactionState.ROLLING_BACK);
        logger.info("Starting rollback for transaction. MinIO operations to rollback: " + context.getMinioOperations().size());

        // Откатываем MinIO операции
        int rollbackCount = 0;
        for (Runnable minioOp : context.getMinioOperations()) {
            try {
                minioOp.run();
                rollbackCount++;
                logger.info("MinIO operation rolled back successfully (" + rollbackCount + "/" + context.getMinioOperations().size() + ")");
            } catch (Exception e) {
                logger.severe("Error during MinIO rollback: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // DB операции откатятся автоматически через JTA транзакцию
        logger.info("DB operations will be rolled back automatically by JTA transaction");

        context.setState(TransactionState.ROLLED_BACK);
        logger.info("Transaction rolled back successfully. Total MinIO operations rolled back: " + rollbackCount);
    }
}
