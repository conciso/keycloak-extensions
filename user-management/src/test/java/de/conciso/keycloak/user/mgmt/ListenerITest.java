package de.conciso.keycloak.user.mgmt;

import com.github.dockerjava.api.exception.UnauthorizedException;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class ListenerITest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerITest.class);
    private static final String KEYCLOAK_VERSION = "23.0.4";
    private static final String REALM_NAME = "conciso";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin";

    @Container
    private static final KeycloakContainer keycloak =
        new KeycloakContainer("quay.io/keycloak/keycloak:" +
            System.getProperty("version.keycloak", KEYCLOAK_VERSION))
            .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASS)
            .withLogConsumer(new Slf4jLogConsumer(LOGGER))
            .withProviderClassesFrom("target/classes")
            .withRealmImportFile("conciso-realm.json");

    @Test
    public void assertLastSuccessfulLoginAtAttributeMatchesExpected() {
        String authUrl = keycloak.getAuthServerUrl();
        String userAccessToken = loginWithUserAccount(authUrl);
        Instant expectedTimeValue = extractLastSuccessfullyLoginTimeFromAccessToken(userAccessToken);
        Keycloak keycloakAdminClient = loginWithAdminAccount(authUrl);
        Instant actualTimeValue = extractAttributeValue(keycloakAdminClient);
        long diff = calculateDifferenceInSecondsBetweenInstants(actualTimeValue, expectedTimeValue);
        assertTrue(diff <= 1,
            String.format("Expected time <%s> and actual time <%s> differ by more than 1 second.", expectedTimeValue, actualTimeValue));
    }

    public String loginWithUserAccount(String url) {
        try {
            Keycloak keycloakUserClient = KeycloakBuilder.builder()
                .serverUrl(url)
                .realm("conciso")
                .username("test")
                .password("test")
                .clientId("admin-cli")
                .build();
            return keycloakUserClient.tokenManager().getAccessToken().getToken();
        } catch (Exception e) {
            throw new UnauthorizedException("User Authentication Failed.");
        }
    }

    public Keycloak loginWithAdminAccount(String url) {
        try {
            Keycloak adminClient = KeycloakBuilder.builder()
                .serverUrl(url)
                .realm("master")
                .username(ADMIN_USER)
                .password(ADMIN_PASS)
                .clientId("admin-cli")
                .build();
            String accessToken = adminClient.tokenManager().getAccessToken().getToken();
            return adminClient;
        } catch (Exception e) {
            throw new UnauthorizedException("Admin Authentication Failed.");
        }
    }

    public Instant extractAttributeValue(Keycloak keycloakAdminClient) {
        UserRepresentation testUser = keycloakAdminClient.realm(REALM_NAME).users().search("test").get(0);
        assertNotNull(testUser.getAttributes());
        String attributeValue = testUser.getAttributes().get("lastSuccessfulLoginAt").get(0);
        return Instant.parse(attributeValue);
    }

    public Instant extractLastSuccessfullyLoginTimeFromAccessToken(String accessToken) {
        try {
            String[] jwtParts = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]));
            JsonWebToken payloadJson = JsonSerialization.readValue(payload, JsonWebToken.class);
            return Instant.ofEpochSecond(payloadJson.getIat());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long calculateDifferenceInSecondsBetweenInstants(Instant actualTime, Instant expectedTime) {
        return Math.abs(ChronoUnit.SECONDS.between(actualTime, expectedTime));
    }
}
