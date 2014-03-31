package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.ApplicationsDAO;


public class PersistentApplicationStore implements ApplicationStore<Application, ApplicationRegistration>{

    private final ApplicationsDAO applicationsDAO;
    public PersistentApplicationStore(ApplicationsDAO applicationsDAO) {
         this.applicationsDAO = applicationsDAO;
    }

    @Override
    public Optional<Application> getApplicationById(Long applicationId) {
        Optional<Application> applicationOptional = applicationsDAO.getById(applicationId);
        return applicationOptional;
    }

    @Override
    public Optional<Application> getApplicationByClientId(String clientId) {
        return applicationsDAO.getByClientId(clientId);
    }

    @Override
    public Application register(final ApplicationRegistration registration) {
        final Long id = applicationsDAO.insertRegistration(registration);
        final Application application = Application.fromApplicationRegistration(registration, id);
        return application;
    }

    @Override
    public void activateForAccountId(Application application, Long accountId) {
        applicationsDAO.insertInstallation(application.id, accountId);
    }

    @Override
    public ImmutableList<Application> getApplicationsByDevId(Long accountId) {
        return applicationsDAO.getAllByDevId(accountId);
    }
}
