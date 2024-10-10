package de.conciso.keycloak.authentication.required_action;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class RequiredActionAuthenticatorFactory implements AuthenticatorFactory {

  public static final String PROVIDER_ID = "required-action-authenticator";

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayType() {
    return "Set Required-Action Authentication";
  }

  @Override
  public String getReferenceCategory() {
    return "required-action";
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
    return new AuthenticationExecutionModel.Requirement[]{
      AuthenticationExecutionModel.Requirement.REQUIRED,
      AuthenticationExecutionModel.Requirement.DISABLED};
  }

  @Override
  public boolean isUserSetupAllowed() {
    return false;
  }

  @Override
  public String getHelpText() {
    return "Sets the configured RequiredAction for the authenticating User";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    //TODO @Robin Maybe multivalued in the future ?
    return List.of(
      new ProviderConfigProperty(
        RequiredActionConstants.CONFIG_REQUIRED_ACTION_KEY,
        "Required Action",
        "Specifies the Required Action, that will be assigned to the authenticating User",
        ProviderConfigProperty.STRING_TYPE,
        "",
        false,
        true)
    );
  }

  @Override
  public Authenticator create(KeycloakSession keycloakSession) {
    return new RequiredActionAuthenticator();
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
}
