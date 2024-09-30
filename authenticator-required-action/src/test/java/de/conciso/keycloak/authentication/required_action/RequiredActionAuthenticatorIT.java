package de.conciso.keycloak.authentication.required_action;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RequiredActionAuthenticatorIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(RequiredActionAuthenticatorIT.class);
  private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "latest");
  private static final String REALM_NAME = "required-action";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "admin";

  @Container
  private static final KeycloakContainer keycloak =
    new KeycloakContainer("quay.io/keycloak/keycloak:" + KEYCLOAK_VERSION)
      .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
      .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASS)
      .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
      .withProviderClassesFrom("target/classes")
      .withRealmImportFile("required-action-realm.json");

  @Test
  void testSomething() {
    assertThat(1).isEqualTo(1);
  }


}
