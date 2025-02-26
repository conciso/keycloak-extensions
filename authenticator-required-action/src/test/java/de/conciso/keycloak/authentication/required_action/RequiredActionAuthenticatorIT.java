package de.conciso.keycloak.authentication.required_action;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.LoggingEvent;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Testcontainers
class RequiredActionAuthenticatorIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequiredActionAuthenticatorIT.class);
  private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "latest");
  private static final String REALM_NAME = "required-action";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "admin";
  private static final String AUTHENTICATOR_CONFIG_ID = "9b46ecb5-2b16-40f0-beae-4fc4ba905292";
  private static final String LOG_AUTHENTICATOR_CONFIG_MISSING_ERROR = "AuthenticatorConfig is missing on RequiredActionAuthenticator";
  private static final String LOG_AUTHENTICATOR_CONFIG_REFERENCES_NON_EXISTENT_REQUIRED_ACTION = "AuthenticatorConfig references an unknown RequiredAction, please double check if it really exists: 'NON_EXISTENT_REQUIRED_ACTION'";

  @Container
  private static final KeycloakContainer keycloak =
      new KeycloakContainer("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
          .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
          .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASS)
          .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
          .withProviderClassesFrom("target/classes")
          .withRealmImportFile("required-action-realm.json");

  private static Stream<Arguments> provideUserLogins() {
    return Stream.of(
        Arguments.of("dieterbohlen", "dietersPassword", "TERMS_AND_CONDITIONS"),
        Arguments.of("mannimammut", "mannimammutsPassword", "UPDATE_PASSWORD"),
        Arguments.of("peterpommes", "peterpommesPassword", "UPDATE_PROFILE")
    );
  }

  @ParameterizedTest
  @MethodSource("provideUserLogins")
  void testThatUsersHaveCorrectRequiredActionAfterLogin(String userName, String password, String requiredAction) {
    // Setting the specific required action to be added to the Authentication Config
    var authenticatorConfig = new AuthenticatorConfigRepresentation();
    authenticatorConfig.setId(AUTHENTICATOR_CONFIG_ID);
    authenticatorConfig.setAlias("RequiredAction");
    var configMap = new HashMap<String, String>();
    configMap.put("REQUIRED_ACTION", requiredAction);
    authenticatorConfig.setConfig(configMap);

    var keycloakAdminClient = keycloak.getKeycloakAdminClient().realm(REALM_NAME);
    keycloakAdminClient.flows().updateAuthenticatorConfig(AUTHENTICATOR_CONFIG_ID, authenticatorConfig);

    assertThat(keycloakAdminClient.users().searchByUsername(userName, true).get(0).getRequiredActions())
        .isEmpty();

    executeBrowserFlow(userName, password, requiredAction);
  }

  void executeBrowserFlow(String userName, String password, String requiredAction) {

    try (Playwright playwright = Playwright.create()) {
      BrowserType chromium = playwright.chromium();
      // comment me in if you want to run in headless mode !
      //Browser browser = chromium.launch(new BrowserType.LaunchOptions().setHeadless(false));
      Browser browser = chromium.launch();
      Page page = browser.newPage();

      loginUser(browser, page, userName, password);

      // split here, for the specific required action
      switch (requiredAction) {
        case "TERMS_AND_CONDITIONS":
          page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Accept")).click();
          break;
        case "UPDATE_PASSWORD":
          page.getByLabel("New Password").fill("Test123!");
          page.getByLabel("Confirm Password").fill("Test123!");
          page.getByLabel("Confirm Password").press("Enter");
          break;
        case "UPDATE_PROFILE":
          assertThat(keycloak.getKeycloakAdminClient().realm(REALM_NAME).users().searchByUsername(userName, true).get(0).getRequiredActions()).containsExactly(requiredAction);
          page.getByText("Submit").click();
          break;
        default:
          Assertions.fail();
      }

      page.waitForURL(String.format("%s/realms/required-action/account/", keycloak.getAuthServerUrl()));

      assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Personal info")).first()).isVisible();

      browser.close();
    }
  }

  @Test
  void testThatFlowIsNotSuccessFulWhenAuthenticatorConfigIsEmpty() throws TimeoutException {
    // Setting the AuthenticatorConfig to empty
    var authenticatorConfig = new AuthenticatorConfigRepresentation();
    authenticatorConfig.setId(AUTHENTICATOR_CONFIG_ID);
    authenticatorConfig.setAlias("RequiredAction");
    var configMap = new HashMap<String, String>();
    configMap.put("REQUIRED_ACTION", "");
    authenticatorConfig.setConfig(configMap);

    var keycloakAdminClient = keycloak.getKeycloakAdminClient().realm(REALM_NAME);
    keycloakAdminClient.flows().updateAuthenticatorConfig(AUTHENTICATOR_CONFIG_ID, authenticatorConfig);

    try (Playwright playwright = Playwright.create()) {
      BrowserType chromium = playwright.chromium();
      Browser browser = chromium.launch();
      Page page = browser.newPage();

      loginUser(browser, page, "dieterbohlen", "dietersPassword");

      assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("We are sorry"))).isVisible();
      assertThat(page.getByText(LOG_AUTHENTICATOR_CONFIG_MISSING_ERROR)).isVisible();
    }

    final List<String> logs = Arrays.stream(keycloak.getLogs().split("\n")).filter(i -> i.contains(LOG_AUTHENTICATOR_CONFIG_MISSING_ERROR)).toList();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).contains(
      "ERROR",
      "[de.conciso.keycloak.authentication.required_action.RequiredActionAuthenticator]",
      "AuthenticatorConfig is missing on RequiredActionAuthenticator"
    );
  }

  @Test
  void testThatErrorIsLoggedWhenAuthenticatorConfigReferencesNonExistentRequiredAction() {
    final String NON_EXISTENT_REQUIRED_ACTION = "NON_EXISTENT_REQUIRED_ACTION";
    // Setting the AuthenticatorConfig to empty
    var authenticatorConfig = new AuthenticatorConfigRepresentation();
    authenticatorConfig.setId(AUTHENTICATOR_CONFIG_ID);
    authenticatorConfig.setAlias("RequiredAction");
    var configMap = new HashMap<String, String>();
    configMap.put("REQUIRED_ACTION", NON_EXISTENT_REQUIRED_ACTION);
    authenticatorConfig.setConfig(configMap);

    var keycloakAdminClient = keycloak.getKeycloakAdminClient().realm(REALM_NAME);
    keycloakAdminClient.flows().updateAuthenticatorConfig(AUTHENTICATOR_CONFIG_ID, authenticatorConfig);

    try (Playwright playwright = Playwright.create()) {
      BrowserType chromium = playwright.chromium();
      Browser browser = chromium.launch();
      Page page = browser.newPage();

      loginUser(browser, page, "dieterbohlen", "dietersPassword");

      assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("We are sorry"))).isVisible();
      assertThat(page.getByText(LOG_AUTHENTICATOR_CONFIG_REFERENCES_NON_EXISTENT_REQUIRED_ACTION)).isVisible();
    }

    final List<String> logs = Arrays.stream(keycloak.getLogs().split("\n")).filter(i -> i.contains(LOG_AUTHENTICATOR_CONFIG_REFERENCES_NON_EXISTENT_REQUIRED_ACTION)).toList();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0)).contains(
      "ERROR",
      "[de.conciso.keycloak.authentication.required_action.RequiredActionAuthenticator]",
      String.format("AuthenticatorConfig references an unknown RequiredAction, please double check if it really exists: '%s'", NON_EXISTENT_REQUIRED_ACTION)
    );

  }

  private void loginUser(Browser browser, Page page, String username, String password) {
    page.navigate(String.format("%s/realms/required-action/account/", keycloak.getAuthServerUrl()));

    page.waitForURL("**/realms/required-action/protocol/openid-connect/auth**");

    // Login Page
    page.getByLabel("Username or email").click();
    page.getByLabel("Username or email").fill(username);

    page.getByLabel("Password").first().fill(password);
    page.getByLabel("Password").first().press("Enter");
  }
}



