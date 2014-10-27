package com.hello.suripu.core.oauth.stores;

import com.google.common.base.Optional;
import com.hello.suripu.core.oauth.Application;
import com.hello.suripu.core.oauth.ApplicationRegistration;
import com.hello.suripu.core.oauth.OAuthScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryApplicationStore implements ApplicationStore<Application, ApplicationRegistration>{

    private final ConcurrentHashMap<String, Application> apps = new ConcurrentHashMap<String, Application>();
    private final ConcurrentHashMap<Long, String> applicationToUserIds = new ConcurrentHashMap<Long, String>();

    private final AtomicLong id = new AtomicLong();

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryApplicationStore.class);

    @Override
    public Application register(final ApplicationRegistration registration) {
        LOGGER.debug("{}", registration);
        final long appId = id.incrementAndGet();
        final Application app = Application.fromApplicationRegistration(registration, id.get());

        apps.put(registration.clientId, app);
        LOGGER.debug("Registered application ({}) {}", app.id, app.clientId);
        return app;
    }

    @Override
    public void activateForAccountId(Application application, Long accountId) {
        // TODO: make sure one account can have multiple applications installed
        applicationToUserIds.put(accountId, application.clientId);
    }

    @Override
    public List<Application> getApplicationsByDevId(Long accountId) {
        return null;
    }

    @Override
    public List<Application> getAll() {
        return new ArrayList<Application>();
    }

    @Override
    public void updateScopes(Long applicationId, List<OAuthScope> scopes) {

    }

    @Override
    public Optional<Application> getApplicationById(final Long applicationId) {
        if(!apps.containsKey(applicationId)) {
            return Optional.absent();
        }

//        final String applicationId = applicationToUserIds.get(accountId);
//        if(applicationId == null) {
//            LOGGER.warn("Application not found");
//            return Optional.absent();
//        }
//
//        // TODO : check that this works with multiple applications
//        if(!applicationId.equals(clientDetails.clientId)) {
//            LOGGER.warn("User {} hasn't installed application with clientId = {}", accountId, clientDetails.clientId);
//            return Optional.absent();
//        }
//
//        final Application application = apps.get(clientDetails.clientId);
//
//        final Set<OAuthScope> requiredScopes = Sets.newHashSet(clientDetails.scopes);
//        final Set<OAuthScope> grantedScopes = Sets.newHashSet(application.scopes);
//
//        if(!grantedScopes.containsAll(requiredScopes)) {
//            LOGGER.warn("Scopes not matching required scopes");
//            return Optional.absent();
//        }
//
//        // We only need the client_secret when not using Password flow
//        if(clientDetails.responseType.equals("password") && !application.clientSecret.equals(clientDetails.secret)) {
//            LOGGER.warn("Secrets not matching");
//            return Optional.absent();
//        }
//
//        return Optional.of(application);

        return Optional.absent();
    }

    @Override
    public Optional<Application> getApplicationByClientId(String clientId) {
        return Optional.absent();
    }


}
