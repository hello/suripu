package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryApplicationStore implements ApplicationStore<Application, ClientDetails>{

    private final ConcurrentHashMap<String, Application> apps = new ConcurrentHashMap<String, Application>();
    private final ConcurrentHashMap<Long, String> applicationToUserIds = new ConcurrentHashMap<Long, String>();


    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryApplicationStore.class);

    @Override
    public void storeApplication(final Application application) {
        apps.put(application.clientId, application);
        LOGGER.debug("Registered application {}", application.clientId);
    }

    @Override
    public void activateForAccountId(Application application, Long accountId) {
        // TODO: make sure one account can have multiple applications installed
        applicationToUserIds.put(accountId, application.clientId);
    }

    @Override
    public Optional<Application> getApplication(final ClientDetails clientDetails, final Long accountId) {
        if(!apps.containsKey(clientDetails.clientId)) {
            return Optional.absent();
        }

        final String applicationId = applicationToUserIds.get(accountId);
        if(applicationId == null) {
            LOGGER.warn("Application not found");
            return Optional.absent();
        }

        // TODO : check that this works with multiple applications
        if(!applicationId.equals(clientDetails.clientId)) {
            LOGGER.warn("User {} hasn't installed application with clientId = {}", accountId, clientDetails.clientId);
            return Optional.absent();
        }

        final Application application = apps.get(clientDetails.clientId);

        final Set<OAuthScope> requiredScopes = Sets.newHashSet(clientDetails.scopes);
        final Set<OAuthScope> grantedScopes = Sets.newHashSet(application.scopes);

        if(!grantedScopes.containsAll(requiredScopes)) {
            LOGGER.warn("Scopes not matching required scopes");
            return Optional.absent();
        }

        // We only need the client_secret when not using Password flow
        if(clientDetails.responseType.equals("password") && !application.clientSecret.equals(clientDetails.secret)) {
            LOGGER.warn("Secrets not matching");
            return Optional.absent();
        }

        return Optional.of(application);
    }


}
