# Authenticator-Required-Action
Imagine a user authenticates via an Identity Provider (IdP) and requires a specific action, such as resetting their password or agreeing to new terms of service.
This extension ensures that the configured required action is automatically set for the user once they authenticate through the Identity Provider.
```mermaid
flowchart LR
    CompanyA(RealmCompanyA)-->UsersA
    CompanyB(RealmCompanyB)-->UsersB
    UsersA-- Connected via Idp alias: CompanyA ---RealmConciso
    UsersB-- Connected via Idp alias: CompanyB ---RealmConciso
```

The First Broker login of Idp CompanyA is configured to set the 'TERMS_AND_CONDITIONS' for all users from CompanyA. Users from CompanyB dont have to accept the terms, so they dont get that action.

## How to use
![required-action-flow.png](../docs/pics/required-action-flow.png)

![required-action-authenticator-config.png](../docs/pics/required-action-authenticator-config.png)

To see your available Required-Actions, go to Realm 'master' -> Provider info -> Search for 'req' or scroll down until you see 'required-action' in the column for SPI
![required-action-available-required-actions.png](../docs/pics/required-action-available-required-actions.png)
