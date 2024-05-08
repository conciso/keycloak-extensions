package de.conciso.keycloak.user.mgmt;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.models.light.LightweightUserAdapter;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class UserLastLoginEventListener implements EventListenerProvider {

    private static final String LAST_SUCCESSFUL_LOGIN_ATTR_KEY = "lastSuccessfulLoginAt";
    private final KeycloakSession keycloakSession;
    private final Clock clock;

    UserLastLoginEventListener(KeycloakSession keycloakSession) {
        this(keycloakSession, Clock.systemUTC());
    }

    UserLastLoginEventListener(KeycloakSession keycloakSession, Clock clock) {
        this.keycloakSession = keycloakSession;
        this.clock = clock;
    }

    @Override
    public void onEvent(Event event) {
        final String realmId = event.getRealmId();
        final RealmModel realm = keycloakSession.realms().getRealm(realmId);

        final String time = OffsetDateTime.now(clock).format(DateTimeFormatter.ISO_DATE_TIME);
        switch (event.getType()) {
            case LOGIN -> handleUserLogin(event, realm, time);
            case CLIENT_LOGIN -> handleClientLoginWithServiceAccount(event, realm, time);
        }
    }

    private void handleClientLoginWithServiceAccount(Event event, RealmModel realm, String time) {
        final String clientId = event.getClientId();
        final ClientModel client = keycloakSession.clients().getClientByClientId(realm, clientId);
        if (client.isServiceAccountsEnabled()) {
            UserModel serviceAccount = keycloakSession.users().getServiceAccount(client);
            serviceAccount.setSingleAttribute(LAST_SUCCESSFUL_LOGIN_ATTR_KEY, time);
        }
    }

    private void handleUserLogin(Event event, RealmModel realm, String time) {
        final String userId = event.getUserId();

        UserModel user = null;
        if (LightweightUserAdapter.isLightweightUser(userId)) {
            UserSessionModel userSession = keycloakSession.sessions().getUserSession(realm, LightweightUserAdapter.getLightweightUserId(userId));
            if (userSession != null) {
                user = userSession.getUser();
            }
        } else {
            user = keycloakSession.users().getUserById(realm, userId);
        }

        if (user != null) {
            user.setSingleAttribute(LAST_SUCCESSFUL_LOGIN_ATTR_KEY, time);
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {

    }

}
