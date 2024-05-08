package de.conciso.utils;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.utils.MediaType;

public class TokenUtil {
  public ValidatableResponse requestToken(KeycloakContainer keycloak, String realm, String username, String password) {
    return requestToken(keycloak, realm, username, password, KeycloakContainer.ADMIN_CLI_CLIENT, 200);
  }
  public ValidatableResponse requestToken(KeycloakContainer keycloak, String realm, String username, String password, String clientId) {
    return requestToken(keycloak, realm, username, password, clientId,200);
  }

  public ValidatableResponse requestToken(KeycloakContainer keycloak, String realm, String username, String password, String clientId, int expectedStatusCode) {
    String tokenEndpoint = getOpenIDConfiguration(keycloak, realm)
        .extract().path("token_endpoint");
    return RestAssured.given()
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .formParam(OAuth2Constants.USERNAME, username)
        .formParam(OAuth2Constants.PASSWORD, password)
        .formParam(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD)
        .formParam(OAuth2Constants.CLIENT_ID, clientId)
        .when().post(tokenEndpoint)
        .then()
        //.log().all()
        .statusCode(expectedStatusCode);
  }

  public ValidatableResponse getOpenIDConfiguration(KeycloakContainer keycloak, String realm) {
    return RestAssured.given().pathParam("realm-name", realm)
        .when().get(keycloak.getAuthServerUrl() + ServiceUrlConstants.DISCOVERY_URL)
        .then().statusCode(200);
  }
}