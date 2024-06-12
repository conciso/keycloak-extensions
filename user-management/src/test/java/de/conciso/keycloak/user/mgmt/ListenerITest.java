package de.conciso.keycloak.user.mgmt;

import com.github.dockerjava.api.exception.UnauthorizedException;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static String attributeValue;
    private static Instant expectedTimeValue;
    private static Instant actualTimeValue;
    private static final List<String> loginTimes = new ArrayList<>();
    private static final Pattern LOG_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) INFO  .*type=LOGIN.*username=test");
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    @Container
    private static final KeycloakContainer keycloak =
        new KeycloakContainer("quay.io/keycloak/keycloak:" +
            System.getProperty("version.keycloak", KEYCLOAK_VERSION))
            .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASS)
            .withEnv("KC_SPI_EVENTS_LISTENER_JBOSS_LOGGING_SUCCESS_LEVEL", "info")
            .withLogConsumer(new Slf4jLogConsumer(LOGGER) {
                @Override
                public void accept(OutputFrame outputFrame) {
                    String logLine = outputFrame.getUtf8String();
                    Matcher matcher = LOG_PATTERN.matcher(logLine);
                    if (matcher.find()) {
                        loginTimes.add(matcher.group(1));
                    }
                    super.accept(outputFrame);
                }
            })
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
    public void assertLastSuccessfulLoginAtMatchesExpected() {
        extractLastLoginTimeFromLogs();
        assertTrue(calculateDifferenceInSecondsBetweenInstants(actualTimeValue, expectedTimeValue) <= 1,
            String.format("Expected time <%s> and actual time <%s> differ by more than 1 second.", expectedTimeValue, actualTimeValue));
    }

    @Test
    @Order(4)
    public void assertLastSuccessfulLoginAtAttributeIsUpToDate() throws InterruptedException {
        Thread.sleep(3000);
        loginWithUserAccount();
        loginWithAdminAccount();
        extractAttributeValue();
        extractLastLoginTimeFromLogs();
        assertTrue(calculateDifferenceInSecondsBetweenInstants(actualTimeValue, expectedTimeValue) <= 1,
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
            String accessToken = keycloakUserClient.tokenManager().getAccessToken().getToken();
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
        attributeValue = testUser.getAttributes().get("lastSuccessfulLoginAt").get(0);
        actualTimeValue = Instant.parse(attributeValue);
    }

    public void extractLastLoginTimeFromLogs() {
        Assertions.assertFalse(loginTimes.isEmpty(), "No LOGIN logs found for user 'test'");
        String lastLoginTimeStr = loginTimes.get(loginTimes.size() - 1);
        expectedTimeValue = LocalDateTime.parse(lastLoginTimeStr, LOG_TIME_FORMATTER).toInstant(ZoneOffset.UTC);
    }

    public long calculateDifferenceInSecondsBetweenInstants(Instant actualTime, Instant expectedTime) {
        return Math.abs(ChronoUnit.SECONDS.between(actualTime, expectedTime));
    }
}
