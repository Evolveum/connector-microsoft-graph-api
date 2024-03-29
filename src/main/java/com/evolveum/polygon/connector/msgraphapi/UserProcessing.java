package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.common.GuardedStringAccessor;
import com.evolveum.polygon.connector.msgraphapi.util.ResourceQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class UserProcessing extends ObjectProcessing {

    private final static String API_ENDPOINT = "https://graph.microsoft.com/v1.0";
    private static final String USERS = "/users";
    private static final String MESSAGES = "messages";
    private static final String INVITATIONS = "/invitations";
    private static final String ASSIGN_LICENSES = "/assignLicense";

    private final static String ROLE_ASSIGNMENT = "/roleManagement/directory/roleAssignments";

    private static final String MANAGER = "/manager/$ref";


    private static final String SPACE = "%20";
    private static final String QUOTATION = "%22";
    private static final String EQUALS = "eq";
    private static final String DOLLAR = "%24";
    private static final String QUESTIONMARK = "%3F";
    private static final String FILTER = "filter";
    private static final String EQUAL = "%3D";
    private static final String SLASH = "%2F";
    private static final String EXPAND = "expand";

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
    private static final String ATTR_OWNER_OF_GROUP = "ownerOfGroup";
    private static final String ATTR_MEMBER_OF_ROLE = "memberOfRole";


    //optional
    private static final String ATTR_ABOUTME = "aboutMe"; // Need SPO license
    private static final String ATTR_USERPHOTO = "photo";

    //Sign in, auxiliary computed attribute representing the last sign in
    private static final String ATTR_SIGN_IN = "lastSignIn";

    //ASSIGNEDLICENSES
    private static final String ATTR_ASSIGNEDLICENSES = "assignedLicenses";
    private static final String ATTR_SKUID = "skuId";
    private static final String ATTR_DISABLEDPLANS = "disabledPlans";
    private static final String ATTR_ASSIGNEDLICENSES_SKUID = ATTR_ASSIGNEDLICENSES + "." + ATTR_SKUID;

    //ASSIGNEDPLAN
    private static final String ATTR_ASSIGNEDPLANS = "assignedPlans";
    private static final String ATTR_ASSIGNEDDATETIME = "assignedDateTime";
    private static final String ATTR_CAPABILITYSTATUS = "capabilityStatus";
    private static final String ATTR_SERVICE = "service";
    private static final String ATTR_SERVICEPLANID = "servicePlanId";

    private static final String ATTR_BIRTHDAY = "birthday"; // Need SPO license
    private static final String ATTR_BUSINESSPHONES = "businessPhones";
    private static final String ATTR_CITY = "city";
    private static final String ATTR_COMPANYNAME = "companyName";
    private static final String ATTR_COUNTRY = "country";
    private static final String ATTR_DEPARTMENT = "department";
    private static final String ATTR_GIVENNAME = "givenName";
    private static final String ATTR_HIREDATE = "hireDate"; // Need SPO license
    private static final String ATTR_ID = "id";
    private static final String ATTR_IMADDRESSES = "imAddresses";
    private static final String ATTR_INTERESTS = "interests"; // Need SPO license
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
    private static final String ATTR_MYSITE = "mySite"; // Need SPO license
    private static final String ATTR_OFFICELOCATION = "officeLocation";
    private static final String ATTR_ONPREMISESLASTSYNCDATETIME = "onPremisesLastSyncDateTime";
    private static final String ATTR_ONPREMISESSECURITYIDENTIFIER = "onPremisesSecurityIdentifier";
    private static final String ATTR_ONPREMISESSYNCENABLED = "onPremisesSyncEnabled";
    private static final String ATTR_PASSWORDPOLICIES = "passwordPolicies";
    private static final String ATTR_PASTPROJECTS = "pastProjects"; // Need SPO license
    private static final String ATTR_POSTALCODE = "postalCode";
    private static final String ATTR_PREFERREDLANGUAGE = "preferredLanguage";
    private static final String ATTR_PREFERREDNAME = "preferredName"; // Need SPO license

    //provisionplans
    private static final String ATTR_PROVISIONEDPLANS = "provisionedPlans";
    private static final String ATTR_PROVISIONINGSTATUS = "provisioningStatus";

    private static final String ATTR_PROXYADDRESSES = "proxyAddresses";
    private static final String ATTR_RESPONSIBILITIES = "responsibilities"; // Need SPO license
    private static final String ATTR_SCHOOLS = "schools"; // Need SPO license
    private static final String ATTR_SKILLS = "skills"; // Need SPO license
    private static final String ATTR_STATE = "state";
    private static final String ATTR_STREETADDRESS = "streetAddress";
    private static final String ATTR_SURNAME = "surname";
    private static final String ATTR_USAGELOCATION = "usageLocation";
    private static final String ATTR_USERTYPE = "userType";
    private static final String ATTR_EMPLOYEE_HIRE_DATE = "employeeHireDate";
    private static final String ATTR_EMPLOYEE_LEAVE_DATE_TIME = "employeeLeaveDateTime";
    private static final String ATTR_EMPLOYEE_TYPE = "employeeType";
    private static final String ATTR_FAX_NUMBER = "faxNumber";
    private static final String ATTR_EMPLOYEE_ID = "employeeId";

    // INVITES
    private static final String ATTR_INVITED_USER = "invitedUser";
    private static final String ATTR_INVITED_USER_EMAIL = "invitedUserEmailAddress";
    private static final String ATTR_INVITE_REDIRECT = "inviteRedirectUrl";
    private static final String ATTR_INVITE_DISPNAME = "invitedUserDisplayName";
    private static final String ATTR_INVITE_SEND_MESSAGE = "sendInvitationMessage";
    private static final String ATTR_INVITE_MSG_INFO = "invitedUserMessageInfo";
    private static final String ATTR_INVITED_USER_TYPE = "invitedUserType";

    // GUEST ACCOUNT STATUS
    private static final String ATTR_EXTERNALUSERSTATE = "externalUserState";
    private static final String ATTR_EXTERNALUSERSTATECHANGEDATETIME = "externalUserStateChangeDateTime";

    // MANAGER
    private static final String ATTR_MANAGER = "manager";
    private static final String ATTR_MANAGER_ID = ATTR_MANAGER + "." + ATTR_ID;


    private static final String ATTR_ICF_PASSWORD = "__PASSWORD__";
    private static final String ATTR_ICF_ENABLED = "__ENABLE__";

    private static final String O_REMOVED = "@removed";

    // extend
    private static final String ATTR_ONPREMISESEXTENSIONATTRIBUTES = "onPremisesExtensionAttributes";
    private  static final String EXTENSION_ATTRIBUTE = "extensionAttribute";
    private static final int NUMBER_OF_EXTENSIONS = 15;
    // technical constants
    private static final String TYPE = "@odata.type";
    private static final String TYPE_GROUP = "#microsoft.graph.group";

    // SPO(SharePoint Online) attributes
    protected static final Set<String> SPO_ATTRS = Stream.of(
            ATTR_ABOUTME,
            ATTR_BIRTHDAY,
            ATTR_HIREDATE,
            ATTR_INTERESTS,
            ATTR_MYSITE,
            ATTR_PASTPROJECTS,
            ATTR_PREFERREDNAME,
            ATTR_RESPONSIBILITIES,
            ATTR_SCHOOLS,
            ATTR_SKILLS
    ).collect(Collectors.toSet());

    protected static final Set<String> EXCLUDE_ATTRS_OF_USER = Stream.of(
            ATTR_MANAGER_ID,
            ATTR_ASSIGNEDLICENSES_SKUID,
            ATTR_USERPHOTO
    ).collect(Collectors.toSet());

    protected static final Set<String> UPDATABLE_MULTIPLE_VALUE_ATTRS_OF_USER = Stream.of(
            ATTR_BUSINESSPHONES,
            ATTR_INTERESTS,
            ATTR_PASTPROJECTS,
            ATTR_RESPONSIBILITIES,
            ATTR_SCHOOLS,
            ATTR_SKILLS
    ).collect(Collectors.toSet());

    public UserProcessing(GraphEndpoint graphEndpoint, SchemaTranslator schemaTranslator) {
        super(graphEndpoint, ICFPostMapper.builder()
                .remap(ATTR_ICF_PASSWORD, "passwordProfile.password")
                .postProcess(ATTR_ICF_PASSWORD, pwAttr -> {
                    GuardedString guardedString = (GuardedString) pwAttr.getSingleValue();
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
    protected String type() {
        return ObjectClass.ACCOUNT_NAME;
    }

    @Override
    protected ObjectClassInfo objectClassInfo() {
        ObjectClassInfoBuilder userObjClassBuilder = new ObjectClassInfoBuilder();
        userObjClassBuilder.setType(type());

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
                .setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true).build());


//        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
//                ATTR_PASSWORDPROFILE + "." + ATTR_PASSWORD)
//                .setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).build());

        //optional

        //Supports $filter.
        AttributeInfoBuilder attrOnPremisesImmutableId = new AttributeInfoBuilder(ATTR_ONPREMISESIMMUTABLEID);
        attrOnPremisesImmutableId.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesImmutableId.build());

        AttributeInfoBuilder attrAboutMe = new AttributeInfoBuilder(ATTR_ABOUTME);
        attrAboutMe.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrAboutMe.build());

        AttributeInfoBuilder attrSignIn = new AttributeInfoBuilder(ATTR_SIGN_IN);
        attrSignIn.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrSignIn.build());

        //read-only - externalUserState
        userObjClassBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_EXTERNALUSERSTATE)
                .setRequired(false).setType(String.class)
                .setCreateable(false).setUpdateable(false).setReadable(true)
                .build());

        //read-only - externalUserStateChangeDateTime
        userObjClassBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_EXTERNALUSERSTATECHANGEDATETIME)
                .setRequired(false).setType(String.class)
                .setCreateable(false).setUpdateable(false).setReadable(true)
                .build());

        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_MEMBER_OF_GROUP)
                .setRequired(false).setType(String.class).setMultiValued(true)
                .setCreateable(false).setUpdateable(false).setReadable(true)
                .setReturnedByDefault(false)
                .build());

        userObjClassBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_OWNER_OF_GROUP)
                .setRequired(false).setType(String.class).setMultiValued(true)
                .setCreateable(false).setUpdateable(false).setReadable(true)
                .setReturnedByDefault(false)
                .build());

        //read-only, not nullable
        userObjClassBuilder.addAttributeInfo(new AttributeInfoBuilder(ATTR_MEMBER_OF_ROLE)
                .setRequired(false).setType(String.class).setMultiValued(true)
                .setCreateable(false).setUpdateable(false).setReadable(true)
                .setReturnedByDefault(false)
                .build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                        ATTR_ASSIGNEDLICENSES_SKUID)
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
                .setCreateable(false).setUpdateable(true).setReadable(true)
                .setReturnedByDefault(false);
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
        attrHireDate.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrHireDate.build());

        //Read-only, not nullable
        AttributeInfoBuilder attrImAddresses = new AttributeInfoBuilder(ATTR_IMADDRESSES);
        attrImAddresses.setMultiValued(true).setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrImAddresses.build());

        //multivalued
        AttributeInfoBuilder attrInterests = new AttributeInfoBuilder(ATTR_INTERESTS);
        attrInterests.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrInterests.build());

        AttributeInfoBuilder userPhoto = new AttributeInfoBuilder(ATTR_USERPHOTO);
        userPhoto.setRequired(false).setType(byte[].class).setCreateable(true).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(userPhoto.build());

        //supports $filter
        AttributeInfoBuilder attrJobTitle = new AttributeInfoBuilder(ATTR_JOBTITLE);
        attrJobTitle.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrJobTitle.build());

        //Read-Only, Supports $filter
        AttributeInfoBuilder attrMail = new AttributeInfoBuilder(ATTR_MAIL);
        attrMail.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMail.build());

        // Extension ATTRIBUTES
        for (int i = 1; i <= NUMBER_OF_EXTENSIONS; i++) {
            String attributeName = ATTR_ONPREMISESEXTENSIONATTRIBUTES + "." + EXTENSION_ATTRIBUTE + i;
            AttributeInfoBuilder attrExtensionAttribute = new AttributeInfoBuilder(attributeName);
            attrExtensionAttribute.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(false).setReturnedByDefault(false);
            userObjClassBuilder.addAttributeInfo(attrExtensionAttribute.build());
        }

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
        attrMySite.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        ;
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
        attrPastProjects.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrPastProjects.build());

        AttributeInfoBuilder attrPostalCode = new AttributeInfoBuilder(ATTR_POSTALCODE);
        attrPostalCode.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPostalCode.build());

        AttributeInfoBuilder attrPreferredLanguage = new AttributeInfoBuilder(ATTR_PREFERREDLANGUAGE);
        attrPreferredLanguage.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPreferredLanguage.build());

        AttributeInfoBuilder attrPreferredName = new AttributeInfoBuilder(ATTR_PREFERREDNAME);
        attrPreferredName.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
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
        attrResponsibilities.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrResponsibilities.build());

        //multivalued
        AttributeInfoBuilder attrSchools = new AttributeInfoBuilder(ATTR_SCHOOLS);
        attrSchools.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrSchools.build());

        //multivalued
        AttributeInfoBuilder attrSkills = new AttributeInfoBuilder(ATTR_SKILLS);
        attrSkills.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
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

        AttributeInfoBuilder attrEmployeeHireDate = new AttributeInfoBuilder(ATTR_EMPLOYEE_HIRE_DATE);
        attrEmployeeHireDate.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrEmployeeHireDate.build());

        AttributeInfoBuilder attrEmployeeLeaveDateTime = new AttributeInfoBuilder(ATTR_EMPLOYEE_LEAVE_DATE_TIME);
        attrEmployeeLeaveDateTime.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrEmployeeLeaveDateTime.build());

        AttributeInfoBuilder attrEmployeeType = new AttributeInfoBuilder(ATTR_EMPLOYEE_TYPE);
        attrEmployeeType.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrEmployeeType.build());

        AttributeInfoBuilder attrFaxNumber = new AttributeInfoBuilder(ATTR_FAX_NUMBER);
        attrFaxNumber.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrFaxNumber.build());

        AttributeInfoBuilder attrEmployeeId = new AttributeInfoBuilder(ATTR_EMPLOYEE_ID);
        attrEmployeeId.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrEmployeeId.build());

        AttributeInfoBuilder attrManager = new AttributeInfoBuilder(ATTR_MANAGER_ID);
        attrManager.setRequired(false)
                .setType(String.class)
                .setCreateable(true).setUpdateable(true).setReadable(true).setReturnedByDefault(false);
        userObjClassBuilder.addAttributeInfo(attrManager.build());


        return userObjClassBuilder.build();
    }

    public boolean isDeleteDelta(JSONObject o) {

        if (o.has(O_REMOVED)) {

            LOG.ok("Delta for processed object is {0}", SyncDeltaType.DELETE);
            return true;
        }

        LOG.ok("Delta for processed object is {0}", SyncDeltaType.CREATE_OR_UPDATE);
        return false;
    }

    private JSONArray buildLicensesJSON(Collection<Object> licenses) {
        LOG.ok("Building licence JSON");
        if (licenses == null)
            return new JSONArray();

        Map<String, List<String>> disabledPlansMap = new HashMap<>();

        String[] disabledPlansList = parseDisabledPlans(getConfiguration().getDisabledPlans());
        LOG.ok("About to construct disabled plans list");
        for (String licensePlans : disabledPlansList) {

            String a[] = licensePlans.split(":", 2);
            if (a.length != 2)
                continue;
            String skuId = a[0];
            String disabledPlans[] = a[1].split(",");
            if (disabledPlans.length >= 1) {
                if (disabledPlansMap.isEmpty()) {

                    disabledPlansMap.put(skuId, Arrays.asList(disabledPlans));
                } else {

                    if (disabledPlansMap.containsKey(skuId)) {

                        LOG.ok("Found skuID {0} among disabled plans map, fetching and augmenting", skuId);
                        ArrayList<String> plans = new ArrayList(disabledPlansMap.get(skuId));

                        for (String plan : disabledPlans) {

                            if (!plans.contains(plan)) {

                                LOG.ok("Augmenting skuID: {0}, by plan: {1}", skuId, plan);
                                plans.add(plan);
                            }
                        }

                        disabledPlansMap.put(skuId, plans);
                    }
                }
            }
        }

        JSONArray json = new JSONArray();
        licenses.forEach(it -> {
            JSONObject jo = new JSONObject();
            String skuId = (String) it;
            jo.put(ATTR_SKUID, skuId);
            if (disabledPlansMap.containsKey(skuId))
                jo.put(ATTR_DISABLEDPLANS, new JSONArray(disabledPlansMap.get(skuId)));
            json.put(jo);
        });
        return json;
    }

    private String[] parseDisabledPlans(String[] disabledPlans) {
        LOG.ok("Parsing disabled plans");
        List<String> list = new ArrayList<String>();

        for (String licensePlan : disabledPlans) {
            LOG.ok("Evaluating license plan: {0}", licensePlan);
            if (licensePlan.contains("[") && licensePlan.contains("]")) {

                String[] divPlan = StringUtils.substringsBetween(licensePlan, "[", "]");

                String potentialPlanId = divPlan[divPlan.length - 1];

                if (potentialPlanId.contains(":")) {
                    LOG.ok("Adding following plan amongst disabled plans: {0}, formatted to {1}", licensePlan
                            , potentialPlanId);

                    list.add(potentialPlanId);
                } else {

                    LOG.warn("Potentially malformed plan ID detected on input: {0}", potentialPlanId);
                }

            } else {
                LOG.ok("Adding license plan amongst parsed: {0}", licensePlan);
                list.add(licensePlan);
            }
        }

        return list.toArray(new String[0]);
    }

    private void assignLicenses(Uid uid, Attribute licenseAttribute) {
        if (licenseAttribute == null) {
            return;
        }
        assignLicenses(uid, licenseAttribute.getValue(), null);
    }

    private void assignLicenses(Uid uid, AttributeDelta licenseDelta) {
        if (licenseDelta == null) {
            return;
        }
        assignLicenses(uid, licenseDelta.getValuesToAdd(), licenseDelta.getValuesToRemove());
    }

    private void assignLicenses(Uid uid, List<Object> addLicenses, List<Object> removeLicenses) {
        if ((addLicenses == null || addLicenses.isEmpty()) && (removeLicenses == null || removeLicenses.isEmpty())) {
            return;
        }

        final GraphEndpoint endpoint = getGraphEndpoint();
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
                LOG.warn(ex, "Problem when unassigning licenses {0}, ignoring", removeLicenses);
            }
        }
        if (addLicenses != null && !addLicenses.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("addLicenses", buildLicensesJSON(addLicenses));
            jsonObject.put("removeLicenses", new JSONArray());
            endpoint.callRequestNoContent(request, null, jsonObject);
        }
    }

    private void assignManager(Uid uid, Attribute attribute) {
        if (attribute == null) {
            return;
        }
        String managerId = attribute.getValue().stream().map(Object::toString).findFirst().orElse(null);
        assignManager(uid, managerId);
    }

    private void assignManager(Uid uid, AttributeDelta attributeDelta) {
        if (attributeDelta == null) {
            return;
        }
        String managerId = attributeDelta.getValuesToReplace().stream().map(Object::toString).findFirst().orElse(null);
        assignManager(uid, managerId);
    }

    private void assignManager(Uid uid, String managerId) {
        final GraphEndpoint endpoint = getGraphEndpoint();
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS + "/" + uid.getUidValue() + MANAGER);

        if (managerId != null) {
            HttpEntityEnclosingRequestBase request = null;
            URI uri = endpoint.getUri(uriBuilder);
            request = new HttpPut(uri);

            String managerRef = API_ENDPOINT + USERS + "/" + managerId;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("@odata.id", managerRef);

            LOG.info("Assign Manager Path: {0}", uri);
            LOG.info("Assign Manager JSON: {0}", jsonObject);

            endpoint.callRequestNoContent(request, null, jsonObject);
        } else {
            HttpDelete request = null;
            URI uri = endpoint.getUri(uriBuilder);
            request = new HttpDelete(uri);

            LOG.info("Unassign Manager Path: {0}", uri);

            endpoint.callRequest(request, false);
        }
    }

    public Set<AttributeDelta> updateUser(Uid uid, Set<AttributeDelta> attrsDelta, OperationOptions options) {
        LOG.info("Start updateUser, Uid: {0}, attrsDelta: {1}", uid, attrsDelta);
        final GraphEndpoint endpoint = getGraphEndpoint();

        AttributeDelta assignedLicensesDelta = null;
        AttributeDelta managerIdDelta = null;
        AttributeDelta photoDelta = null;
        List<String> oldSelectors = new ArrayList<>();
        for (AttributeDelta delta : attrsDelta) {
            if (UPDATABLE_MULTIPLE_VALUE_ATTRS_OF_USER.contains(delta.getName())) {
                oldSelectors.add(delta.getName());
                continue;
            }
            switch (delta.getName()) {
                case ATTR_ASSIGNEDLICENSES_SKUID:
                    assignedLicensesDelta = delta;
                    break;
                case ATTR_MANAGER_ID:
                    managerIdDelta = delta;
                    break;
                case ATTR_USERPHOTO:
                    photoDelta = delta;
                    break;
            }
        }

        // When updating multiple value of the user resource, we need to fetch the current JSON array and merge it with requested delta
        // since Microsoft Graph API doesn't provide a way to patch the JSON array
        JSONObject oldJson = null;
        if (!oldSelectors.isEmpty()) {
            final String select = "$select=" + String.join(",", oldSelectors);

            oldJson = endpoint.executeGetRequest(USERS + "/" + uid.getUidValue() + "/", select, options);

            // Remove unrelated keys
            for (String key : oldJson.keySet()) {
                if (!UPDATABLE_MULTIPLE_VALUE_ATTRS_OF_USER.contains(key)) {
                    oldJson.remove(key);
                }
            }
        }

        // Update user resource
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS + "/" + uid.getUidValue());
        URI uri = endpoint.getUri(uriBuilder);
        LOG.info("update user, PATCH");
        LOG.info("Path: {0}", uri);
        HttpEntityEnclosingRequestBase request = new HttpPatch(uri);
        List<JSONObject> jsonObjectAccount = buildLayeredAttribute(oldJson, attrsDelta, EXCLUDE_ATTRS_OF_USER, SPO_ATTRS);
        endpoint.callRequestNoContentNoJson(request, jsonObjectAccount);

        // Update other resources if necessary
        assignLicenses(uid, assignedLicensesDelta);
        assignManager(uid, managerIdDelta);
        assignPhoto(uid, photoDelta);

        return null;
    }

    private void assignPhoto(Uid uid, Attribute attribute) {
        if (attribute == null) {
            return;
        }
        byte[] photoData = (byte[]) attribute.getValue().get(0);
        assignPhoto(uid, photoData);
    }

    private void assignPhoto(Uid uid, AttributeDelta attributeDelta) {
        if (attributeDelta == null) {
            return;
        }
        byte[] photoData = (byte[]) attributeDelta.getValuesToReplace().stream().findFirst().orElse(null);
        assignPhoto(uid, photoData);
    }

    private void assignPhoto(Uid uid, byte[] photoData) {
        final GraphEndpoint endpoint = getGraphEndpoint();
        final URIBuilder uriBuilder = endpoint.createURIBuilder()
                .setPath(USERS + "/" + uid.getUidValue() + "/" + ATTR_USERPHOTO + "/$value");
        URI uri = endpoint.getUri(uriBuilder);

        if (photoData != null) {
            HttpEntityEnclosingRequestBase request = new HttpPut(uri);
            try {
                request.setHeader("Content-Type", "image/jpeg");
                request.setEntity(new ByteArrayEntity(photoData));
                endpoint.callRequest(request, false);
            } catch (IllegalArgumentException e) {
                LOG.error("Invalid Base64 encoded photo data");
            }
        } else {
            // Microsoft Graph API doesn't support photo deletion
            // Reference: https://learn.microsoft.com/en-us/graph/api/profilephoto-update?view=graph-rest-1.0&tabs=http
            LOG.warn("Photo deletion was ignored because Microsoft Graph API does not support it. uid: {0}", uid);
        }
    }

    public Uid createUser(Set<Attribute> attributes) {
        LOG.info("Start createUser, attributes: {0}", attributes);
        final GraphEndpoint endpoint = getGraphEndpoint();

        String mail = null;
        String upn = null;
        String displayName = null;
        String userType = null;
        Attribute managerId = null;
        Attribute assignedLicenses = null;
        Attribute photo = null;
        for (Attribute attribute : attributes) {
            switch (attribute.getName()) {
                case ATTR_MAIL:
                    mail = AttributeUtil.getStringValue(attribute);
                    break;
                case ATTR_USERPRINCIPALNAME:
                    upn = AttributeUtil.getStringValue(attribute);
                    break;
                case ATTR_DISPLAYNAME:
                    displayName = AttributeUtil.getStringValue(attribute);
                    break;
                case ATTR_USERTYPE:
                    userType = AttributeUtil.getStringValue(attribute);
                    break;
                case ATTR_MANAGER_ID:
                    managerId = attribute;
                    break;
                case ATTR_ASSIGNEDLICENSES_SKUID:
                    assignedLicenses = attribute;
                    break;
                case ATTR_USERPHOTO:
                    photo = attribute;
                    break;
            }
        }

        final boolean hasUPN = upn != null;
        final boolean invite = mail != null &&
                !mail.split("@")[1].equals(getConfiguration().getTenantId()) &&
                getConfiguration().isInviteGuests() &&
                !hasUPN;

        final Uid newUid;
        if (invite) {
            AttributesValidator.builder()
                    .withNonEmpty()
                    .withExactlyOne(ATTR_MAIL)
                    .build()
                    .validate(attributes);

            final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(INVITATIONS);
            final URI uri = endpoint.getUri(uriBuilder);
            final HttpEntityEnclosingRequestBase request = new HttpPost(uri);
            final JSONObject payload = buildInvitation(displayName, mail, userType);
            final JSONObject jsonRequest = endpoint.callRequest(request, payload, true);
            newUid = new Uid(jsonRequest.getJSONObject(ATTR_INVITED_USER).getString(ATTR_ID));
        } else {
            AttributesValidator.builder()
                    .withNonEmpty(ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME, ATTR_ICF_PASSWORD)
                    .withExactlyOne(ATTR_USERPRINCIPALNAME)
                    .withRegex(ATTR_USERPRINCIPALNAME, "[^@]+@[^@]+")
                    .build()
                    .validate(attributes);

            final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS);
            final URI uri = endpoint.getUri(uriBuilder);
            final HttpEntityEnclosingRequestBase request = new HttpPost(uri);
            final JSONObject payload = buildLayeredAttributeJSON(attributes, EXCLUDE_ATTRS_OF_USER);
            final JSONObject jsonRequest = endpoint.callRequest(request, payload, true);
            newUid = new Uid(jsonRequest.getString(ATTR_ID));
        }

        assignLicenses(newUid, assignedLicenses);
        assignManager(newUid, managerId);
        assignPhoto(newUid, photo);

        return newUid;
    }

    public void delete(Uid uid) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }
        HttpDelete request;

        final GraphEndpoint endpoint = getGraphEndpoint();
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(USERS + "/" + uid.getUidValue());
        URI uri = endpoint.getUri(uriBuilder);
        LOG.info("Delete: {0}", uri);
        request = new HttpDelete(uri);
        if (endpoint.callRequest(request, false) == null) {
            LOG.info("Deleted user with Uid {0}", uid.getUidValue());
        }
    }

    // TODO: refactor rewrite to method executeQueryForUser
    public void executeQueryForUser(ResourceQuery translatedQuery,
                                    Boolean fetchSpecific,
                                    ResultsHandler handler,
                                    OperationOptions options) {
        LOG.info("executeQueryForUser()");
        final GraphEndpoint endpoint = getGraphEndpoint();
        final String selectorSingle = getSelectorSingle(options);

        final String selectorList = selector(
                ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME,
                ATTR_ONPREMISESIMMUTABLEID, ATTR_MAILNICKNAME, ATTR_USERPRINCIPALNAME,
                ATTR_BUSINESSPHONES, ATTR_CITY, ATTR_COMPANYNAME, ATTR_COUNTRY, ATTR_DEPARTMENT,
                ATTR_GIVENNAME, ATTR_IMADDRESSES, ATTR_ID,
                ATTR_JOBTITLE, ATTR_MAIL, ATTR_MOBILEPHONE, ATTR_OFFICELOCATION,
                ATTR_ONPREMISESLASTSYNCDATETIME, ATTR_ONPREMISESSECURITYIDENTIFIER,
                ATTR_ONPREMISESSYNCENABLED, ATTR_PASSWORDPOLICIES,
                ATTR_POSTALCODE, ATTR_PREFERREDLANGUAGE,
                ATTR_PROXYADDRESSES,
                ATTR_STATE, ATTR_STREETADDRESS, ATTR_SURNAME,
                ATTR_USAGELOCATION, ATTR_USERTYPE, ATTR_ASSIGNEDLICENSES,
                ATTR_EXTERNALUSERSTATE, ATTR_EXTERNALUSERSTATECHANGEDATETIME, ATTR_MANAGER,
                ATTR_ONPREMISESEXTENSIONATTRIBUTES
        );

        String query = null;
        Boolean fetchAll = false;

        if (translatedQuery != null) {

            query = translatedQuery.toString();

            if (query != null && !query.isEmpty()) {

            } else {

                if (translatedQuery.hasIdOrMembershipExpression()) {
                    query = translatedQuery.getIdOrMembershipExpression();
                } else {

                    fetchAll = true;
                }

            }

        } else {

            fetchAll = true;

        }

        if (!fetchAll) {

            if (fetchSpecific) {

                LOG.info("Fetching account info for account: {0}", query);
                StringBuilder sbPath = new StringBuilder();
                sbPath.append(toGetURLByUserPrincipalName(query)).append("/");
                String filter = "";

                Set<String> attributesToGet = getAttributesToGet(options);
                if (attributesToGet.contains(ATTR_MANAGER_ID)) {

                    LOG.info("Fetching manager info for account: {0}", query);

                    filter = "$" + EXPAND + "=" + ATTR_MANAGER;
                }
                LOG.ok("The constructed additional filter clause: {0}", filter.isEmpty() ? "Empty filter clause." : filter);

                //not included : ATTR_PASSWORDPROFILE,
                // ATTR_MAILBOXSETTINGS,ATTR_PROVISIONEDPLANS

                //TODO
                JSONObject user = endpoint.executeGetRequest(sbPath.toString(), selectorSingle + "&" +
                        filter, options);

                if (attributesToGet.contains(ATTR_SIGN_IN)) {
                    LOG.info("Fetching sing-in info for account: {0}", query);
                    sbPath = new StringBuilder()
                            .append("/auditLogs/signIns");

                    // /auditLogs/signIns doesn't support $select
                    // https://learn.microsoft.com/en-us/graph/api/signin-list?view=graph-rest-1.0&tabs=http#optional-query-parameters
                    StringBuilder signInSelector = new StringBuilder()
                            .append("$top=1&$filter=").append("userId").append(" eq ").append("'" + query + "'");

                    LOG.ok("Sign-in info query with path: {0} and filter {1}", sbPath, signInSelector);
                    // Use "paging = false" with customQuery contains "$top=1" here since we need the last sign object only
                    endpoint.executeListRequest(sbPath.toString(), signInSelector.toString(), options, false, (opt, signIn) -> {
                        // First object in the json array is the last sign in
                        if (!signIn.isNull("createdDateTime")) {
                            String lastSignInTime = signIn.getString("createdDateTime");
                            user.put(ATTR_SIGN_IN, lastSignInTime);
                        }

                        LOG.ok("The last sign in: {0}", signIn);
                        return false;
                    });
                }

                LOG.ok("The retrieved JSONObject for the account {0}: {1}", query, user);
                handleJSONObject(options, user, handler);

            } else {

                // TODO significance ?
                //(Arrays.asList(ATTR_DISPLAYNAME, ATTR_GIVENNAME, ATTR_JOBTITLE)
                //(Arrays.asList(ATTR_JOBTITLE, ATTR_GIVENNAME, ATTR_USERPRINCIPALNAME, ATTR_DISPLAYNAME)

                // final String filter = "$filter=" + translatedQuery;
                LOG.ok("The constructed filter: {0}", query);
                endpoint.executeListRequest(USERS, selectorList + '&' + query, options, true, createJSONObjectHandler(handler));
            }

        } else {
            LOG.info("Empty query, returning full list of objects for the {0} object class", ObjectClass.ACCOUNT_NAME);

            endpoint.executeListRequest(USERS, selectorList, options, true, createJSONObjectHandler(handler));
        }
    }

    protected Set<String> getAttributesToGet(OperationOptions options) {
        if (options == null || options.getAttributesToGet() == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(Arrays.asList(options.getAttributesToGet()));
    }

    /**
     * When the userPrincipalName begins with a $ character, remove the slash (/) after /users and
     * enclose the userPrincipalName in parentheses and single quotes.
     * See https://docs.microsoft.com/en-us/graph/api/user-get?view=graph-rest-1.0&tabs=http
     *
     * @param userPrincipalName
     * @return
     */
    private String toGetURLByUserPrincipalName(String userPrincipalName) {
        StringBuilder sbPath = new StringBuilder();
        sbPath.append(USERS);

        userPrincipalName.replace("#", "%23"); // Escape

        if (userPrincipalName.startsWith("$")) {
            sbPath.append("('");
            sbPath.append(userPrincipalName);
            sbPath.append("')");
            return sbPath.toString();
        }

        sbPath.append("/");
        sbPath.append(userPrincipalName);
        return sbPath.toString();
    }

    @Override
    protected boolean handleJSONObject(OperationOptions options, JSONObject user, ResultsHandler handler) {
        LOG.ok("processingObjectFromGET (Object)");
        if (shouldSaturate(options, ObjectClass.ACCOUNT_NAME, ATTR_MEMBER_OF_GROUP)) {
            user = saturateGroupMembership(user);
        }

        if (shouldSaturate(options, ObjectClass.ACCOUNT_NAME, ATTR_OWNER_OF_GROUP)) {
            user = saturateGroupOwnership(user);
        }

        if (shouldSaturate(options, ObjectClass.ACCOUNT_NAME, ATTR_MEMBER_OF_ROLE)) {
            user = saturateRoleMembership(user);
        }

        if (shouldSaturate(options, ObjectClass.ACCOUNT_NAME, ATTR_USERPHOTO)) {
            user = saturatePhoto(options, user);
        }

        ConnectorObjectBuilder builder = convertUserJSONObjectToConnectorObject(user);

        incompleteIfNecessary(options, ObjectClass.ACCOUNT_NAME, ATTR_MEMBER_OF_GROUP, builder);
        incompleteIfNecessary(options, ObjectClass.ACCOUNT_NAME, ATTR_OWNER_OF_GROUP, builder);
        incompleteIfNecessary(options, ObjectClass.ACCOUNT_NAME, ATTR_MEMBER_OF_ROLE, builder);

        ConnectorObject connectorObject = builder.build();
        LOG.info("convertUserToConnectorObject, user: {0}, \n\tconnectorObject: {1}", user.get("id"), connectorObject);
        return handler.handle(connectorObject);
    }

    public ConnectorObjectBuilder evaluateAndFetchAttributesToGet(Uid uid,
                                                                  OperationOptions oo){

        Set<String> attributesToGet = getAttributesToGet(oo);
        String query = uid.getUidValue();
        String filter = "";

        final GraphEndpoint endpoint = getGraphEndpoint();
        final String selectorSingle = getSelectorSingle(oo);

        if (attributesToGet.contains(ATTR_MANAGER_ID)) {

            LOG.info("Fetching manager info for account: {0}", query);

            filter = "$" + EXPAND + "=" + ATTR_MANAGER;
        }

        JSONObject user = endpoint.executeGetRequest(toGetURLByUserPrincipalName(query)+"/",
                selectorSingle + "&" + filter, oo);

        return  convertUserJSONObjectToConnectorObject(user);
    }


    private JSONObject buildInvitation(String displayName, String mail, String userType) {
        final JSONObject invitation = new JSONObject()
                .put(ATTR_INVITE_SEND_MESSAGE, getConfiguration().isSendInviteMail())
                .put(ATTR_INVITE_MSG_INFO, getConfiguration().getInviteMessage())
                .put(ATTR_INVITE_REDIRECT, getConfiguration().getInviteRedirectUrl());

        if (displayName != null) invitation.put(ATTR_INVITE_DISPNAME, displayName);
        if (mail != null) invitation.put(ATTR_INVITED_USER_EMAIL, mail);
        if (userType != null) invitation.put(ATTR_INVITED_USER_TYPE, userType);

        return invitation;
    }

    private JSONObject saturateGroupMembership(JSONObject user) {
        final String uid = user.getString(ATTR_ID);
        final List<String> groups = getGraphEndpoint().executeListRequest(
                        String.format("/users/%s/memberOf", uid), "$select=id", null, true)
                .toList().stream()
                .filter(o -> TYPE_GROUP.equals(((Map) o).get(TYPE)))
                .map(o -> (String) ((Map) o).get(ATTR_ID))
                .collect(Collectors.toList());
        user.put(ATTR_MEMBER_OF_GROUP, new JSONArray(groups));
        return user;
    }

    // Saturate group ownership function
    private JSONObject saturateGroupOwnership(JSONObject user) {
        final String uid = user.getString(ATTR_ID);
        final List<String> groups = getGraphEndpoint().executeListRequest(
                        String.format("/users/%s/ownedObjects", uid), "$select=id", null, true)
                .toList().stream()
                .filter(o -> TYPE_GROUP.equals(((Map) o).get(TYPE)))
                .map(o -> (String) ((Map) o).get(ATTR_ID))
                .collect(Collectors.toList());
        user.put(ATTR_OWNER_OF_GROUP, new JSONArray(groups));
        return user;
    }

    private JSONObject saturateRoleMembership(JSONObject user) {
        final GraphEndpoint endpoint = getGraphEndpoint();
        final String uid = user.getString(ATTR_ID);

        LOG.info("[GET] - saturateRoleMembership(), for user with UID: {0}", uid);

        final String customQuery = "$select=roleDefinitionId&$filter=principalId eq '" + uid + "'";
        final JSONArray userMembership = endpoint.executeListRequest(ROLE_ASSIGNMENT, customQuery, null, true);
        user.put(ATTR_MEMBER_OF_ROLE, getJSONArray(userMembership, "roleDefinitionId"));
        return user;
    }

    private JSONObject saturatePhoto(OperationOptions options, JSONObject user) {
        final GraphEndpoint endpoint = getGraphEndpoint();
        final String uid = user.getString(ATTR_ID);

        if (getSchemaTranslator().containsToGet(ObjectClass.ACCOUNT_NAME, options, ATTR_USERPHOTO)) {
            LOG.info("[GET] - /photo/$value, for user with UID: {0}", uid);
            String photoPath = USERS + "/" + uid + "/" + ATTR_USERPHOTO + "/$value";
            final JSONObject userPhoto = endpoint.executeGetRequest(photoPath, null, options);
            if (userPhoto.length() != 0)
                user.put(ATTR_USERPHOTO, userPhoto.get("data"));
        }
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
        getMultiIfExists(user, ATTR_OWNER_OF_GROUP, builder);
        getMultiIfExists(user, ATTR_MEMBER_OF_ROLE, builder);
        getIfExists(user, ATTR_BIRTHDAY, String.class, builder);
        getIfExists(user, ATTR_CITY, String.class, builder);
        getIfExists(user, ATTR_COMPANYNAME, String.class, builder);
        getIfExists(user, ATTR_COUNTRY, String.class, builder);
        getIfExists(user, ATTR_DEPARTMENT, String.class, builder);
        getIfExists(user, ATTR_GIVENNAME, String.class, builder);
        getIfExists(user, ATTR_HIREDATE, String.class, builder);
        getMultiIfExists(user, ATTR_IMADDRESSES, builder);
        getIfExists(user, ATTR_ID, String.class, builder);
        getMultiIfExists(user, ATTR_BUSINESSPHONES, builder);
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
        getIfExists(user, ATTR_SIGN_IN, String.class, builder);
        getIfExists(user, ATTR_EXTERNALUSERSTATE, String.class, builder);
        getIfExists(user, ATTR_EXTERNALUSERSTATECHANGEDATETIME, String.class, builder);
        getIfExists(user, ATTR_EMPLOYEE_HIRE_DATE, String.class, builder);
        getIfExists(user, ATTR_EMPLOYEE_LEAVE_DATE_TIME, String.class, builder);
        getIfExists(user, ATTR_EMPLOYEE_TYPE, String.class, builder);
        getIfExists(user, ATTR_FAX_NUMBER, String.class, builder);
        getIfExists(user, ATTR_EMPLOYEE_ID, String.class, builder);
        getIfExists(user, ATTR_USERPHOTO, byte[].class, builder);

        for (int i = 1; i <= NUMBER_OF_EXTENSIONS; i++) {
            getFromItemIfExists(user, ATTR_ONPREMISESEXTENSIONATTRIBUTES, EXTENSION_ATTRIBUTE + i, String.class, builder);
        }

        getMultiIfExists(user, ATTR_PROXYADDRESSES, builder);
        getFromArrayIfExists(user, ATTR_ASSIGNEDLICENSES, ATTR_SKUID, String.class, builder);
        getJSONObjectItemIfExists(user, ATTR_MANAGER, ATTR_ID, String.class, builder);
        return builder;
    }

    public ConnectorObjectBuilder enhanceConnectorObjectWithDeltaItems(JSONObject user,
                                                                       ConnectorObjectBuilder builder) {
        LOG.info("Evaluating Account delta items conversion.");

        getFromArrayIfExists(user, ATTR_MANAGER, ATTR_ID, O_REMOVED, String.class, builder, true);
        return builder;
    }

    protected String getUIDIfExists(JSONObject object) {
        if (object.has(ATTR_ID)) {
            String uid = object.getString(ATTR_ID);
            return uid;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing required attribute: ").append(ATTR_ID)
                    .append(" for converting JSONObject to ConnectorObject.");
            throw new InvalidAttributeValueException(sb.toString());
        }
    }

    public String getSelectorSingle(OperationOptions options) {

        if (options != null) {

            return selector(getSchemaTranslator().filter(ObjectClass.ACCOUNT_NAME, options,
                    ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME,
                    ATTR_ONPREMISESIMMUTABLEID, ATTR_MAILNICKNAME, ATTR_USERPRINCIPALNAME, ATTR_ABOUTME,
                    ATTR_BIRTHDAY, ATTR_BUSINESSPHONES, ATTR_CITY, ATTR_COMPANYNAME, ATTR_COUNTRY, ATTR_DEPARTMENT,
                    ATTR_GIVENNAME, ATTR_HIREDATE, ATTR_IMADDRESSES, ATTR_ID, ATTR_INTERESTS,
                    ATTR_JOBTITLE, ATTR_MAIL, ATTR_MOBILEPHONE, ATTR_MYSITE, ATTR_OFFICELOCATION,
                    ATTR_ONPREMISESLASTSYNCDATETIME, ATTR_ONPREMISESSECURITYIDENTIFIER,
                    ATTR_ONPREMISESSYNCENABLED, ATTR_PASSWORDPOLICIES, ATTR_PASTPROJECTS,
                    ATTR_POSTALCODE, ATTR_PREFERREDLANGUAGE, ATTR_PREFERREDNAME,
                    ATTR_PROXYADDRESSES, ATTR_RESPONSIBILITIES, ATTR_SCHOOLS,
                    ATTR_SKILLS, ATTR_STATE, ATTR_STREETADDRESS, ATTR_SURNAME,
                    ATTR_USAGELOCATION, ATTR_USERTYPE, ATTR_ASSIGNEDLICENSES,
                    ATTR_EXTERNALUSERSTATE, ATTR_EXTERNALUSERSTATECHANGEDATETIME, ATTR_MANAGER,
                    ATTR_EMPLOYEE_HIRE_DATE, ATTR_EMPLOYEE_LEAVE_DATE_TIME, ATTR_EMPLOYEE_TYPE,
                    ATTR_FAX_NUMBER, ATTR_EMPLOYEE_ID, ATTR_ONPREMISESEXTENSIONATTRIBUTES
            ));
        } else {

            return selector(
                    ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME,
                    ATTR_ONPREMISESIMMUTABLEID, ATTR_MAILNICKNAME, ATTR_USERPRINCIPALNAME,
                    ATTR_BUSINESSPHONES, ATTR_CITY, ATTR_COMPANYNAME, ATTR_COUNTRY, ATTR_DEPARTMENT,
                    ATTR_GIVENNAME, ATTR_IMADDRESSES, ATTR_ID,
                    ATTR_JOBTITLE, ATTR_MAIL, ATTR_MOBILEPHONE, ATTR_OFFICELOCATION,
                    ATTR_ONPREMISESLASTSYNCDATETIME, ATTR_ONPREMISESSECURITYIDENTIFIER,
                    ATTR_ONPREMISESSYNCENABLED, ATTR_PASSWORDPOLICIES,
                    ATTR_POSTALCODE, ATTR_PREFERREDLANGUAGE,
                    ATTR_PROXYADDRESSES, ATTR_STATE, ATTR_STREETADDRESS, ATTR_SURNAME,
                    ATTR_USAGELOCATION, ATTR_USERTYPE, ATTR_ASSIGNEDLICENSES,
                    ATTR_EXTERNALUSERSTATE, ATTR_EXTERNALUSERSTATECHANGEDATETIME, ATTR_MANAGER,
                    ATTR_EMPLOYEE_HIRE_DATE, ATTR_EMPLOYEE_LEAVE_DATE_TIME, ATTR_EMPLOYEE_TYPE,
                    ATTR_FAX_NUMBER, ATTR_EMPLOYEE_ID, ATTR_ONPREMISESEXTENSIONATTRIBUTES
            );
        }

    }

    public boolean isNamePresent(JSONObject object) {
        if (object.has(ATTR_USERPRINCIPALNAME)) {

            LOG.ok("Naming attribute present for the currently processed object");

            return true;
        }

        String objectId = getUIDIfExists(object);

        LOG.warn("Naming attribute not present for the currently processed object with the Id {0}. Most probably " +
                "object already deleted, yet it might indicate potential consistency issues with the currently " +
                "processed object.", objectId);

        return false;
    }

    public String getNameAttribute() {

        return ATTR_USERPRINCIPALNAME;
    }

    public String getUIDAttribute() {

        return ATTR_ID;
    }

    public Set<String> getObjectDeltaItems() {

        return new HashSet<>(Arrays.asList(ATTR_MANAGER+O_DELTA));
    }
}
