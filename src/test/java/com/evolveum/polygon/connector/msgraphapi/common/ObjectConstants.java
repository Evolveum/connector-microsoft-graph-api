package com.evolveum.polygon.connector.msgraphapi.common;

public interface ObjectConstants {
    String ATTR_ACCOUNTENABLED = "accountEnabled";
    String ATTR_DISPLAYNAME = "displayName";
    String ATTR_ONPREMISESIMMUTABLEID = "onPremisesImmutableId";
    String ATTR_MAILNICKNAME = "mailNickname";

    String ATTR_PASSWORDPROFILE = "passwordProfile";
    String ATTR_FORCECHANGEPASSWORDNEXTSIGNIN = "forceChangePasswordNextSignIn";

    String ATTR_USERPRINCIPALNAME = "userPrincipalName";
    String ATTR_MEMBER_OF_GROUP = "memberOfGroup";

    String ATTR_ABOUTME = "aboutMe"; // Need SPO license


    String ATTR_SIGN_IN = "signIn";

    //ASSIGNEDLICENSES
    String ATTR_ASSIGNEDLICENSES = "assignedLicenses";
    String ATTR_SKUID = "skuId";
    String ATTR_DISABLEDPLANS = "disabledPlans";
    String ATTR_ASSIGNEDLICENSES__SKUID = ATTR_ASSIGNEDLICENSES + "." + ATTR_SKUID;

    //ASSIGNEDPLAN
    String ATTR_ASSIGNEDPLANS = "assignedPlans";
    String ATTR_ASSIGNEDDATETIME = "assignedDateTime";
    String ATTR_CAPABILITYSTATUS = "capabilityStatus";
    String ATTR_SERVICE = "service";
    String ATTR_SERVICEPLANID = "servicePlanId";

    String ATTR_BIRTHDAY = "birthday"; // Need SPO license
    String ATTR_BUSINESSPHONES = "businessPhones";
    String ATTR_CITY = "city";
    String ATTR_COMPANYNAME = "companyName";
    String ATTR_COUNTRY = "country";
    String ATTR_DEPARTMENT = "department";
    String ATTR_GIVENNAME = "givenName";
    String ATTR_HIREDATE = "hireDate"; // Need SPO license
    String ATTR_ID = "id";
    String ATTR_IMADDRESSES = "imAddresses";
    String ATTR_INTERESTS = "interests"; // Need SPO license
    String ATTR_JOBTITLE = "jobTitle";
    String ATTR_MAIL = "mail";

    //MAILBOXSETTINGS
    String ATTR_MAILBOXSETTINGS = "mailboxSettings";
    String ATTR_AUTOMATICREPLIESSETTING = "automaticRepliesSetting";
    String ATTR_EXTERNALAUDIENCE = "externalAudience";
    String ATTR_EXTERNALREPLYMESSAGE = "externalReplyMessage";
    String ATTR_INTERNALREPLYMESSAGE = "internalReplyMessage";
    String ATTR_SCHEDULEDENDDATETIME = "scheduledEndDateTime";
    String ATTR_SCHEDULEDSTARTDATETIME = "scheduledStartDateTime";
    String ATTR_DATETIME = "DateTime";
    String ATTR_TIMEZONE = "TimeZone";
    String ATTR_STATUS = "status";
    String ATTR_LANGUAGE = "language";
    String ATTR_LOCALE = "locale";


    String ATTR_MOBILEPHONE = "mobilePhone";
    String ATTR_MYSITE = "mySite"; // Need SPO license
    String ATTR_OFFICELOCATION = "officeLocation";
    String ATTR_ONPREMISESLASTSYNCDATETIME = "onPremisesLastSyncDateTime";
    String ATTR_ONPREMISESSECURITYIDENTIFIER = "onPremisesSecurityIdentifier";
    String ATTR_ONPREMISESSYNCENABLED = "onPremisesSyncEnabled";
    String ATTR_PASSWORDPOLICIES = "passwordPolicies";
    String ATTR_PASTPROJECTS = "pastProjects"; // Need SPO license
    String ATTR_POSTALCODE = "postalCode";
    String ATTR_PREFERREDLANGUAGE = "preferredLanguage";
    String ATTR_PREFERREDNAME = "preferredName"; // Need SPO license

    //provisionplans
    String ATTR_PROVISIONEDPLANS = "provisionedPlans";
    String ATTR_PROVISIONINGSTATUS = "provisioningStatus";

    String ATTR_PROXYADDRESSES = "proxyAddresses";
    String ATTR_RESPONSIBILITIES = "responsibilities"; // Need SPO license
    String ATTR_SCHOOLS = "schools"; // Need SPO license
    String ATTR_SKILLS = "skills"; // Need SPO license
    String ATTR_STATE = "state";
    String ATTR_STREETADDRESS = "streetAddress";
    String ATTR_SURNAME = "surname";
    String ATTR_USAGELOCATION = "usageLocation";
    String ATTR_USERTYPE = "userType";

    // INVITES
    String ATTR_INVITED_USER = "invitedUser";
    String ATTR_INVITED_USER_EMAIL = "invitedUserEmailAddress";
    String ATTR_INVITE_REDIRECT = "inviteRedirectUrl";
    String ATTR_INVITE_DISPNAME = "invitedUserDisplayName";
    String ATTR_INVITE_SEND_MESSAGE = "sendInvitationMessage";
    String ATTR_INVITE_MSG_INFO = "invitedUserMessageInfo";
    String ATTR_INVITED_USER_TYPE = "invitedUserType";

    String ATTR_ICF_PASSWORD = "__PASSWORD__";
    String ATTR_ICF_ENABLED = "__ENABLE__";
}
