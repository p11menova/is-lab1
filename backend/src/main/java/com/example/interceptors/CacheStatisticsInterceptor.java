package com.example.interceptors;

import com.example.config.CacheStatisticsConfig;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;

import java.util.logging.Logger;

@Interceptor
@CacheStatisticsLogging
public class CacheStatisticsInterceptor {

    private static final Logger logger = Logger.getLogger(CacheStatisticsInterceptor.class.getName());

    @Inject
    private CacheStatisticsConfig cacheStatisticsConfig;

    @PersistenceContext
    private EntityManager entityManager;

    @AroundInvoke
    public Object logCacheStatistics(InvocationContext context) throws Exception {
        if (!cacheStatisticsConfig.isEnabled()) {
            return context.proceed();
        }

        // Получаем Statistics через EntityManager
        Statistics stats = null;
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session != null) {
                stats = session.getSessionFactory().getStatistics();
            }
        } catch (Exception e) {
            logger.warning("Failed to get Hibernate statistics: " + e.getMessage());
            return context.proceed();
        }

        if (stats == null) {
            return context.proceed();
        }
        
        long beforeHits = stats.getSecondLevelCacheHitCount();
        long beforeMisses = stats.getSecondLevelCacheMissCount();
        long beforeQueries = stats.getQueryCacheHitCount() + stats.getQueryCacheMissCount();

        Object result = context.proceed();

        long afterHits = stats.getSecondLevelCacheHitCount();
        long afterMisses = stats.getSecondLevelCacheMissCount();
        long afterQueries = stats.getQueryCacheHitCount() + stats.getQueryCacheMissCount();

        long hits = afterHits - beforeHits;
        long misses = afterMisses - beforeMisses;
        long queries = afterQueries - beforeQueries;

        // Логируем всегда, даже если нет изменений, чтобы показать работу interceptor
        logger.info(String.format(
            "Cache Statistics for %s.%s: L2 Hits=%d, L2 Misses=%d, Query Cache Operations=%d (Total: Hits=%d, Misses=%d)",
            context.getMethod().getDeclaringClass().getSimpleName(),
            context.getMethod().getName(),
            hits,
            misses,
            queries,
            afterHits,
            afterMisses
        ));

        return result;
    }
}
