package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.common.GuardedStringAccessor;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;


public class UserProcessing extends ObjectProcessing {

    private static final String USERS = "/users";
    private static final String MESSAGES = "messages";
    private static final String INVITATIONS = "/invitations";
    private static final String ASSIGN_LICENSES = "/assignLicense";

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

    private static final String ATTR_USERPRINCIPALNAME = "userPrincipalName";
    private static final String ATTR_MEMBER_OF_GROUP = "memberOfGroup";
    //private static final String ATTR_MEMBER_OF_ROLE= "memberOfRole";


    //optional
    private static final String ATTR_ABOUTME = "aboutMe";

    //ASSIGNEDLICENSES
    private static final String ATTR_ASSIGNEDLICENSES = "assignedLicenses";
    private static final String ATTR_SKUID = "skuId";
    private static final String ATTR_DISABLEDPLANS = "disabledPlans";
    private static final String ATTR_ASSIGNEDLICENSES__SKUID = ATTR_ASSIGNEDLICENSES + "." + ATTR_SKUID;

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

    // INVITES
    private static final String ATTR_INVITED_USER = "invitedUser";
    private static final String ATTR_INVITED_USER_EMAIL = "invitedUserEmailAddress";
    private static final String ATTR_INVITE_REDIRECT = "inviteRedirectUrl";
    private static final String ATTR_INVITE_DISPNAME = "invitedUserDisplayName";
    private static final String ATTR_INVITE_SEND_MESSAGE = "sendInvitationMessage";
    private static final String ATTR_INVITE_MSG_INFO = "invitedUserMessageInfo";
    private static final String ATTR_INVITED_USER_TYPE = "invitedUserType";

    private static final String ATTR_ICF_PASSWORD = "__PASSWORD__";
    private static final String ATTR_ICF_ENABLED = "__ENABLE__";


    // technical constants
    private static final String TYPE = "@odata.type";
    private static final String TYPE_GROUP = "#microsoft.graph.group";

    public UserProcessing(MSGraphConfiguration configuration, MSGraphConnector connector) {
        super(configuration, ICFPostMapper.builder()
                .remap(ATTR_ICF_PASSWORD, "passwordProfile.password")
                .postProcess(ATTR_ICF_PASSWORD, pwAttr -> {
                    GuardedString guardedString = (GuardedString) AttributeUtil.getSingleValue(pwAttr);
                    GuardedStringAccessor accessor = new GuardedStringAccessor();
                    guardedString.access(accessor);
                    final List<Object> rv = new LinkedList<>();
                    rv.add(accessor.getClearString());
                    return rv;
                })
                .remap(ATTR_ICF_ENABLED, ATTR_ACCOUNTENABLED)
                .build()
        );
    }


    public void buildUserObjectClass(SchemaBuilder schemaBuilder) {
        schemaBuilder.defineObjectClass(objectClassInfo());
    }

    @Override
    protected ObjectClassInfo objectClassInfo() {
        ObjectClassInfoBuilder userObjClassBuilder = new ObjectClassInfoBuilder();
        userObjClassBuilder.setType(ObjectClass.ACCOUNT_NAME);

        userObjClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        userObjClassBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);

        //Read-only,
        AttributeInfoBuilder attrId = new AttributeInfoBuilder(ATTR_ID);
        attrId.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrId.build());

        // Supports $filter and $orderby.
        AttributeInfoBuilder attrUserPrincipalName = new AttributeInfoBuilder(ATTR_USERPRINCIPALNAME);
        attrUserPrincipalName.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrUserPrincipalName.build());

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


//        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
//                ATTR_PASSWORDPROFILE + "." + ATTR_PASSWORD)
//                .setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).build());

        //optional

        //Supports $filter.
        AttributeInfoBuilder attrOnPremisesImmutableId = new AttributeInfoBuilder(ATTR_ONPREMISESIMMUTABLEID);
        attrOnPremisesImmutableId.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesImmutableId.build());

        AttributeInfoBuilder attrAboutMe = new AttributeInfoBuilder(ATTR_ABOUTME);
        attrAboutMe.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrAboutMe.build());


        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_MEMBER_OF_GROUP)
                .setRequired(false).setType(String.class).setMultiValued(true)
                .setCreateable(false).setUpdateable(false).setReadable(true)
                .build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ATTR_ASSIGNEDLICENSES__SKUID)
                .setRequired(false)
                //.setType(GUID.class)
                .setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(true).build());

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
        attrCompanyName.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
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
        attrMail.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
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


        return userObjClassBuilder.build();
    }

    private Set<Attribute> prepareAttributes(Uid uid, Set<Attribute> replaceAttributes, Set<AttributeDelta> deltas, boolean create) {
        AttributeDeltaBuilder delta = new AttributeDeltaBuilder();
        delta.setName(ATTR_ASSIGNEDLICENSES__SKUID);
        List<Object> addLicenses = new ArrayList<>();
        // filter out the assignedLicense.skuId attribute, which must be handled separately
        Set<Attribute> preparedAttributes = replaceAttributes.stream()
            .filter(it -> {
                if (it.getName().equals(ATTR_ASSIGNEDLICENSES__SKUID)) {
                    if (it.getValue() != null)
                        addLicenses.addAll(it.getValue());
                    return false;
                } else
                    return true;
            })
            .collect(Collectors.toSet());
        delta.addValueToAdd(addLicenses);
        // read and fill-out the old values
        if (!create) {
            LOG.info("Read old licenses, Uid {0}", uid);
            final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
            final String selectorLicenses = selector(ATTR_ID, ATTR_USERPRINCIPALNAME, ATTR_ASSIGNEDLICENSES);
            final OperationOptions options = new OperationOptions(new HashMap<>());

            JSONObject user = endpoint.executeGetRequest(USERS + "/" + uid.getUidValue() + "/", selectorLicenses, options, false);
            ConnectorObject co = convertUserJSONObjectToConnectorObject(user).build();
            //LOG.info("License: fetched user {0}", co);
            Attribute attrLicense = co.getAttributeByName(ATTR_ASSIGNEDLICENSES__SKUID);
            if (attrLicense != null && attrLicense.getValue() != null) {
                // the assigned licenses (=added or replaced value) should not be removed
                List<Object> removeLicenses = CollectionUtil.newList(attrLicense.getValue());
                removeLicenses.removeAll(addLicenses);
                delta.addValueToRemove(removeLicenses);
            }
        }
        deltas.add(delta.build());
        return preparedAttributes;
    }

    private JSONArray buildLicensesJSON(Collection<Object> licenses) {
        if (licenses == null)
            return new JSONArray();

        Map<String,List<String>> disabledPlansMap = new HashMap<>();
        for (String licensePlans : getConfiguration().getDisabledPlans()) {
            String a[] = licensePlans.split(":", 2);
            if (a.length != 2)
                continue;
            String skuId = a[0];
            String disabledPlans[] = a[1].split(",");
            if (disabledPlans.length >= 1)
                disabledPlansMap.put(skuId, Arrays.asList(disabledPlans));
        }

        JSONArray json = new JSONArray();
        licenses.forEach(it -> {
            JSONObject jo = new JSONObject();
            String skuId = (String)it;
            jo.put(ATTR_SKUID, skuId);
            if (disabledPlansMap.containsKey(skuId))
                jo.put(ATTR_DISABLEDPLANS, new JSONArray(disabledPlansMap.get(skuId)));
            json.put(jo);
        });
        return json;
    }

    private void assignLicenses(Uid uid, AttributeDelta deltaLicense) {
        final List<Object> addLicenses = deltaLicense.getValuesToAdd();
        final List<Object> removeLicenses = deltaLicense.getValuesToRemove();
        if ((addLicenses == null || addLicenses.isEmpty()) && (removeLicenses == null || removeLicenses.isEmpty()))
            return;

        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS + "/" + uid.getUidValue() + ASSIGN_LICENSES);
        HttpEntityEnclosingRequestBase request = null;
        URI uri = endpoint.getUri(uriBuilder);
        request = new HttpPost(uri);

        // Office365 removes licenses automatically on user disable:
        // ==> ignore problems when unassigning licenses + separate call for it
        if (removeLicenses != null && !removeLicenses.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("addLicenses", new JSONArray());
            jsonObject.put("removeLicenses", removeLicenses == null ? new JSONArray() : new JSONArray(removeLicenses));
            try {
                endpoint.callRequestNoContent(request, null, jsonObject);
            } catch (InvalidAttributeValueException ex) {
                LOG.warn(ex, "Problem when unassiging licenses {0}, ignoring", removeLicenses);
            }
        }
        if (addLicenses != null && !addLicenses.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("addLicenses", buildLicensesJSON(addLicenses));
            jsonObject.put("removeLicenses", new JSONArray());
            endpoint.callRequestNoContent(request, null, jsonObject);
        }
    }

    public void updateUser(Uid uid, Set<Attribute> attributes) {
        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS + "/" + uid.getUidValue());
        HttpEntityEnclosingRequestBase request = null;
        URI uri = endpoint.getUri(uriBuilder);
        LOG.info("update user, PATCH");
        LOG.info("Path: {0}", uri);
        request = new HttpPatch(uri);
        final Set<AttributeDelta> deltas = new HashSet<>();
        Set<Attribute> updateAttributes = prepareAttributes(uid, attributes, deltas, false);

        List<Object> jsonObjectaccount = buildLayeredAtrribute(updateAttributes);
        endpoint.callRequestNoContentNoJson(request, jsonObjectaccount);
        assignLicenses(uid, AttributeDeltaUtil.find(ATTR_ASSIGNEDLICENSES__SKUID, deltas));
    }

    public Uid createUser(Uid uid, Set<Attribute> attributes) {
        LOG.info("Start createUser, Uid: {0}, attributes: {1}", uid, attributes);

        if (attributes == null || attributes.isEmpty()) {
            throw new InvalidAttributeValueException("attributes not provided or empty");
        }

        if (uid != null) return uid;

        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());

        final String emailAddress = attributes.stream()
                .filter(a -> a.is(ATTR_MAIL))
                .map(a -> a.getValue().get(0).toString())
                .findFirst().get();

        final boolean hasUPN = attributes.stream()
                .filter(a -> a.is(ATTR_USERPRINCIPALNAME))
                .anyMatch(a -> !a.getValue().isEmpty());


        final boolean invite = !emailAddress.split("@")[1].equals(getConfiguration().getTenantId()) &&
                getConfiguration().isInviteGuests() &&
                !hasUPN;

        final Set<AttributeDelta> deltas = new HashSet<>();
        Set<Attribute> createAttributes = prepareAttributes(uid, attributes, deltas, true);

        final String newUid;
        if (invite) {
            AttributesValidator.builder()
                    .withNonEmpty()
                    .withExactlyOne(ATTR_MAIL)
                    .build()
                    .validate(createAttributes);

            final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(INVITATIONS);
            final URI uri = endpoint.getUri(uriBuilder);
            final HttpEntityEnclosingRequestBase request = new HttpPost(uri);
            final JSONObject payload = buildInvitation(createAttributes);
            final JSONObject jsonRequest = endpoint.callRequest(request, payload, true);
            newUid = jsonRequest.getJSONObject(ATTR_INVITED_USER).getString(ATTR_ID);
        } else {
            AttributesValidator.builder()
                    .withNonEmpty(ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME, ATTR_ICF_PASSWORD)
                    .withExactlyOne(ATTR_USERPRINCIPALNAME)
                    .withRegex(ATTR_USERPRINCIPALNAME, "[^@]+@[^@]+")
                    .build()
                    .validate(createAttributes);

            final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS);
            final URI uri = endpoint.getUri(uriBuilder);
            final HttpEntityEnclosingRequestBase request = new HttpPost(uri);
            final JSONObject payload = buildLayeredAttributeJSON(createAttributes);
            final JSONObject jsonRequest = endpoint.callRequest(request, payload, true);
            newUid = jsonRequest.getString(ATTR_ID);
        }

        assignLicenses(new Uid(newUid), AttributeDeltaUtil.find(ATTR_ASSIGNEDLICENSES__SKUID, deltas));

        return new Uid(newUid);
    }

    public void delete(Uid uid) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }
        HttpDelete request;

        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS + "/" + uid.getUidValue());
        URI uri = endpoint.getUri(uriBuilder);
        LOG.info("Delete: {0}", uri);
        request = new HttpDelete(uri);
        if (endpoint.callRequest(request, false) == null) {
            LOG.info("Deleted user with Uid {0}", uid.getUidValue());
        }
    }


    public void updateDeltaMultiValues(Uid uid, Set<AttributeDelta> attributesDelta, OperationOptions options) {
        LOG.info("updateDeltaMultiValues uid: {0} , attributesDelta {1} , options {2}", uid, attributesDelta, options);


        for (AttributeDelta attrDelta : attributesDelta) {
            List<Object> addValues = attrDelta.getValuesToAdd();
            List<Object> removeValues = attrDelta.getValuesToRemove();

            switch(attrDelta.getName()) {
            case ATTR_BUSINESSPHONES:
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
                break;
            case ATTR_ASSIGNEDLICENSES__SKUID:
                assignLicenses(uid, attrDelta);
                break;
            }
        }
    }


    public void executeQueryForUser(Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQueryForUser()");
        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final String selectorSingle = selector(
                ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME,
                        ATTR_ONPREMISESIMMUTABLEID, ATTR_MAILNICKNAME, ATTR_USERPRINCIPALNAME, ATTR_ABOUTME,
                        ATTR_BIRTHDAY, ATTR_CITY, ATTR_COMPANYNAME, ATTR_COUNTRY, ATTR_DEPARTMENT,
                        ATTR_GIVENNAME, ATTR_HIREDATE, ATTR_IMADDRESSES, ATTR_ID, ATTR_INTERESTS,
                        ATTR_JOBTITLE, ATTR_MAIL, ATTR_MOBILEPHONE, ATTR_MYSITE, ATTR_OFFICELOCATION,
                        ATTR_ONPREMISESLASTSYNCDATETIME, ATTR_ONPREMISESSECURITYIDENTIFIER,
                        ATTR_ONPREMISESSYNCENABLED, ATTR_PASSWORDPOLICIES, ATTR_PASTPROJECTS,
                        ATTR_POSTALCODE, ATTR_PREFERREDLANGUAGE, ATTR_PREFERREDNAME,
                        ATTR_PROXYADDRESSES, ATTR_RESPONSIBILITIES, ATTR_SCHOOLS,
                        ATTR_SKILLS, ATTR_STATE, ATTR_STREETADDRESS, ATTR_SURNAME,
                        ATTR_USAGELOCATION, ATTR_USERTYPE, ATTR_ASSIGNEDLICENSES);

        final String selectorList = selector(
                ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME,
                ATTR_ONPREMISESIMMUTABLEID, ATTR_MAILNICKNAME, ATTR_USERPRINCIPALNAME,
                ATTR_CITY, ATTR_COMPANYNAME, ATTR_COUNTRY, ATTR_DEPARTMENT,
                ATTR_GIVENNAME, ATTR_IMADDRESSES, ATTR_ID,
                ATTR_JOBTITLE, ATTR_MAIL, ATTR_MOBILEPHONE, ATTR_OFFICELOCATION,
                ATTR_ONPREMISESLASTSYNCDATETIME, ATTR_ONPREMISESSECURITYIDENTIFIER,
                ATTR_ONPREMISESSYNCENABLED, ATTR_PASSWORDPOLICIES,
                ATTR_POSTALCODE, ATTR_PREFERREDLANGUAGE,
                ATTR_PROXYADDRESSES,
                ATTR_STATE, ATTR_STREETADDRESS, ATTR_SURNAME,
                ATTR_USAGELOCATION, ATTR_USERTYPE, ATTR_ASSIGNEDLICENSES);

        if (query instanceof EqualsFilter) {
            final EqualsFilter equalsFilter = (EqualsFilter) query;
            LOG.info("query instanceof EqualsFilter");
            if (equalsFilter.getAttribute() instanceof Uid) {
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

                JSONObject user = endpoint.executeGetRequest(sbPath.toString(), selectorSingle, options, false);
                LOG.info("JSONObject user {0}", user.toString());
                handleJSONObject(user, handler);

            } else if (equalsFilter.getAttribute().getName().equals(ATTR_USERPRINCIPALNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof userPrincipalName");

                final String attributeValue = getAttributeFirstValue(equalsFilter);
                StringBuilder sbPath = new StringBuilder();
                sbPath.append(USERS).append("/").append(attributeValue);

                LOG.info("value {0}", attributeValue);

                JSONObject user = endpoint.executeGetRequest(sbPath.toString(), selectorSingle, options, false);
                LOG.info("JSONObject user {0}", user.toString());
                handleJSONObject(user, handler);

            } else if (Arrays.asList(ATTR_DISPLAYNAME, ATTR_GIVENNAME, ATTR_JOBTITLE)
                    .contains(equalsFilter.getAttribute().getName())
            ) {
                final String attributeValue = getAttributeFirstValue(equalsFilter);
                final String filter = "$filter=" + equalsFilter.getAttribute().getName() + " eq '" + attributeValue + "'";
                JSONObject users = endpoint.executeGetRequest(USERS, selectorList + '&' + filter, options, true);
                handleJSONArray(users, handler);
            }
        } else if (query instanceof ContainsFilter) {
            final ContainsFilter containsFilter = (ContainsFilter) query;
            if (Arrays.asList(ATTR_JOBTITLE, ATTR_GIVENNAME, ATTR_USERPRINCIPALNAME, ATTR_DISPLAYNAME)
                    .contains(containsFilter.getAttribute().getName())
            ) {
                final String attributeName = containsFilter.getAttribute().getName();
                final String attributeValue = getAttributeFirstValue(containsFilter);
                LOG.info("value {0}", attributeValue);
                final String filter = "$filter=" + STARTSWITH + "(" + attributeName + ",'" + attributeValue + "')";
                JSONObject users = endpoint.executeGetRequest(USERS, selectorList + '&' + filter, options, true);
                LOG.info("JSONObject users {0}", users.toString());
                handleJSONArray(users, handler);
            }
        } else if (query == null) {
            LOG.info("query==null");
            JSONObject users = endpoint.executeGetRequest(USERS, selectorList, options, true);
            LOG.info("JSONObject users {0}", users.toString());
            handleJSONArray(users, handler);
        }
    }

    @Override
    protected boolean handleJSONObject(JSONObject user, ResultsHandler handler) {
        LOG.info("processingObjectFromGET (Object)");
        ConnectorObject connectorObject = convertUserJSONObjectToConnectorObject(
                saturateGroupMembership(user)
        ).build();
        LOG.info("convertUserToConnectorObject, user: {0}, \n\tconnectorObject: {1}", user.get("id"), connectorObject.toString());
        return handler.handle(connectorObject);
    }

    private JSONObject buildInvitation(Set<Attribute> attributes) {
        final String displayName = getStringValue(attributes, ATTR_DISPLAYNAME);
        final String mail = getStringValue(attributes, ATTR_MAIL);
        final String userType = getStringValue(attributes, ATTR_USERTYPE);

        final JSONObject invitation = new JSONObject()
                .put(ATTR_INVITE_SEND_MESSAGE, getConfiguration().isSendInviteMail())
                .put(ATTR_INVITE_MSG_INFO, getConfiguration().getInviteMessage())
                .put(ATTR_INVITE_REDIRECT, getConfiguration().getInviteRedirectUrl());

        if (displayName != null) invitation.put(ATTR_INVITE_DISPNAME, displayName);
        if (mail != null) invitation.put(ATTR_INVITED_USER_EMAIL, mail);
        if (userType != null) invitation.put(ATTR_INVITED_USER_TYPE, userType);

        return invitation;
    }

    private String getStringValue(Set<Attribute> attributes, String attributeName) {
        final Optional<Attribute> ao = attributes.stream().filter(a -> a.is(attributeName)).findFirst();
        if (!ao.isPresent()) return null;
        final Optional<String> aov = ao.get().getValue().stream().map(v -> v.toString()).findFirst();
        return aov.isPresent() ? aov.get() : null;
    }

    private JSONObject saturateGroupMembership(JSONObject user) {
        final String uid = user.getString(ATTR_ID);
        final List<String> groups = new GraphEndpoint(getConfiguration()).executeGetRequest(
                String.format("/users/%s/memberOf", uid), "$select=id", null, false
        ).getJSONArray("value").toList().stream()
                .filter(o -> TYPE_GROUP.equals(((Map)o).get(TYPE)))
                .map(o -> (String)((Map)o).get(ATTR_ID))
                .collect(Collectors.toList());
        user.put(ATTR_MEMBER_OF_GROUP, new JSONArray(groups)); 
        //user.put(ATTR_MEMBER_OF_GROUP, new JSONArray()); //Comment this line out after putting back in above
        return user;
    }

    public ConnectorObjectBuilder convertUserJSONObjectToConnectorObject(JSONObject user) {
        LOG.info("convertUserJSONObjectToConnectorObject");
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.ACCOUNT);

        getUIDIfExists(user, ATTR_ID, builder);
        getNAMEIfExists(user, ATTR_USERPRINCIPALNAME, builder);
        getAndRenameIfExists(user, ATTR_ACCOUNTENABLED, Boolean.class, ATTR_ICF_ENABLED, builder);

        getIfExists(user, ATTR_ACCOUNTENABLED, Boolean.class, builder);
        getIfExists(user, ATTR_ID, String.class, builder);
        getIfExists(user, ATTR_USERPRINCIPALNAME, String.class, builder);
        getIfExists(user, ATTR_ACCOUNTENABLED, Boolean.class, builder);
        getIfExists(user, ATTR_DISPLAYNAME, String.class, builder);
        getIfExists(user, ATTR_ONPREMISESIMMUTABLEID, String.class, builder);
        getIfExists(user, ATTR_MAILNICKNAME, String.class, builder);
        getIfExists(user, ATTR_ABOUTME, String.class, builder);
        getMultiIfExists(user, ATTR_MEMBER_OF_GROUP, builder);
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
        getFromArrayIfExists(user, ATTR_ASSIGNEDLICENSES, ATTR_SKUID, String.class, builder);
        return builder;
    }


}
