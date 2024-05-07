package de.conciso.keycloak.resource.admin;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.light.LightweightUserAdapter;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class GetUsersByIdResource {

  private static final Logger log = Logger.getLogger(GetUsersByIdResource.class);
  private final KeycloakSession session;
  private final AdminPermissionEvaluator auth;

  public GetUsersByIdResource(KeycloakSession session, AdminPermissionEvaluator auth) {
    Objects.requireNonNull(session);
    this.session = session;
    Objects.requireNonNull(auth);
    this.auth = auth;
  }


  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public Response getAllUsersByListOfIds(
      @QueryParam("listWithIds") List<UUID> list,
      @QueryParam("briefRepresentation") boolean briefRepresentation) {
    this.auth.users().requireQuery();

    List<UserRepresentation> userRepresentations = new ArrayList<>();
    for (UUID id : list) {
      UserModel userModel = null;
      if (LightweightUserAdapter.isLightweightUser(id.toString())) {
        UserSessionModel userSession = session.sessions()
            .getUserSession(session.getContext().getRealm(),
                LightweightUserAdapter.getLightweightUserId(id.toString()));
        if (userSession != null) {
          userModel = userSession.getUser();
        }
      } else {
        userModel = session.users()
            .getUserById(session.getContext().getRealm(), id.toString());
      }
      if (userModel == null) {
        String errorMessage = "User with id " + id + " could not be Found";
        log.error(errorMessage);
        return Response.status(Status.NOT_FOUND).entity(errorMessage).build();
      }
      auth.users().requireView(userModel);
      if (briefRepresentation) {
        userRepresentations.add(ModelToRepresentation.toBriefRepresentation(userModel));
      } else {
        userRepresentations.add(ModelToRepresentation
            .toRepresentation(session, session.getContext().getRealm(), userModel));
      }
    }
    return Response.status(Status.OK).entity(userRepresentations).build();
  }
}
