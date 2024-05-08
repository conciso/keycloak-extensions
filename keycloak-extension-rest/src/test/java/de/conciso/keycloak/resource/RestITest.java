package de.conciso.keycloak.resource;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.conciso.utils.TokenUtil;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.matcher.ResponseAwareMatcher;
import jakarta.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.not;


@Testcontainers
class RestITest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestITest.class);
  private static final String LATEST = "23.0.4";
  private static final String REALM_NAME = "conciso";
  private static final String URL_PATH = "users-by-id";
  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASS = "admin";
  private final TokenUtil tokenUtil = new TokenUtil();
  private String urlEndpoint;

  @Container
  private static final KeycloakContainer keycloak =
      new KeycloakContainer("quay.io/keycloak/keycloak:" +
          System.getProperty("version.keycloak", LATEST))
          .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
          .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASS)
          .withLogConsumer(new Slf4jLogConsumer(LOGGER).withSeparateOutputStreams())
          .withProviderClassesFrom("target/classes")
          .withRealmImportFile("conciso-realm.json");


  @BeforeEach
  void setUp() {
    StringBuilder builder = new StringBuilder();
    builder.append(keycloak.getAuthServerUrl()).append("/");
    builder.append("admin").append("/");
    builder.append("realms").append("/");
    builder.append(REALM_NAME).append("/");
    builder.append(URL_PATH);
    urlEndpoint = builder.toString();
  }

  @Test
  void testThatBriefRepresentationFlagIsRespected() {
    RestAssured.given()
        .baseUri(urlEndpoint)
        .accept(MediaType.APPLICATION_JSON)
        .queryParams(Map.of(
            "briefRepresentation", true,
            "listWithIds", List.of("42ed876b-d758-4e69-9f39-9874e47a4d59")))
        .header("Authorization", "Bearer " + getAccessToken())
        .log().all()
        .get()
        .then()
        .log().all()
        .statusCode(200)
        .body("[0]", hasKey("id"))
        .body("[0]", not(hasKey("attributes")));

    RestAssured.given()
        .baseUri(urlEndpoint)
        .accept(MediaType.APPLICATION_JSON)
        .queryParams(Map.of(
            "briefRepresentation", false,
            "listWithIds", List.of("42ed876b-d758-4e69-9f39-9874e47a4d59")))
        .header("Authorization", "Bearer " + getAccessToken())
        .log().all()
        .get()
        .then()
        .log().all()
        .statusCode(200)
        .body("[0]", hasKey("id"))
        .body("[0].attributes", hasKey("someKey"))
        .body("[0].attributes.someKey[0]", equalTo("someValue"));
  }

  private static Stream<Arguments> expectedUserCompositions() {
    return Stream.of(
        Arguments.of(200, Map.of("briefRepresentation", true), List.of()),
        Arguments.of(200,
            Map.of(
                "briefRepresentation", true,
                "listWithIds", List.of("42ed876b-d758-4e69-9f39-9874e47a4d59")),
            List.of("dieterbohlen")),
        Arguments.of(200,
            Map.of(
                "briefRepresentation", true,
                "listWithIds", List.of("42ed876b-d758-4e69-9f39-9874e47a4d59", "f558ca89-e4c7-4964-84d0-197879944ad7")),
            List.of("dieterbohlen", "mannimammut")),
        Arguments.of(200,
            Map.of(
                "briefRepresentation", true,
                "listWithIds", List.of(
                    "42ed876b-d758-4e69-9f39-9874e47a4d59",
                    "f558ca89-e4c7-4964-84d0-197879944ad7",
                    "bc6e9875-ccc4-4938-9db0-6572bc3312a7")),
            List.of("dieterbohlen", "mannimammut", "peterpommes")),
        Arguments.of(404,
            Map.of(
                "briefRepresentation", true,
                "listWithIds",
                List.of("00000000-0000-0000-0000-000000000000")),
            List.of()),
        Arguments.of(404,
            Map.of(
                "briefRepresentation", true,
                "listWithIds",
                List.of("42ed876b-d758-4e69-9f39-9874e47a4d59", "00000000-0000-0000-0000-000000000000")),
            List.of())
    );
  }

  @ParameterizedTest
  @MethodSource("expectedUserCompositions")
  void testCombinationOfRequests(int statusCode, Map<String, ?> queryParams, List<String> expectedUserNames) {
    assertThat(
        makeCall(
            statusCode,
            queryParams,
            getAccessToken()))
        .map(UserRepresentation::getUsername)
        .containsAll(expectedUserNames);

  }


  private List<UserRepresentation> makeCall(int expectedStatusCode, Map<String, ?> queryParams, String accessToken) {
    var response = RestAssured.given()
        .baseUri(urlEndpoint)
        .accept(MediaType.APPLICATION_JSON)
        .queryParams(queryParams)
        .header("Authorization", "Bearer " + accessToken)
        .log().all()
        .get();
    response.then().log().all().statusCode(expectedStatusCode);

    return response.statusCode() == 200
        ? Arrays.asList(response.getBody().as(UserRepresentation[].class))
        : Collections.emptyList();
  }

  private List<UserRepresentation> makeCall(int expectedStatusCode, Map<String, ?> queryParams) {
    return makeCall(expectedStatusCode, queryParams, "");
  }

  @Test
  void testThatRequestReturns401WithoutValidToken() {
    assertThat(makeCall(401, Map.of())).isEmpty();
  }

  @Test
  void testNonAdminUserIsUnableToUseApi() {
    var token = getAccessToken("dieterbohlen", "dietersPassword", "conciso", 200);
    assertThat(makeCall(403, Map.of(), token)).isEmpty();
  }

  private String getAccessToken(String username, String password, String realm, int statusCode) {
    return tokenUtil.requestToken(keycloak, realm, username, password)
        .contentType(ContentType.JSON)
        .statusCode(statusCode)
        .extract().response()
        .path("access_token");
  }

  private String getAccessToken() {
    return getAccessToken(ADMIN_USER, ADMIN_PASS, "master", 200);
  }

}
