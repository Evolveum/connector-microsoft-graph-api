package com.evolveum.polygon.connector.msgraphapi;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UserProcessing extends ObjectProcessing {

    private final static String API_ENDPOINT = "graph.microsoft.com/v1.0";
    private final static String USERS = "/users";
    private static final String MESSAGES = "messages";

    private static final String SPACE = "%20";
    private static final String QUOTATION = "%22";
    private static final String EQUALS = "eq";
    private static final String DOLLAR = "%24";
    private static final String QUESTIONMARK = "%3F";
    private static final String FILTER = "filter";
    private static final String EQUAL = "%3D";
    private static final String SLASH = "%2F";

    //required
    private static final String ATTR_ACCOUNTENABLED = "accountEnabled";
    private static final String ATTR_DISPLAYNAME = "displayName";
    private static final String ATTR_ONPREMISESIMMUTABLEID = "onPremisesImmutableId";
    private static final String ATTR_MAILNICKNAME = "mailNickname";
    //passwordprofile
    private static final String ATTR_PASSWORDPROFILE = "passwordProfile";
    private static final String ATTR_FORCECHANGEPASSWORDNEXTSIGNIN = "forceChangePasswordNextSignIn";
    private static final String ATTR_PASSWORD = "password";

    private static final String ATTR_USERPRINCIPALNAME = "userPrincipalName";


    //optional
    private static final String ATTR_ABOUTME = "aboutMe";

    //ASSIGNEDLICENSES
    private static final String ATTR_ASSIGNEDLICENSES = "assignedLicenses";
    private static final String ATTR_SKUID = "skuId";
    private static final String ATTR_DISABLEDPLANS = "disabledPlans";

    //ASSIGNEDPLAN
    private static final String ATTR_ASSIGNEDPLANS = "assignedPlans";
    private static final String ATTR_ASSIGNEDDATETIME = "assignedDateTime";
    private static final String ATTR_CAPABILITYSTATUS = "capabilityStatus";
    private static final String ATTR_SERVICE = "service";
    private static final String ATTR_SERVICEPLANID = "servicePlanId";

    private static final String ATTR_BIRTHDAY = "birthday";
    private static final String ATTR_BUSINESSPHONES = "businessPhones";
    private static final String ATTR_CITY = "city";
    private static final String ATTR_COMPANYNAME = "companyName";
    private static final String ATTR_COUNTRY = "country";
    private static final String ATTR_DEPARTMENT = "department";
    private static final String ATTR_GIVENNAME = "givenName";
    private static final String ATTR_HIREDATE = "hireDate";
    private static final String ATTR_ID = "id";
    private static final String ATTR_IMADDRESSES = "imAddresses";
    private static final String ATTR_INTERESTS = "interests";
    private static final String ATTR_JOBTITLE = "jobTitle";
    private static final String ATTR_MAIL = "mail";

    //MAILBOXSETTINGS
    private static final String ATTR_MAILBOXSETTINGS = "mailboxSettings";
    private static final String ATTR_AUTOMATICREPLIESSETTING = "automaticRepliesSetting";
    private static final String ATTR_EXTERNALAUDIENCE = "externalAudience";
    private static final String ATTR_EXTERNALREPLYMESSAGE = "externalReplyMessage";
    private static final String ATTR_INTERNALREPLYMESSAGE = "internalReplyMessage";
    private static final String ATTR_SCHEDULEDENDDATETIME = "scheduledEndDateTime";
    private static final String ATTR_SCHEDULEDSTARTDATETIME = "scheduledStartDateTime";
    private static final String ATTR_DATETIME = "DateTime";
    private static final String ATTR_TIMEZONE = "TimeZone";
    private static final String ATTR_STATUS = "status";
    private static final String ATTR_LANGUAGE = "language";
    private static final String ATTR_LOCALE = "locale";


    private static final String ATTR_MOBILEPHONE = "mobilePhone";
    private static final String ATTR_MYSITE = "mySite";
    private static final String ATTR_OFFICELOCATION = "officeLocation";
    private static final String ATTR_ONPREMISESLASTSYNCDATETIME = "onPremisesLastSyncDateTime";
    private static final String ATTR_ONPREMISESSECURITYIDENTIFIER = "onPremisesSecurityIdentifier";
    private static final String ATTR_ONPREMISESSYNCENABLED = "onPremisesSyncEnabled";
    private static final String ATTR_PASSWORDPOLICIES = "passwordPolicies";
    private static final String ATTR_PASTPROJECTS = "pastProjects";
    private static final String ATTR_POSTALCODE = "postalCode";
    private static final String ATTR_PREFERREDLANGUAGE = "preferredLanguage";
    private static final String ATTR_PREFERREDNAME = "preferredName";

    //provisionplans
    private static final String ATTR_PROVISIONEDPLANS = "provisionedPlans";
    private static final String ATTR_PROVISIONINGSTATUS = "provisioningStatus";

    private static final String ATTR_PROXYADDRESSES = "proxyAddresses";
    private static final String ATTR_RESPONSIBILITIES = "responsibilities";
    private static final String ATTR_SCHOOLS = "schools";
    private static final String ATTR_SKILLS = "skills";
    private static final String ATTR_STATE = "state";
    private static final String ATTR_STREETADDRESS = "streetAddress";
    private static final String ATTR_SURNAME = "surname";
    private static final String ATTR_USAGELOCATION = "usageLocation";
    private static final String ATTR_USERTYPE = "userType";

    public UserProcessing(MSGraphConfiguration configuration, MSGraphConnector connector) {
        super(configuration, connector);
    }


    public void buildUserObjectClass(SchemaBuilder schemaBuilder) {
        ObjectClassInfoBuilder userObjClassBuilder = new ObjectClassInfoBuilder();
        userObjClassBuilder.setType(ObjectClass.ACCOUNT_NAME);

        //required

        //Supports $filter
        AttributeInfoBuilder attrAccountEnabled = new AttributeInfoBuilder(ATTR_ACCOUNTENABLED);
        attrAccountEnabled.setRequired(true).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrAccountEnabled.build());

        //Supports $filter and $orderby.
        AttributeInfoBuilder attrDisplayName = new AttributeInfoBuilder(ATTR_DISPLAYNAME);
        attrDisplayName.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrDisplayName.build());

        //Supports $filter
        AttributeInfoBuilder attrMailNickname = new AttributeInfoBuilder(ATTR_MAILNICKNAME);
        attrMailNickname.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMailNickname.build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_PASSWORDPROFILE + "." + ATTR_FORCECHANGEPASSWORDNEXTSIGNIN)
                .setRequired(true).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true).build());

        userObjClassBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);


//        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
//                ATTR_PASSWORDPROFILE + "." + ATTR_PASSWORD)
//                .setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).build());

        // Supports $filter and $orderby.
        AttributeInfoBuilder attrUserPrincipalName = new AttributeInfoBuilder(ATTR_USERPRINCIPALNAME);
        attrUserPrincipalName.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrUserPrincipalName.build());

        //optional

        //Supports $filter.
        AttributeInfoBuilder attrOnPremisesImmutableId = new AttributeInfoBuilder(ATTR_ONPREMISESIMMUTABLEID);
        attrOnPremisesImmutableId.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesImmutableId.build());

        AttributeInfoBuilder attrAboutMe = new AttributeInfoBuilder(ATTR_ABOUTME);
        attrAboutMe.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrAboutMe.build());


        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_ASSIGNEDLICENSES + "." + ATTR_SKUID)
                .setRequired(false)
                //.setType(GUID.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).build());

        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_ASSIGNEDLICENSES + "." + ATTR_DISABLEDPLANS)
                .setRequired(false)
                //.setType(GUID.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).setMultiValued(true).build());


        //not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_ASSIGNEDPLANS + "." + ATTR_ASSIGNEDDATETIME)
                .setRequired(false)
                //.setType(OffsetDateTime.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).build());

        //not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_ASSIGNEDPLANS + "." + ATTR_CAPABILITYSTATUS)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());

        //not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_ASSIGNEDPLANS + "." + ATTR_SERVICE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());

        //not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_ASSIGNEDPLANS + "." + ATTR_SERVICEPLANID)
                .setRequired(false)
                //.setType(GUID.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).build());

        //update
        AttributeInfoBuilder attrBirthday = new AttributeInfoBuilder(ATTR_BIRTHDAY);
        attrBirthday.setRequired(false)
                .setType(String.class)
                .setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrBirthday.build());

        //multivalued but only one number can be set for this property
        AttributeInfoBuilder attrBusinessPhones = new AttributeInfoBuilder(ATTR_BUSINESSPHONES);
        attrBusinessPhones
                .setMultiValued(true)
                .setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrBusinessPhones.build());

        //supports $filter
        AttributeInfoBuilder attrCity = new AttributeInfoBuilder(ATTR_CITY);
        attrCity.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrCity.build());

        //readonly
        AttributeInfoBuilder attrCompanyName = new AttributeInfoBuilder(ATTR_COMPANYNAME);
        attrCompanyName.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrCompanyName.build());

        //supports $filter
        AttributeInfoBuilder attrCountry = new AttributeInfoBuilder(ATTR_COUNTRY);
        attrCountry.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrCountry.build());

        //supports $filter
        AttributeInfoBuilder attrDepartment = new AttributeInfoBuilder(ATTR_DEPARTMENT);
        attrDepartment.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrDepartment.build());

        //supports $filter
        AttributeInfoBuilder attrGivenName = new AttributeInfoBuilder(ATTR_GIVENNAME);
        attrGivenName.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrGivenName.build());

        AttributeInfoBuilder attrHireDate = new AttributeInfoBuilder(ATTR_HIREDATE);
        attrHireDate.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrHireDate.build());

        //Read-only, not nullable
        AttributeInfoBuilder attrImAddresses = new AttributeInfoBuilder(ATTR_IMADDRESSES);
        attrImAddresses.setMultiValued(true).setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrImAddresses.build());

        //Read-only,
        AttributeInfoBuilder attrId = new AttributeInfoBuilder(ATTR_ID);
        attrId.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrId.build());

        //multivalued
        AttributeInfoBuilder attrInterests = new AttributeInfoBuilder(ATTR_INTERESTS);
        attrInterests.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrInterests.build());


        //supports $filter
        AttributeInfoBuilder attrJobTitle = new AttributeInfoBuilder(ATTR_JOBTITLE);
        attrJobTitle.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrJobTitle.build());

        //Read-Only, Supports $filter
        AttributeInfoBuilder attrMail = new AttributeInfoBuilder(ATTR_MAIL);
        attrMail.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMail.build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_EXTERNALAUDIENCE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_EXTERNALREPLYMESSAGE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_INTERNALREPLYMESSAGE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_SCHEDULEDENDDATETIME + "." + ATTR_DATETIME)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_SCHEDULEDENDDATETIME + "." + ATTR_TIMEZONE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_SCHEDULEDSTARTDATETIME + "." + ATTR_DATETIME)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_SCHEDULEDSTARTDATETIME + "." + ATTR_TIMEZONE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_AUTOMATICREPLIESSETTING + "." + ATTR_STATUS)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_LANGUAGE + "." + ATTR_LOCALE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_LANGUAGE + "." + ATTR_DISPLAYNAME)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());

        //get or update
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_MAILBOXSETTINGS + "." + ATTR_TIMEZONE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).build());


        AttributeInfoBuilder attrMobilePhone = new AttributeInfoBuilder(ATTR_MOBILEPHONE);
        attrMobilePhone.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMobilePhone.build());

        AttributeInfoBuilder attrMySite = new AttributeInfoBuilder(ATTR_MYSITE);
        attrMySite.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMySite.build());

        AttributeInfoBuilder attrOfficeLocation = new AttributeInfoBuilder(ATTR_OFFICELOCATION);
        attrOfficeLocation.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOfficeLocation.build());

        //Read-only
        AttributeInfoBuilder attrOnPremisesLastSyncDateTime = new AttributeInfoBuilder(ATTR_ONPREMISESLASTSYNCDATETIME);
        attrOnPremisesLastSyncDateTime.setRequired(false).setType(String.class)
                //.setType(OffsetDateTime.class)
                .setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesLastSyncDateTime.build());

        //read-only
        AttributeInfoBuilder attrOnPremisesSecurityIdentifier = new AttributeInfoBuilder(ATTR_ONPREMISESSECURITYIDENTIFIER);
        attrOnPremisesSecurityIdentifier.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesSecurityIdentifier.build());

        //read-only
        AttributeInfoBuilder attrOnPremisesSyncEnabled = new AttributeInfoBuilder(ATTR_ONPREMISESSYNCENABLED);
        attrOnPremisesSyncEnabled.setRequired(false).setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesSyncEnabled.build());

        AttributeInfoBuilder attrPasswordPolicies = new AttributeInfoBuilder(ATTR_PASSWORDPOLICIES);
        attrPasswordPolicies.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPasswordPolicies.build());

        //multivalued
        AttributeInfoBuilder attrPastProjects = new AttributeInfoBuilder(ATTR_PASTPROJECTS);
        attrPastProjects.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPastProjects.build());

        AttributeInfoBuilder attrPostalCode = new AttributeInfoBuilder(ATTR_POSTALCODE);
        attrPostalCode.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPostalCode.build());

        AttributeInfoBuilder attrPreferredLanguage = new AttributeInfoBuilder(ATTR_PREFERREDLANGUAGE);
        attrPreferredLanguage.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPreferredLanguage.build());

        AttributeInfoBuilder attrPreferredName = new AttributeInfoBuilder(ATTR_PREFERREDNAME);
        attrPreferredName.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPreferredName.build());


        //multivalued, read-only, not nullable
        AttributeInfoBuilder attrProxyAddresses = new AttributeInfoBuilder(ATTR_PROXYADDRESSES);
        attrProxyAddresses.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrProxyAddresses.build());

        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_PROVISIONEDPLANS + "." + ATTR_CAPABILITYSTATUS)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());

        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_PROVISIONEDPLANS + "." + ATTR_PROVISIONINGSTATUS)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());

        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_PROVISIONEDPLANS + "." + ATTR_SERVICE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());


        //multivalued
        AttributeInfoBuilder attrResponsibilities = new AttributeInfoBuilder(ATTR_RESPONSIBILITIES);
        attrResponsibilities.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrResponsibilities.build());

        //multivalued
        AttributeInfoBuilder attrSchools = new AttributeInfoBuilder(ATTR_SCHOOLS);
        attrSchools.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrSchools.build());

        //multivalued
        AttributeInfoBuilder attrSkills = new AttributeInfoBuilder(ATTR_SKILLS);
        attrSkills.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrSkills.build());

        //supports $filter
        AttributeInfoBuilder attrState = new AttributeInfoBuilder(ATTR_STATE);
        attrState.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrState.build());

        AttributeInfoBuilder attrStreetAddress = new AttributeInfoBuilder(ATTR_STREETADDRESS);
        attrStreetAddress.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrStreetAddress.build());

        //Supports $filter
        AttributeInfoBuilder attrSurname = new AttributeInfoBuilder(ATTR_SURNAME);
        attrSurname.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrSurname.build());

        //Not nullable. Supports $filter
        AttributeInfoBuilder attrUsageLocation = new AttributeInfoBuilder(ATTR_USAGELOCATION);
        attrUsageLocation.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrUsageLocation.build());

        //Supports $filter
        AttributeInfoBuilder attrUserType = new AttributeInfoBuilder(ATTR_USERTYPE);
        attrUserType.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrUserType.build());


        schemaBuilder.defineObjectClass(userObjClassBuilder.build());

    }

    public void updateUser(Uid uid, Set<Attribute> attributes) {
        URIBuilder uriBuilder = getURIBuilder();
        HttpEntityEnclosingRequestBase request = null;
        URI uri = null;
        uriBuilder.setPath(USERS + "/" + uid.getUidValue());
        uri = getUri(uriBuilder);
        LOG.info("update user, PATCH");
        LOG.info("Path: {0}", uri);
        request = new HttpPatch(uri);
        List<Object> jsonObjectaccount = buildLayeredAtrribute(attributes);
        callRequestNoContentNoJson(request, jsonObjectaccount);

    }

    public Uid createUser(Uid uid, Set<Attribute> attributes) {
        LOG.info("Start createUser, Uid: {0}, attributes: {1}", uid, attributes);

        if (attributes == null || attributes.isEmpty()) {
            throw new InvalidAttributeValueException("attributes not provided or empty");
        }

        Boolean create = uid == null;
        if (create) {

            int mandatoryAttributes = 0;
            for (Attribute attribute : attributes) {
                if ((attribute.getName().equals("accountEnabled") ||
                        attribute.getName().equals("displayName") ||
                        attribute.getName().equals("userPrincipalName") ||
                        attribute.getName().equals("passwordProfile.forceChangePasswordNextSignIn") ||
                        attribute.getName().equals("__PASSWORD__")) &&
                        !attribute.getValue().isEmpty()) {
                    mandatoryAttributes++;
                }
            }
            if (mandatoryAttributes < 5) {
                throw new InvalidAttributeValueException();
            }

            URIBuilder uriBuilder = getURIBuilder();


            HttpEntityEnclosingRequestBase request = null;
            URI uri = null;


            uriBuilder.setPath(USERS);
            uri = getUri(uriBuilder);
            LOG.info("Uid == null -> create user");
            LOG.info("Path: {0}", uri);
            request = new HttpPost(uri);

            JSONObject jsonObject1 = buildLayeredAttributeJSON(attributes);
            JSONObject jsonRequest = callRequest(request, jsonObject1, true);
            if (jsonRequest == null) {
                LOG.info("Returning original Uid- {0} ", uid);
                return uid;
            }
            String newUid = jsonRequest.getString("id");
            LOG.info("The new Uid is {0} ", newUid);


            return new Uid(newUid);
        } else {
            LOG.error("uid != null, return the old uid");
            return uid;
        }

    }


    private URI getUri(URIBuilder uriBuilder) {
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new ConnectorException("It is not possible to create URI" + e.getLocalizedMessage(), e);
        }
        return uri;
    }


    public void delete(Uid uid) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }
        HttpDelete request;
        URI uri = null;

        URIBuilder uriBuilder = new URIBuilder().setScheme("https").setHost(API_ENDPOINT);
        uriBuilder.setPath(USERS + "/" + uid.getUidValue());
        uri = getUri(uriBuilder);
        LOG.info("Delete: {0}", uri);
        request = new HttpDelete(uri);
        if (callRequest(request, false) == null) {
            LOG.info("Deleted user with Uid {0}", uid.getUidValue());
        }


    }


    public void updateDeltaMultiValues(Uid uid, Set<AttributeDelta> attributesDelta, OperationOptions options) {
        LOG.info("updateDeltaMultiValues uid: {0} , attributesDelta {1} , options {2}", uid, attributesDelta, options);


        for (AttributeDelta attrDelta : attributesDelta) {
            if (ATTR_BUSINESSPHONES.equals(attrDelta.getName())) {
                List<Object> addValues = attrDelta.getValuesToAdd();
                List<Object> removeValues = attrDelta.getValuesToRemove();
                if (removeValues != null && !removeValues.isEmpty()) {
                    for (Object removeValue : removeValues) {
                        Set<Attribute> attributeReplace = new HashSet<>();
                        attributeReplace.add(AttributeBuilder.build(attrDelta.getName(), removeValue));
                        updateUser(uid, attributeReplace);
                    }
                }
                if (addValues != null && !addValues.isEmpty()) {

                    for (Object addValue : addValues) {
                        LOG.info("addValue {0}", addValue);
                        Set<Attribute> attributeReplace = new HashSet<>();
                        attributeReplace.add(AttributeBuilder.build(attrDelta.getName(), addValue));
                        updateUser(uid, attributeReplace);
                    }
                }
            }
        }
    }


    public void executeQueryForUser(Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQueryForUser()");
        if (query instanceof EqualsFilter) {
            LOG.info("query instanceof EqualsFilter");
            if (((EqualsFilter) query).getAttribute() instanceof Uid) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof Uid");

                Uid uid = (Uid) ((EqualsFilter) query).getAttribute();
                if (uid.getUidValue() == null) {
                    invalidAttributeValue("Uid", query);
                }
                StringBuilder sbPath = new StringBuilder();
                sbPath.append(USERS).append("/").append(uid.getUidValue()).append("/");
                LOG.info("sbPath: {0}", sbPath);
                //not included : ATTR_PASSWORDPROFILE,ATTR_ASSIGNEDLICENSES,
                // ATTR_BUSINESSPHONES,ATTR_MAILBOXSETTINGS,ATTR_PROVISIONEDPLANS
                String customQuery = "$select=" + ATTR_ACCOUNTENABLED + "," + ATTR_DISPLAYNAME + "," +
                        ATTR_ONPREMISESIMMUTABLEID + "," + ATTR_MAILNICKNAME + "," + ATTR_USERPRINCIPALNAME + "," + ATTR_ABOUTME + "," +
                        ATTR_BIRTHDAY + "," + ATTR_CITY + "," + ATTR_COMPANYNAME + "," + ATTR_COUNTRY + "," + ATTR_DEPARTMENT + "," +
                        ATTR_GIVENNAME + "," + ATTR_HIREDATE + "," + ATTR_IMADDRESSES + "," + ATTR_ID + "," + ATTR_INTERESTS + "," +
                        ATTR_JOBTITLE + "," + ATTR_MAIL + "," + ATTR_MOBILEPHONE + "," + ATTR_MYSITE + "," + ATTR_OFFICELOCATION + "," +
                        ATTR_ONPREMISESLASTSYNCDATETIME + "," + ATTR_ONPREMISESSECURITYIDENTIFIER + "," +
                        ATTR_ONPREMISESSYNCENABLED + "," + ATTR_PASSWORDPOLICIES + "," + ATTR_PASTPROJECTS + "," +
                        ATTR_POSTALCODE + "," + ATTR_PREFERREDLANGUAGE + "," + ATTR_PREFERREDNAME + "," +
                        ATTR_PROXYADDRESSES + "," + ATTR_RESPONSIBILITIES + "," + ATTR_SCHOOLS + "," +
                        ATTR_SKILLS + "," + ATTR_STATE + "," + ATTR_STREETADDRESS + "," + ATTR_SURNAME + "," +
                        ATTR_USAGELOCATION + "," + ATTR_USERTYPE;


                JSONObject user = executeGetRequest(sbPath.toString(), customQuery, options, false);
                LOG.info("JSONObject user {0}", user.toString());
                processingObjectFromGET(user, handler);

            } else if (((EqualsFilter) query).getAttribute().getName().equals("displayName")) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof Name");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("Name", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + ATTR_DISPLAYNAME + " eq '" + attributeValue + "'";

                JSONObject users = executeGetRequest(USERS, customQuery, options, true);
                LOG.info("JSONObject users {0}", users.toString());
                processingMultipleObjectFromGET(users, handler);

            } else if (((EqualsFilter) query).getAttribute().getName().equals(ATTR_GIVENNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof givenName");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_GIVENNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + ATTR_GIVENNAME + " eq '" + attributeValue + "'";

                JSONObject users = executeGetRequest(USERS, customQuery, options, true);
                LOG.info("JSONObject users {0}", users.toString());
                processingMultipleObjectFromGET(users, handler);

            } else if (((EqualsFilter) query).getAttribute().getName().equals(ATTR_JOBTITLE)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof jobTitle");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_JOBTITLE, query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + ATTR_JOBTITLE + " eq '" + attributeValue + "'";

                JSONObject users = executeGetRequest(USERS, customQuery, options, true);


                LOG.info("JSONObject users {0}", users.toString());
                processingMultipleObjectFromGET(users, handler);

            } else if (((EqualsFilter) query).getAttribute().getName().equals(ATTR_USERPRINCIPALNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof userPrincipalName");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_USERPRINCIPALNAME, query);
                }

                StringBuilder sbPath = new StringBuilder();
                String attributeValue = allValues.get(0).toString();
                sbPath.append(USERS).append("/").append(attributeValue);

                LOG.info("value {0}", attributeValue);

                JSONObject user = executeGetRequest(sbPath.toString(), null, options, false);
                LOG.info("JSONObject user {0}", user.toString());
                processingObjectFromGET(user, handler);

            }

        } else if (query instanceof ContainsFilter) {
            if (((ContainsFilter) query).getAttribute().getName().equals(ATTR_JOBTITLE)) {
                LOG.info("((ContainsFilter) query).getAttribute() instanceof jobTitle");

                List<Object> allValues = ((ContainsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_JOBTITLE, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + STARTSWITH + "(" + ATTR_JOBTITLE + ",'" + attributeValue + "')";
                JSONObject users = executeGetRequest(USERS, customQuery, options, true);
                LOG.info("JSONObject users {0}", users.toString());
                processingMultipleObjectFromGET(users, handler);

            } else if (((ContainsFilter) query).getAttribute().getName().equals(ATTR_GIVENNAME)) {
                LOG.info("((ContainsFilter) query).getAttribute() instanceof givenName");

                List<Object> allValues = ((ContainsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_GIVENNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + STARTSWITH + "(" + ATTR_GIVENNAME + ",'" + attributeValue + "')";
                JSONObject users = executeGetRequest(USERS, customQuery, options, true);
                LOG.info("JSONObject users {0}", users.toString());
                processingMultipleObjectFromGET(users, handler);

            } else if (((ContainsFilter) query).getAttribute().getName().equals(ATTR_USERPRINCIPALNAME)) {
                LOG.info("((ContainsFilter) query).getAttribute() instanceof userPrincipialName");

                List<Object> allValues = ((ContainsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_USERPRINCIPALNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + STARTSWITH + "(" + ATTR_USERPRINCIPALNAME + ",'" + attributeValue + "')";
                JSONObject users = executeGetRequest(USERS, customQuery, options, true);
                LOG.info("JSONObject users {0}", users.toString());
                processingMultipleObjectFromGET(users, handler);

            } else if (((ContainsFilter) query).getAttribute().getName().equals(ATTR_DISPLAYNAME)) {
                LOG.info("((ContainsFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((ContainsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_DISPLAYNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + STARTSWITH + "(" + ATTR_DISPLAYNAME + ",'" + attributeValue + "')";
                JSONObject users = executeGetRequest(USERS, customQuery, options, true);
                LOG.info("JSONObject users {0}", users.toString());
                processingMultipleObjectFromGET(users, handler);

            }


        } else if (query == null) {
            LOG.info("query==null");
            JSONObject users = executeGetRequest(USERS, null, options, true);
            LOG.info("JSONObject users {0}", users.toString());
            processingMultipleObjectFromGET(users, handler);


        }


    }

    private void processingObjectFromGET(JSONObject user, ResultsHandler handler) {
        LOG.info("processingObjectFromGET (Object)");
        ConnectorObjectBuilder builder = convertUserJSONObjectToConnectorObject(user);
        ConnectorObject connectorObject = builder.build();
        LOG.info("convertUserToConnectorObject, user: {0}, \n\tconnectorObject: {1}", user.get("id"), connectorObject.toString());
        handler.handle(connectorObject);
    }

    private void processingMultipleObjectFromGET(JSONObject users, ResultsHandler handler) {
        LOG.info("processingMultipleObjectFromGET (Object)");

        String jsonStr = users.toString();
        JSONObject jsonObj = new JSONObject(jsonStr);

        JSONArray value;
        try {
            value = jsonObj.getJSONArray("value");
        } catch (JSONException e) {
            LOG.info("not find anything");
            return;
        }
        int length = value.length();
        LOG.info("jsonObj length: {0}", length);

        for (int i = 0; i < length; i++) {
            JSONObject user = value.getJSONObject(i);
            processingObjectFromGET(user, handler);

        }
    }


    private ConnectorObjectBuilder convertUserJSONObjectToConnectorObject(JSONObject user) {
        LOG.info("convertUserJSONObjectToConnectorObject");
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.ACCOUNT);

        getUIDIfExists(user, ATTR_ID, builder);

        getNAMEIfExists(user, ATTR_USERPRINCIPALNAME, builder);

        getIfExists(user, ATTR_ACCOUNTENABLED, Boolean.class, builder);
        getIfExists(user, ATTR_DISPLAYNAME, String.class, builder);
        getIfExists(user, ATTR_ONPREMISESIMMUTABLEID, String.class, builder);
        getIfExists(user, ATTR_MAILNICKNAME, String.class, builder);
        getIfExists(user, ATTR_ABOUTME, String.class, builder);
        getIfExists(user, ATTR_BIRTHDAY, String.class, builder);
        getIfExists(user, ATTR_CITY, String.class, builder);
        getIfExists(user, ATTR_COMPANYNAME, String.class, builder);
        getIfExists(user, ATTR_COUNTRY, String.class, builder);
        getIfExists(user, ATTR_DEPARTMENT, String.class, builder);
        getIfExists(user, ATTR_GIVENNAME, String.class, builder);
        getIfExists(user, ATTR_HIREDATE, String.class, builder);
        getMultiIfExists(user, ATTR_IMADDRESSES, builder);
        getIfExists(user, ATTR_ID, String.class, builder);
        //getMultiIfExists(user, ATTR_BUSINESSPHONES, builder);
        getMultiIfExists(user, ATTR_INTERESTS, builder);
        getIfExists(user, ATTR_JOBTITLE, String.class, builder);
        getIfExists(user, ATTR_MAIL, String.class, builder);
        getIfExists(user, ATTR_MOBILEPHONE, String.class, builder);
        getIfExists(user, ATTR_MYSITE, String.class, builder);
        getIfExists(user, ATTR_OFFICELOCATION, String.class, builder);
        getIfExists(user, ATTR_ONPREMISESLASTSYNCDATETIME, String.class, builder);
        getIfExists(user, ATTR_ONPREMISESSECURITYIDENTIFIER, String.class, builder);
        getIfExists(user, ATTR_ONPREMISESSYNCENABLED, Boolean.class, builder);
        getIfExists(user, ATTR_PASSWORDPOLICIES, String.class, builder);
        getMultiIfExists(user, ATTR_PASTPROJECTS, builder);
        getIfExists(user, ATTR_POSTALCODE, String.class, builder);
        getIfExists(user, ATTR_PREFERREDLANGUAGE, String.class, builder);
        getIfExists(user, ATTR_PREFERREDNAME, String.class, builder);
        getMultiIfExists(user, ATTR_RESPONSIBILITIES, builder);
        getMultiIfExists(user, ATTR_SCHOOLS, builder);
        getMultiIfExists(user, ATTR_SKILLS, builder);
        getIfExists(user, ATTR_STATE, String.class, builder);
        getIfExists(user, ATTR_STREETADDRESS, String.class, builder);
        getIfExists(user, ATTR_SURNAME, String.class, builder);
        getIfExists(user, ATTR_USAGELOCATION, String.class, builder);
        getIfExists(user, ATTR_USERTYPE, String.class, builder);

        getMultiIfExists(user, ATTR_PROXYADDRESSES, builder);
        return builder;
    }


    protected void invalidAttributeValue(String attrName, Filter query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Value of").append(attrName).append("attribute not provided for query: ").append(query);
        throw new InvalidAttributeValueException(sb.toString());
    }

}
