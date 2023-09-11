# Connector for Microsoft Graph API

This is midPoint/ConnId connector for Microsoft Graph API. It is meant to manage users in Microsoft cloud applications, such as Azure AD and Office365.

See https://wiki.evolveum.com/display/midPoint/Microsoft+Graph+API+Connector


## Build with Maven

* download Microsoft Graph API connector source code from github
* build connector with maven: 
```
mvn clean install -Dmaven.test.skip=true
```
* find connector-msgraph-{version}.jar in ```\target``` folder

## Installation

* put connector-msgraph-{version}.jar to ```{midPoint_home}\icf-connectors\``` directory
* run/restart midPoint 
 
## Config

* Import of SSL certificates is needed. Download current DigiCert Global Root G2 and DigiCert Global Root CA certificate in .der format and after that you must import it to midPoint keystore.jceks:
```
keytool -keystore keystore.jceks -storetype jceks -storepass changeit -import -alias nlight -trustcacerts -file {your certificate}.der
```
* create your application in Azure Active Directory, for more information how to create it please see https://docs.microsoft.com/en-us/microsoft-identity-manager/microsoft-identity-manager-2016-connector-graph.
* add all DELEGATED permissions - see Permissions.
* fill all required Configuration properties in resource (clientId, clientSecret, tenantId) - see also samples.

## Permissions

This are permissions which you need to add to your Azure Active Directory application for midPoint:
### Application Permission
#### Required
* Directory.Read.All
* Directory.ReadWrite.All
* Group.Create
* Group.Read.All
* Group.ReadWrite.All
* Group.Selected
* GroupMember.Read.All
* GroupMember.ReadWrite.All
* PrivilegedAccess.Read.AzureADGroup
* PrivilegedAccess.ReadWrite.AzureADGroup
* User.Read.All
* User.ReadWrite.All
#### Optional: Role Membership Management
* EntitlementManagement.Read.All 
* EntitlementManagement.ReadWrite.All 
* RoleManagement.Read.Directory 
* RoleManagement.ReadWrite.Directory

## Resource Examples
see [midpoint-samples - Microsoft Graph Connector](https://github.com/Evolveum/midpoint-samples/tree/master/samples/resources/msgraph)