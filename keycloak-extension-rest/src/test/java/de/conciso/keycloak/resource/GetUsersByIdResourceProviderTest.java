package de.conciso.keycloak.resource;

import de.conciso.keycloak.resource.admin.GetUsersByIdResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetUsersByIdResourceProviderTest {

  private final static Logger LOG = LoggerFactory.getLogger(GetUsersByIdResourceProviderTest.class);
  private final static UUID SOME_ID = new UUID(0, 0);

  @InjectMocks
  GetUsersByIdResource cut;

  @Mock
  KeycloakSession session;

  @Mock
  UserProvider userProvider;

  @Mock
  KeycloakContext keycloakContext;

  @Mock
  RealmModel realmModel;

  @Mock
  UserModel userModel;

  void setKeycloakContextMocks() {
    given(session.users()).willReturn(userProvider);
    given(session.getContext()).willReturn(keycloakContext);
    given(keycloakContext.getRealm()).willReturn(realmModel);
  }

  void setUserMocks() {
    given(userModel.getId()).willReturn(SOME_ID.toString());
    given(userModel.getEmail()).willReturn("test@gmail.com");
    given(userModel.getUsername()).willReturn("test@gmail.com");
    given(userModel.getCreatedTimestamp()).willReturn(1L);
    given(userModel.getFirstName()).willReturn("firstName");
    given(userModel.getLastName()).willReturn("lastName");
    given(userModel.isEnabled()).willReturn(true);
    given(userModel.isEmailVerified()).willReturn(true);
    given(userModel.getFederationLink()).willReturn("FederationLink");
  }

  @Nested
  class GivenEmptyIdList {

    List<UUID> emptyList = List.of();

    @Nested
    class WhenGetAllUsersById {

      Response response;

      @BeforeEach
      void setUp() {
        response = cut.getAllUsersByListOfIds(emptyList, true);
      }

      @Test
      void thenReturnsEmptyList() {
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.readEntity(List.class)).isEmpty();
      }
    }
  }

  @Nested
  class GivenOneIdInList {

    List<UUID> listWithOneID = List.of(SOME_ID);

    @BeforeEach
    void setUp() {
      setKeycloakContextMocks();
      setUserMocks();
      given(userProvider.getUserById(realmModel, SOME_ID.toString())).willReturn(userModel);
    }

    @Nested
    class WhenGetAllUsersById {

      Response response;

      @BeforeEach
      void setUp() {
        response = cut.getAllUsersByListOfIds(listWithOneID, true);
      }

      @Test
      void thenReturnsCorrectUser() {
        assertThat(response.getStatus()).isEqualTo(200);
        var userRepresentations = (List<UserRepresentation>) response.getEntity();

        assertThat(userRepresentations)
            .extracting(UserRepresentation::getEmail)
            .containsExactly("test@gmail.com");
      }
    }
  }


  @Nested
  class GivenTwoIdsInListButOneDoesntExist {
    private final static UUID NOT_KNOWN_ID = new UUID(0, 1);
    @BeforeEach
    void setUp() {
      setKeycloakContextMocks();
    }


    @Nested
    class AndTheNonExistingIdIsFirst {

      List<UUID> listWithTwoIds = List.of(NOT_KNOWN_ID, SOME_ID);

      @BeforeEach
      void setUp() {
        given(userProvider.getUserById(realmModel, NOT_KNOWN_ID.toString())).willReturn(null);
      }

      @Nested
      class WhenGetAllUsersById {

        Response response;

        @BeforeEach
        void setUp() {
          response = cut.getAllUsersByListOfIds(listWithTwoIds, true);
        }

        @Test
        void thenReturns404() {
          assertThat(response.getStatus()).isEqualTo(404);
          assertThat((String) response.getEntity())
              .contains(NOT_KNOWN_ID.toString())
              .contains("could not be Found");

        }
      }
    }

    @Nested
    class AndTheNonExistingIdIsLast {

      List<UUID> listWithTwoIds = List.of(SOME_ID, NOT_KNOWN_ID);

      @BeforeEach
      void setUp() {
        given(userProvider.getUserById(realmModel, NOT_KNOWN_ID.toString())).willReturn(null);
        given(userProvider.getUserById(realmModel, SOME_ID.toString())).willReturn(userModel);
      }

      @Nested
      class WhenGetAllUsersById {

        Response response;

        @BeforeEach
        void setUp() {
          response = cut.getAllUsersByListOfIds(listWithTwoIds, true);
        }

        @Test
        void thenReturns404() {
          assertThat(response.getStatus()).isEqualTo(404);
          assertThat((String) response.getEntity())
              .contains(NOT_KNOWN_ID.toString())
              .contains("could not be Found");

        }
      }
    }
  }
}