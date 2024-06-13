package de.conciso.keycloak.user.mgmt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.dockerjava.api.exception.UnauthorizedException;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ListenerITest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerITest.class);
    private static final String KEYCLOAK_VERSION = "23.0.4";
    private static final String REALM_NAME = "conciso";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin";
    private static String authUrl;
    private static Keycloak keycloakAdminClient;
    private static Instant expectedTimeValue;
    private static Instant actualTimeValue;
    private static String userAccessToken;

    @Container
    private static final KeycloakContainer keycloak =
        new KeycloakContainer("quay.io/keycloak/keycloak:" +
            System.getProperty("version.keycloak", KEYCLOAK_VERSION))
            .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASS)
            .withEnv("KC_SPI_EVENTS_LISTENER_JBOSS_LOGGING_SUCCESS_LEVEL", "info")
            .withLogConsumer(new Slf4jLogConsumer(LOGGER))
            .withProviderClassesFrom("target/classes")
            .withRealmImportFile("conciso-realm.json");

    @BeforeAll
    public static void setUp() {
        authUrl = keycloak.getAuthServerUrl();
    }

    @Test
    @Order(1)
    public void testContainerStatus() {
        assertTrue(keycloak.isRunning());
    }

    @Test
    @Order(2)
    public void verifyUserHasLastSuccessfulLoginAtAttribute() {
        loginWithUserAccount();
        loginWithAdminAccount();
        extractAttributeValue();
        assertNotNull(actualTimeValue);
    }

    @Test
    @Order(3)
    public void assertLastSuccessfulLoginAtAttributeMatchesExpected() {
        extractLastSuccessfullyLoginTimeFromAccessToken();
        long diff = calculateDifferenceInSecondsBetweenInstants(actualTimeValue, expectedTimeValue);
        assertTrue(diff <= 1,
            String.format("Expected time <%s> and actual time <%s> differ by more than 1 second.", expectedTimeValue, actualTimeValue));
    }

    @Test
    @Order(4)
    public void assertLastSuccessfulLoginAtAttributeIsUpToDate() throws InterruptedException {
        Thread.sleep(3000);
        loginWithUserAccount();
        loginWithAdminAccount();
        extractAttributeValue();
        extractLastSuccessfullyLoginTimeFromAccessToken();
        long diff = calculateDifferenceInSecondsBetweenInstants(actualTimeValue, expectedTimeValue);
        assertTrue(diff <= 1,
            String.format("Expected time <%s> and actual time <%s> differ by more than 1 second.", expectedTimeValue, actualTimeValue));
    }

    public void loginWithUserAccount() {
        try {
            Keycloak keycloakUserClient = KeycloakBuilder.builder()
                .serverUrl(authUrl)
                .realm("conciso")
                .username("test")
                .password("test")
                .clientId("admin-cli")
                .build();
            userAccessToken = keycloakUserClient.tokenManager().getAccessToken().getToken();
        } catch (Exception e) {
            throw new UnauthorizedException("User Authentication Failed.");
        }
    }

    public void loginWithAdminAccount() {
        try {
            keycloakAdminClient = KeycloakBuilder.builder()
                .serverUrl(authUrl)
                .realm("master")
                .username(ADMIN_USER)
                .password(ADMIN_PASS)
                .clientId("admin-cli")
                .build();
            String accessToken = keycloakAdminClient.tokenManager().getAccessToken().getToken();
        } catch (Exception e) {
            throw new UnauthorizedException("Admin Authentication Failed.");
        }
    }

    public void extractAttributeValue() {
        UserRepresentation testUser = keycloakAdminClient.realm(REALM_NAME).users().search("test").get(0);
        assertNotNull(testUser.getAttributes());
        String attributeValue = testUser.getAttributes().get("lastSuccessfulLoginAt").get(0);
        actualTimeValue = Instant.parse(attributeValue);
    }

    public void extractLastSuccessfullyLoginTimeFromAccessToken() {
        try {
            DecodedJWT decodedJWT = JWT.decode(userAccessToken);
            expectedTimeValue = decodedJWT.getIssuedAt().toInstant();
        } catch (JWTDecodeException e) {
            throw new JWTDecodeException(e.getMessage());
        }
    }

    public long calculateDifferenceInSecondsBetweenInstants(Instant actualTime, Instant expectedTime) {
        return Math.abs(ChronoUnit.SECONDS.between(actualTime, expectedTime));
    }
}
