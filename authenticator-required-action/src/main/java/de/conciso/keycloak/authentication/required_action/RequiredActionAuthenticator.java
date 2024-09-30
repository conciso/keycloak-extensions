package de.conciso.keycloak.authentication.required_action;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class RequiredActionAuthenticator implements Authenticator {

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    context.getUser().addRequiredAction(
      context.getAuthenticatorConfig()
        .getConfig().get(RequiredActionConstants.CONFIG_REQUIRED_ACTION_KEY));
    context.success();
  }

  @Override
  public void action(AuthenticationFlowContext context) {
    //intentionally empty
    context.success();
  }

  @Override
  public boolean requiresUser() {
    return true;
  }

  @Override
  public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
    return true;
  }

  @Override
  public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
    //intentionally empty
  }

  @Override
  public void close() {

  }
}
