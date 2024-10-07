# Authenticator-Required-Action

```mermaid
flowchart LR
    CompanyA(RealmCompanyA)-->UsersA
    CompanyB(RealmCompanyB)-->UsersB
    UsersA-- Connected via Idp alias: CompanyA ---RealmConciso
    UsersB-- Connected via Idp alias: CompanyB ---RealmConciso
```

The First Broker login of Idp CompanyA is configured to set the 'TERMS_AND_CONDITIONS' for all users from CompanyA. Users from CompanyB dont have to accept the terms, so they dont get that action.
![required-action-flow.png](../docs/pics/required-action-flow.png)

![required-action-authenticator-config.png](../docs/pics/required-action-authenticator-config.png)

To see your availabaddle Required-Actions, go to Realm 'master' -> Provider infor -> Search for 'req' or scroll down until you see 'required-action' in the column for SPI
![required-action-available-required-actions.png](../docs/pics/required-action-available-required-actions.png)
