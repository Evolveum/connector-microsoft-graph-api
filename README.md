# Connector for Microsoft Graph API

This is midPoint/ConnId connector for Microsoft Graph API. It is meant to manage users in Microsoft cloud applications, such as Azure AD and Office365.

This connector is **EXPERIMENTAL**. Connector is under development and it is not yet fully supported. Users interested in using this connectors should contact Evolveum to gain information about status and availability of support for this connector.

See https://wiki.evolveum.com/display/midPoint/Microsoft+Graph+API+Connector


##Build with Maven

* download Microsoft Graph API connector source code from github
* build connector with maven: 
```
mvn clean install -Dmaven.test.skip=true
```
* find connector-msgraph-1.0-beta.jar in ```\target``` folder

##Installation

* put connector-msgraph-1.0-beta.jar to ```{midPoint_home}\icf-connectors\``` directory
* run/restart midPoint 
 
##Config

* you must import certificate to your keystore. Download current (Baltimore CyberTrust Root) certificate in .der format and after that you must import it to midPoint keystore.jceks:
```
keytool -keystore keystore.jceks -storetype jceks -storepass changeit -import -alias nlight -trustcacerts -file {your certificate}.der
```
* create your application in Azure Active Directory, for more information how to create it please see https://docs.microsoft.com/en-us/microsoft-identity-manager/microsoft-identity-manager-2016-connector-graph.
* add all DELEGATED permissions - see Permissions.
* fill all required Configuration properties in resource (clientId, clientSecret, tenantId) - see also samples.

##Permissions

This are permissions which you need to add to your Azure Active Directory application for midPoint:
 
* Directory.Read.All -> Delegated persmission
* Directory.REadWrite.All -> Delegated permission
* Group.Create -> Application permission
* Group.Read.All -> Delegated permission
* Group.Read.All -> Aplication permision
* Group.ReadWrite.All -> Delegated permission
* Group.ReadWrite.All -> Application permission
* Group.Selected ->Application permission
* GroupMember.Read.All -> Delegated permission
* GroupMember.Read.All -> Application permission
* GroupMember.ReadWrite.All -> Delegated permission
* GroupMember.ReadWrite.All -> Application permission
* PrivilegedAccess.Read.AsureADGroup -> Delegated permission
* PrivilegedAccess.Read.AsureADGroup -> Application permission
* PrivilegedAccess.ReadWrite.AsureADGroup -> Delegated permission
* PrivilegedAccess.ReadWrite.AsureADGroup -> Application permission
* User.Read -> Delegated permission
* User.Read.All -> Delegated permission
* User.Read.All -> Application permission
* User.ReadWrite.All -> Delegated permission
* User.ReadWrite.All -> Application permission
###For SharePoint you need also:
* User.Read.All -> Delegated permission
* User.ReadWrite.All -> Delegated permission

##Resource Examples
* AAD-resource.xml - sample resource.
* AAD Account.xml - sample role to creeate account in  Azure Active Directory with user location.
* AAD Metarole for Office 365 groups.xml - metarole for Office 365 groups.
* ADD Metarole for Security groups.xml - metarole for Security groups.
see [/sample folder](https://github.com/artinsolutions/connector-microsoft-graph-api/tree/master/sample)