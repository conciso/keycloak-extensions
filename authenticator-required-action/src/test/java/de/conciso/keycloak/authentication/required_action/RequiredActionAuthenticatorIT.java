package de.conciso.keycloak.authentication.required_action;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RequiredActionAuthenticatorIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequiredActionAuthenticatorIT.class);
  private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "latest");
  private static final String REALM_NAME = "required-action";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "admin";
  private static final String AUTHENTICATOR_CONFIG_ID = "9b46ecb5-2b16-40f0-beae-4fc4ba905292";

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
        Arguments.of("peterpommes", "peterpommesPassword", "VERIFY_PROFILE")
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
      page.navigate(String.format("%s/realms/required-action/account/", keycloak.getAuthServerUrl()));

      page.getByText("Signing in").click();
      page.waitForURL("**/realms/required-action/protocol/openid-connect/auth**");

      // Login Page
      page.getByLabel("Username or email").click();
      page.getByLabel("Username or email").fill(userName);

      page.getByLabel("Password").first().fill(password);
      page.getByLabel("Password").first().press("Enter");

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
        case "VERIFY_PROFILE":
          assertThat(keycloak.getKeycloakAdminClient().realm(REALM_NAME).users().searchByUsername(userName, true).get(0).getRequiredActions()).containsExactly(requiredAction);
          break;
        default:
          Assertions.fail();
      }

      page.waitForURL(String.format("%s/realms/required-action/account/#/security/signingin", keycloak.getAuthServerUrl()));

      assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Signing in"))).isVisible();

      browser.close();
    }
  }

  @Test
  void testThatFlowIsNotSuccessFulWhenAuthenticatorConfigIsEmpty() {
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
      // comment me in if you want to run in headless mode !
      Browser browser = chromium.launch(new BrowserType.LaunchOptions().setHeadless(false));
//      Browser browser = chromium.launch();
      Page page = browser.newPage();
      page.navigate(String.format("%s/realms/required-action/account/", keycloak.getAuthServerUrl()));

      page.getByText("Signing in").click();
      page.waitForURL("**/realms/required-action/protocol/openid-connect/auth**");

      // Login Page
      page.getByLabel("Username or email").click();
      page.getByLabel("Username or email").fill("dieterbohlen");

      page.getByLabel("Password").first().fill("dietersPassword");
      page.getByLabel("Password").first().press("Enter");

    }
  }
}
