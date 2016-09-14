package com.hello.suripu.coredropwizard.oauth.stores;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.hello.suripu.coredropwizard.db.ExternalApplicationsDAO;
import com.hello.suripu.coredropwizard.oauth.ExternalApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

//TODO: Add caching
public class PersistentExternalApplicationStore implements ExternalApplicationStore<ExternalApplication>{

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentExternalApplicationStore.class);

    private final ExternalApplicationsDAO applicationsDAO;
    final LoadingCache<String, Optional<ExternalApplication>> nameCache;
    final LoadingCache<Long, Optional<ExternalApplication>> idCache;

    // This is called by the cache when it doesn't contain the key
    final CacheLoader nameCacheLoader = new CacheLoader<String, Optional<ExternalApplication>>() {
        public Optional<ExternalApplication> load(final String applicationName) {
            LOGGER.debug("Application '{}' not in cache, fetching from DB", applicationName);
            return getApplicationByNameFromDB(applicationName);
        }
    };

    final CacheLoader idCacheLoader = new CacheLoader<Long, Optional<ExternalApplication>>() {
        public Optional<ExternalApplication> load(final Long applicationId) {
            LOGGER.debug("Application ID '{}' not in cache, fetching from DB", applicationId);
            return getApplicationByIdFromDB(applicationId);
        }
    };

    public PersistentExternalApplicationStore(final ExternalApplicationsDAO applicationsDAO) {
         this.applicationsDAO = applicationsDAO;

        this.nameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build(nameCacheLoader);
        this.idCache = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build(idCacheLoader);
    }

    public Optional<ExternalApplication> getApplicationByIdFromDB(final Long applicationId) {
        return applicationsDAO.getById(applicationId);
    }

    @Override
    public Optional<ExternalApplication> getApplicationById(final Long applicationId) {
        return idCache.getUnchecked(applicationId);
    }

    @Override
    public Optional<ExternalApplication> getApplicationByClientId(final String clientId) {
        return applicationsDAO.getByClientId(clientId);
    }

    public Optional<ExternalApplication> getApplicationByName(final String applicationName){
        return nameCache.getUnchecked(applicationName);
    }

    public Optional<ExternalApplication> getApplicationByNameFromDB(final String applicationName) {
        return applicationsDAO.getByName(applicationName);
    }

    @Override
    public List<ExternalApplication> getAll() {
        return applicationsDAO.getAll();
    }
}
