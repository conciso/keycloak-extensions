package de.conciso.keycloak.resource.admin;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public class GetUsersByIdResourceProviderFactory implements AdminRealmResourceProviderFactory {
    public static final String ID = "users-by-id";

    @Override
    public AdminRealmResourceProvider create(KeycloakSession keycloakSession) {
        return new GetUsersByIdResourceProvider(keycloakSession);
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }

}
