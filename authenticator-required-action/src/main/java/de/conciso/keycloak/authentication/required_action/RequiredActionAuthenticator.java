package de.conciso.keycloak.authentication.required_action;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Optional;

public class RequiredActionAuthenticator implements Authenticator {
  private static final Logger LOGGER = Logger.getLogger(RequiredActionAuthenticator.class);
  private static final String LOG_ERROR_MESSAGE_MISSING_AUTH_CONFIG = "AuthenticatorConfig is missing on RequiredActionAuthenticator";
  private static final String HTML_ERROR_MESSAGE = LOG_ERROR_MESSAGE_MISSING_AUTH_CONFIG + "!\nPlease contact your administrator";
  private static final String LOG_ERROR_MESSAGE_NON_EXISTENT_REQUIRED_ACTION = "AuthenticatorConfig references an unknown RequiredAction, please double check if it really exists: 'NON_EXISTENT_REQUIRED_ACTION'";


  @Override
  public void authenticate(AuthenticationFlowContext context) {
    var authenticatorConfig = Optional.ofNullable(
      context.getAuthenticatorConfig()
        .getConfig()
        .get(RequiredActionConstants.CONFIG_REQUIRED_ACTION_KEY));
    if (authenticatorConfig.isEmpty()) {
      LOGGER.error(LOG_ERROR_MESSAGE_MISSING_AUTH_CONFIG);
      context.getEvent()
        .realm(context.getRealm())
        .client(context.getSession().getContext().getClient())
        .user(context.getUser())
        .error(Errors.INVALID_CONFIG);
      context.failure(AuthenticationFlowError.INTERNAL_ERROR,
        htmlErrorResponse(context,
          LOG_ERROR_MESSAGE_MISSING_AUTH_CONFIG + HTML_ERROR_MESSAGE));
    } else if (!doesRequiredActionExists(context, authenticatorConfig.get())) {
      LOGGER.error(LOG_ERROR_MESSAGE_NON_EXISTENT_REQUIRED_ACTION);
      context.getEvent()
        .realm(context.getRealm())
        .client(context.getSession().getContext().getClient())
        .user(context.getUser())
        .error(Errors.INVALID_CONFIG);
      context.failure(AuthenticationFlowError.INTERNAL_ERROR,
        htmlErrorResponse(context,
          LOG_ERROR_MESSAGE_NON_EXISTENT_REQUIRED_ACTION + HTML_ERROR_MESSAGE));
    } else {
      context.getUser().addRequiredAction(authenticatorConfig.get());
      context.success();
    }
  }

  private boolean doesRequiredActionExists(AuthenticationFlowContext context, String providerId) {
    var requiredAction = context.getRealm().getRequiredActionProvidersStream()
      .map(RequiredActionProviderModel::getProviderId)
      .filter(id -> id.equals(providerId))
      .findFirst();
    return requiredAction.isPresent();
  }

  private Response htmlErrorResponse(AuthenticationFlowContext context, String errorMessage) {
    AuthenticationSessionModel authSession = context.getAuthenticationSession();
    return context.form()
      .setError(errorMessage, authSession.getAuthenticatedUser().getUsername(),
        authSession.getClient().getClientId())
      .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @Override
  public void action(AuthenticationFlowContext context) {
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
