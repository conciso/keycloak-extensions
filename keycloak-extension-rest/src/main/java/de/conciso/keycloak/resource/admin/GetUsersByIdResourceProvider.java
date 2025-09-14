package de.conciso.keycloak.resource.admin;

import java.util.Map;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class GetUsersByIdResourceProvider implements AdminRealmResourceProvider,
    ServerInfoAwareProviderFactory {

  private final KeycloakSession session;

  public GetUsersByIdResourceProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public void close() {

  }

  @Override
  public Map<String, String> getOperationalInfo() {
    String version = getClass().getPackage().getImplementationVersion();
    return Map.of("Version", version);
  }

  @Override
  public Object getResource(KeycloakSession keycloakSession, RealmModel realmModel, AdminPermissionEvaluator adminPermissionEvaluator, AdminEventBuilder adminEventBuilder) {
    return new GetUsersByIdResource(session, adminPermissionEvaluator);
  }
}
