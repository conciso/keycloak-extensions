package de.conciso.keycloak.authentication.required_action;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Optional;

public class RequiredActionAuthenticator implements Authenticator {

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    var authenticatorConfig = Optional.ofNullable(
      context.getAuthenticatorConfig()
        .getConfig()
        .get(RequiredActionConstants.CONFIG_REQUIRED_ACTION_KEY));
    if (authenticatorConfig.isEmpty()) {
      context.failure(AuthenticationFlowError.INTERNAL_ERROR,
        Response.serverError().entity("AuthenticatorConfig is missing REQUIRED_ACTION").build(),
        "",
        "AuthenticatorConfig is missing REQUIRED_ACTION");
      // TODO instead of sending a Response to the user, show real error page.
      // something like: AuthenticatorConfig is missing REQUIRED_ACTION attribute
      // also post to the ErrorLog
    } else {
      context.getUser().addRequiredAction(authenticatorConfig.get());
      context.success();
    }
  }

  @Override
  public void action(AuthenticationFlowContext context) {}

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
