# Keycloak-Extensions

This Repository contains free to use Extensions for the OpenSource Project [Keycloak](https://github.com/keycloak/keycloak)

Contained Extensions:

* Rest-Endpoints
  * GetUsersByIdResource -> <root_url>/admins/realms/<realm_name>/users-by-id
    * QueryParams:
      * briefRepresentation true | false
      * listWithIds List containing Ids of Users
    * Returns List of Users
