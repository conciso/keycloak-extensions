# Keycloak-Extensions

This Repository contains free to use Extensions for the OpenSource Project [Keycloak](https://github.com/keycloak/keycloak)

This repo contains the following extensions:

## Authenticator-Required-Action

You can add this Authenticator to your flow, so a User gets the defined Required-Action set on signing in.
[README.md - Authenticator-Required Action](./authenticator-required-action/README.md)

## Rest-Endpoints

* GetUsersByIdResource -> <root_url>/admins/realms/<realm_name>/users-by-id
  * QueryParams:
    * briefRepresentation true | false
    * listWithIds List containing Ids of Users
  * Returns List of Users
