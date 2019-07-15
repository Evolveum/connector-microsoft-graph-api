package com.evolveum.polygon.connector.msconnector.rest;

import com.evolveum.polygon.common.GuardedStringAccessor;
import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.*;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequest;
import com.microsoft.graph.requests.extensions.IUserCollectionPage;
import com.microsoft.graph.requests.extensions.IUserCollectionRequest;
import com.microsoft.graph.requests.extensions.IUserCollectionRequestBuilder;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class UserProcessing extends ObjectProcessing {

    //By default, only a limited set of properties are returned, use select to return more properties
    private static final String SELECT = "id,userPrincipalName,accountEnabled,ageGroup,assignedLicenses," +
            "assignedPlans,businessPhones,city,companyName,consentProvidedForMinor,country,department,displayName," +
            "employeeId,faxNumber,givenName,imAddresses,jobTitle,legalAgeGroupClassification,mail,mailNickname," +
            "mobilePhone,officeLocation,onPremisesImmutableId,onPremisesLastSyncDateTime,onPremisesSamAccountName," +
            "onPremisesSecurityIdentifier,onPremisesSyncEnabled,onPremisesUserPrincipalName,otherMails," +
            "passwordPolicies,passwordProfile,postalCode,preferredLanguage,provisionedPlans,proxyAddresses," +
            "showInAddressList,state,streetAddress,surname,usageLocation,userType";

    private static final String STARTSWITH = "startswith";

    //required
    private static final String ACCOUNTENABLED = "accountEnabled";
    private static final String DISPLAYNAME = "displayName";
    private static final String ONPREMISESIMMUTABLEID = "onPremisesImmutableId";
    private static final String MAILNICKNAME = "mailNickname";

    //passwordprofile
    private static final String PASSWORDPROFILE = "passwordProfile";
    private static final String FORCECHANGEPASSWORDNEXTSIGNINWITHMFA = "forceChangePasswordNextSignInWithMfa";
    private static final String USERPRINCIPALNAME = "userPrincipalName";


    //optional
    private static final String AGEGROUP = "ageGroup";

    //ASSIGNEDLICENSES
    private static final String ASSIGNEDLICENSES = "assignedLicenses";
    private static final String SKUID = "skuId";
    private static final String DISABLEDPLANS = "disabledPlans";

    //ASSIGNEDPLAN
    private static final String ASSIGNEDPLANS = "assignedPlans";
    private static final String ASSIGNEDDATETIME = "assignedDateTime";
    private static final String CAPABILITYSTATUS = "capabilityStatus";
    private static final String SERVICE = "service";
    private static final String SERVICEPLANID = "servicePlanId";

    private static final String BUSINESSPHONES = "businessPhones";
    private static final String CITY = "city";
    private static final String COMPANYNAME = "companyName";
    private static final String COUNTRY = "country";
    private static final String CONSENTPROVIDEDFORMINOR = "consentProvidedForMinor";
    private static final String CREATEDDATETIME = "createdDateTime";
    private static final String DEPARTMENT = "department";
    private static final String EMPLOYEEID = "employeeId";
    private static final String FAXNUMBER = "faxNumber";
    private static final String GIVENNAME = "givenName";
    private static final String ID = "id";
    private static final String IMADDRESSES = "imAddresses";
    private static final String JOBTITLE = "jobTitle";
    private static final String LEGALAGEGROUPCLASSIFICATION = "legalAgeGroupClassification";
    private static final String MAIL = "mail";


    private static final String MOBILEPHONE = "mobilePhone";
    private static final String OFFICELOCATION = "officeLocation";

    private static final String ONPREMISESLASTSYNCDATETIME = "onPremisesLastSyncDateTime";
    private static final String ONPREMISESSAMACCOUNTNAME = "onPremisesSamAccountName";
    private static final String ONPREMISESSECURITYIDENTIFIER = "onPremisesSecurityIdentifier";
    private static final String ONPREMISESSYNCENABLED = "onPremisesSyncEnabled";
    private static final String ONPREMISESUSERPRINCIPALNAME = "onPremisesUserPrincipalName";
    private static final String OTHERMAILS = "otherMails";
    private static final String PASSWORDPOLICIES = "passwordPolicies";
    private static final String POSTALCODE = "postalCode";


    private static final String PREFERREDLANGUAGE = "preferredLanguage";

    private static final String PROVISIONEDPLANS = "provisionedPlans";
    private static final String PROVISIONINGSTATUS = "provisioningStatus";

    private static final String PROXYADDRESSES = "proxyAddresses";
    private static final String SHOWINADDRESSLIST = "showInAddressList";
    private static final String STATE = "state";
    private static final String STREETADDRESS = "streetAddress";
    private static final String SURNAME = "surname";
    private static final String USAGELOCATION = "usageLocation";
    private static final String USERTYPE = "userType";


    private static final String USERDIRECTORYOBJECTMEMBER = "userDirectoryObjectMember";
    private static final String USERDIRECTORYOBJECTOWNER = "userDirectoryObjectOwner";


    private Uid userUid;
    private boolean failure;
    private ClientException clientException;
    private CountDownLatch loginLatch;



    public UserProcessing(MSGraphConfiguration configuration) {
        super(configuration);
    }


    public void buildUserObjectClass(SchemaBuilder schemaBuilder) {

        ObjectClassInfoBuilder userObjClassBuilder = new ObjectClassInfoBuilder();
        userObjClassBuilder.setType(ObjectClass.ACCOUNT_NAME);


        AttributeInfoBuilder attrDisplayName = new AttributeInfoBuilder(DISPLAYNAME);
        attrDisplayName.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrDisplayName.build());

        AttributeInfoBuilder attrMailNickname = new AttributeInfoBuilder(MAILNICKNAME);
        attrMailNickname.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMailNickname.build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                PASSWORDPROFILE + "." + FORCECHANGEPASSWORDNEXTSIGNINWITHMFA)
                .setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true).build());


        userObjClassBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);

        userObjClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);

        userObjClassBuilder.addAttributeInfo(OperationalAttributeInfos.FORCE_PASSWORD_CHANGE);

        AttributeInfoBuilder attrUserPrincipalName = new AttributeInfoBuilder(USERPRINCIPALNAME);
        attrUserPrincipalName.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrUserPrincipalName.build());

        AttributeInfoBuilder attrOnPremisesImmutableId = new AttributeInfoBuilder(ONPREMISESIMMUTABLEID);
        attrOnPremisesImmutableId.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesImmutableId.build());


        AttributeInfoBuilder attrAgeGroup = new AttributeInfoBuilder(AGEGROUP);
        attrAgeGroup.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrAgeGroup.build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ASSIGNEDLICENSES + "." + SKUID)
                .setRequired(false)
                .setType(String.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).setMultiValued(true).build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ASSIGNEDLICENSES + "." + DISABLEDPLANS).setRequired(false)
                .setType(String.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).setMultiValued(true).build());


        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ASSIGNEDPLANS + "." + ASSIGNEDDATETIME)
                .setRequired(false)
                .setType(String.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).setMultiValued(true).build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ASSIGNEDPLANS + "." + CAPABILITYSTATUS)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setMultiValued(true).setReadable(true).build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ASSIGNEDPLANS + "." + SERVICE)
                .setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setMultiValued(true).setReadable(true).build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                ASSIGNEDPLANS + "." + SERVICEPLANID)
                .setRequired(false)
                .setType(String.class)
                .setCreateable(false).setUpdateable(false).setReadable(true).setMultiValued(true).build());

        AttributeInfoBuilder attrBusinessPhones = new AttributeInfoBuilder(BUSINESSPHONES);
        attrBusinessPhones
                .setMultiValued(true)
                .setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrBusinessPhones.build());

        AttributeInfoBuilder attrCity = new AttributeInfoBuilder(CITY);
        attrCity.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrCity.build());

        AttributeInfoBuilder attrCompanyName = new AttributeInfoBuilder(COMPANYNAME);
        attrCompanyName.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrCompanyName.build());

        AttributeInfoBuilder attrCountry = new AttributeInfoBuilder(COUNTRY);
        attrCountry.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrCountry.build());

        AttributeInfoBuilder attrConsentProvidedForMinor = new AttributeInfoBuilder(CONSENTPROVIDEDFORMINOR);
        attrConsentProvidedForMinor.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrConsentProvidedForMinor.build());

        AttributeInfoBuilder attrcreatedDateTime = new AttributeInfoBuilder(CREATEDDATETIME);
        attrcreatedDateTime.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrcreatedDateTime.build());

        AttributeInfoBuilder attrDepartment = new AttributeInfoBuilder(DEPARTMENT);
        attrDepartment.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrDepartment.build());

        AttributeInfoBuilder attrEmployeeId = new AttributeInfoBuilder(EMPLOYEEID);
        attrEmployeeId.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrEmployeeId.build());

        AttributeInfoBuilder attrFaxNumber = new AttributeInfoBuilder(FAXNUMBER);
        attrFaxNumber.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrFaxNumber.build());

        AttributeInfoBuilder attrGivenName = new AttributeInfoBuilder(GIVENNAME);
        attrGivenName.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrGivenName.build());

        AttributeInfoBuilder attrImAddresses = new AttributeInfoBuilder(IMADDRESSES);
        attrImAddresses.setMultiValued(true).setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrImAddresses.build());

        AttributeInfoBuilder attrId = new AttributeInfoBuilder(ID);
        attrId.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrId.build());


        AttributeInfoBuilder attrJobTitle = new AttributeInfoBuilder(JOBTITLE);
        attrJobTitle.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrJobTitle.build());

        AttributeInfoBuilder attrLegalAgeGroupClassification = new AttributeInfoBuilder(LEGALAGEGROUPCLASSIFICATION);
        attrLegalAgeGroupClassification.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrLegalAgeGroupClassification.build());

        AttributeInfoBuilder attrMail = new AttributeInfoBuilder(MAIL);
        attrMail.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMail.build());


        AttributeInfoBuilder attrMobilePhone = new AttributeInfoBuilder(MOBILEPHONE);
        attrMobilePhone.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrMobilePhone.build());


        AttributeInfoBuilder attrOfficeLocation = new AttributeInfoBuilder(OFFICELOCATION);
        attrOfficeLocation.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOfficeLocation.build());

        AttributeInfoBuilder attrOnPremisesLastSyncDateTime = new AttributeInfoBuilder(ONPREMISESLASTSYNCDATETIME);
        attrOnPremisesLastSyncDateTime.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesLastSyncDateTime.build());

        AttributeInfoBuilder attrOnPremisesSamAccountName = new AttributeInfoBuilder(ONPREMISESSAMACCOUNTNAME);
        attrOnPremisesSamAccountName.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesSamAccountName.build());

        AttributeInfoBuilder attrOnPremisesSecurityIdentifier = new AttributeInfoBuilder(ONPREMISESSECURITYIDENTIFIER);
        attrOnPremisesSecurityIdentifier.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesSecurityIdentifier.build());

        AttributeInfoBuilder attrOnPremisesUserPrincipalName = new AttributeInfoBuilder(ONPREMISESUSERPRINCIPALNAME);
        attrOnPremisesUserPrincipalName.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesUserPrincipalName.build());

        AttributeInfoBuilder attrOnPremisesSyncEnabled = new AttributeInfoBuilder(ONPREMISESSYNCENABLED);
        attrOnPremisesSyncEnabled.setRequired(false).setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrOnPremisesSyncEnabled.build());

        AttributeInfoBuilder attrOtherMails = new AttributeInfoBuilder(OTHERMAILS);
        attrOtherMails.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(true);
        userObjClassBuilder.addAttributeInfo(attrOtherMails.build());


        AttributeInfoBuilder attrPasswordPolicies = new AttributeInfoBuilder(PASSWORDPOLICIES);
        attrPasswordPolicies.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPasswordPolicies.build());


        AttributeInfoBuilder attrPostalCode = new AttributeInfoBuilder(POSTALCODE);
        attrPostalCode.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPostalCode.build());

        AttributeInfoBuilder attrPreferredLanguage = new AttributeInfoBuilder(PREFERREDLANGUAGE);
        attrPreferredLanguage.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrPreferredLanguage.build());


        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                PROVISIONEDPLANS + "." + CAPABILITYSTATUS)
                .setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                PROVISIONEDPLANS + "." + PROVISIONINGSTATUS)
                .setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());

        userObjClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(
                PROVISIONEDPLANS + "." + SERVICE)
                .setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).build());

        AttributeInfoBuilder attrProxyAddresses = new AttributeInfoBuilder(PROXYADDRESSES);
        attrProxyAddresses.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrProxyAddresses.build());

        AttributeInfoBuilder attrShowInAddressList = new AttributeInfoBuilder(SHOWINADDRESSLIST);
        attrShowInAddressList.setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrShowInAddressList.build());

        AttributeInfoBuilder attrState = new AttributeInfoBuilder(STATE);
        attrState.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrState.build());

        AttributeInfoBuilder attrStreetAddress = new AttributeInfoBuilder(STREETADDRESS);
        attrStreetAddress.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrStreetAddress.build());

        AttributeInfoBuilder attrSurname = new AttributeInfoBuilder(SURNAME);
        attrSurname.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrSurname.build());

        AttributeInfoBuilder attrUsageLocation = new AttributeInfoBuilder(USAGELOCATION);
        attrUsageLocation.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrUsageLocation.build());

        AttributeInfoBuilder attrUserType = new AttributeInfoBuilder(USERTYPE);
        attrUserType.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        userObjClassBuilder.addAttributeInfo(attrUserType.build());


        AttributeInfoBuilder attrGroupsMember = new AttributeInfoBuilder(USERDIRECTORYOBJECTMEMBER);
        attrGroupsMember.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(true);
        userObjClassBuilder.addAttributeInfo(attrGroupsMember.build());

        AttributeInfoBuilder attrGroupsOwner = new AttributeInfoBuilder(USERDIRECTORYOBJECTOWNER);
        attrGroupsOwner.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(true);
        userObjClassBuilder.addAttributeInfo(attrGroupsOwner.build());

        schemaBuilder.defineObjectClass(userObjClassBuilder.build());

    }


    protected Uid createUser(Set<Attribute> attributes) {
        LOG.info("Start createUser, attributes: {0}", attributes);

        if (attributes == null || attributes.isEmpty()) {
            throw new InvalidAttributeValueException("attributes not provided or empty");
        }
        userUid = null;
        final ICallback<User> callback = new ICallback<User>() {
            @Override
            public void success(final User user) {
                LOG.ok("Create user success!");
                userUid = new Uid(user.id);
                failure = false;
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.info("Create user failed!");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        User user = new User();

        PasswordProfile passwordProfile = new PasswordProfile();


        for (Attribute attribute : attributes) {
            if (attribute.getName().equals(Name.NAME)) {
                user.userPrincipalName = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(OperationalAttributes.ENABLE_NAME)) {
                user.accountEnabled = AttributeUtil.getBooleanValue(attribute);

            } else if (attribute.getName().equals(DISPLAYNAME)) {
                user.displayName = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(MAILNICKNAME)) {
                user.mailNickname = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(ONPREMISESIMMUTABLEID)) {
                user.onPremisesImmutableId = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(OperationalAttributes.FORCE_PASSWORD_CHANGE_NAME)) {
                passwordProfile.forceChangePasswordNextSignIn = AttributeUtil.getBooleanValue(attribute);

            } else if (attribute.getName().equals(PASSWORDPROFILE + "." + FORCECHANGEPASSWORDNEXTSIGNINWITHMFA)) {
                passwordProfile.forceChangePasswordNextSignInWithMfa = AttributeUtil.getBooleanValue(attribute);

            } else if (attribute.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
                GuardedString guardedString = (GuardedString) AttributeUtil.getSingleValue(attribute);
                GuardedStringAccessor accessor = new GuardedStringAccessor();
                guardedString.access(accessor);
                passwordProfile.password = accessor.getClearString();

            } else if (attribute.getName().equals(AGEGROUP)) {
                user.ageGroup = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(BUSINESSPHONES)) {
                List<String> businessPhones = new ArrayList<>();
                for (Object businessPhone : attribute.getValue()) {
                    businessPhones.add((String) businessPhone);
                }
                user.businessPhones = businessPhones;

            } else if (attribute.getName().equals(CITY)) {
                user.city = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(COMPANYNAME)) {
                user.companyName = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(CONSENTPROVIDEDFORMINOR)) {
                user.consentProvidedForMinor = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(COUNTRY)) {
                user.country = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(DEPARTMENT)) {
                user.department = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(EMPLOYEEID)) {
                user.employeeId = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(FAXNUMBER)) {
                user.faxNumber = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(GIVENNAME)) {
                user.givenName = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(JOBTITLE)) {
                user.jobTitle = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(MOBILEPHONE)) {
                user.mobilePhone = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(OFFICELOCATION)) {
                user.officeLocation = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(OTHERMAILS)) {
                List<String> otherMails = new ArrayList<>();
                for (Object otherMail : attribute.getValue()) {
                    otherMails.add((String) otherMail);
                }
                user.otherMails = otherMails;

            } else if (attribute.getName().equals(PASSWORDPOLICIES)) {
                user.passwordPolicies = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(POSTALCODE)) {
                user.postalCode = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(PREFERREDLANGUAGE)) {
                user.preferredLanguage = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(SHOWINADDRESSLIST)) {
                user.showInAddressList = AttributeUtil.getBooleanValue(attribute);

            } else if (attribute.getName().equals(STATE)) {
                user.state = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(STREETADDRESS)) {
                user.streetAddress = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(SURNAME)) {
                user.surname = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(USAGELOCATION)) {
                user.usageLocation = AttributeUtil.getAsStringValue(attribute);

            } else if (attribute.getName().equals(USERTYPE)) {
                user.userType = AttributeUtil.getAsStringValue(attribute);
            }
        }
        user.passwordProfile = passwordProfile;


        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().users().buildRequest().post(user, callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }

        if (failure) {

            throw new ClientException("Error while creating user! ", clientException);
        }

        LOG.info("{0}", userUid.getUidValue());

        //Add created user to groups
        for (Attribute attribute : attributes) {
            if (attribute.getName().equals(USERDIRECTORYOBJECTMEMBER)) {
                addUserToGroup(attribute, userUid);
            }
            if (attribute.getName().equals(USERDIRECTORYOBJECTOWNER)) {
                addUserGroupOwner(attribute, userUid);
            }
        }

        return userUid;


    }

    public void deleteUser(Uid uid) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }

        LOG.info("Delete user with UID: {0}", uid.getUidValue());

        final ICallback<User> callback = new ICallback<User>() {
            @Override
            public void success(User user) {
                LOG.ok("Delete user success!");
                failure = false;
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Delete user failed!");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().users().byId(uid.getUidValue()).buildRequest().delete(callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }

        if (failure) {
            throw new ClientException("Error while deleting user!", clientException);
        }

    }

    public void executeQueryForUser(Filter query, final ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQueryForUser()");
        Integer top = options.getPageSize();
        Integer skip = options.getPagedResultsOffset();
        if (query instanceof EqualsFilter) {
            LOG.info("query instanceof EqualsFilter");
            if (((EqualsFilter) query).getAttribute() instanceof Uid) {

                LOG.info("((EqualsFilter) query).getAttribute() instanceof Uid");

                Uid uid = (Uid) ((EqualsFilter) query).getAttribute();
                LOG.info("Uid {0}", uid);
                if (uid.getUidValue() == null) {
                    invalidAttributeValue("Uid", query);
                }

                final ICallback<User> callback = new ICallback<User>() {
                    @Override
                    public void success(User user) {
                        LOG.ok("Get User success!");
                        convertUserToConnectorObject(user, handler);
                        failure = false;
                        loginLatch.countDown();
                    }

                    @Override
                    public void failure(ClientException e) {
                        LOG.info("Get user failed!");
                        clientException = e;
                        failure = true;
                        loginLatch.countDown();
                    }
                };

                loginLatch = new CountDownLatch(1);

                List<Option> requestOptions = new ArrayList<>();
                requestOptions.add(new QueryOption("$select", SELECT));

                getGraphServiceClient().users(uid.getUidValue()).buildRequest(requestOptions).get(callback);
                try {
                    loginLatch.await();
                } catch (InterruptedException e) {
                    throw new ConnectorException(e);
                }

                if (failure) {
                    throw new ClientException("Error in executeQueryForUser!", clientException);
                }
            } else if (((EqualsFilter) query).getAttribute() instanceof Name) {

                LOG.info("((EqualsFilter) query).getAttribute() instanceof Name");


                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("Name", query);
                }

                String userPrincipalName = allValues.get(0).toString();
                LOG.info("userPrincipalName name is: {0}", userPrincipalName);


                final ICallback<User> callback = new ICallback<User>() {
                    @Override
                    public void success(User user) {
                        LOG.ok("Get User success!");
                        convertUserToConnectorObject(user, handler);
                        failure = false;
                        loginLatch.countDown();
                    }

                    @Override
                    public void failure(ClientException e) {
                        LOG.info("Get user failed!");
                        clientException = e;
                        failure = true;
                        loginLatch.countDown();
                    }
                };

                loginLatch = new CountDownLatch(1);

                List<Option> requestOptions = new ArrayList<>();
                requestOptions.add(new QueryOption("$select", SELECT));

                getGraphServiceClient().users(userPrincipalName).buildRequest(requestOptions).get(callback);
                try {
                    loginLatch.await();
                } catch (InterruptedException e) {
                    throw new ConnectorException(e);
                }

                if (failure) {
                    throw new ClientException("Error in executeQueryForUser!", clientException);
                }
            } else if (((EqualsFilter) query).getAttribute().getName().equals(OperationalAttributes.ENABLE_NAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof Enable");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("Enabled", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=accountEnabled eq boolean
                requestOptions.add(new QueryOption("$filter", ACCOUNTENABLED + "%20eq%20" + attributeValue));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);

            } else if (((EqualsFilter) query).getAttribute().getName().equals(CITY)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("Name", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=city eq 'attribute'
                requestOptions.add(new QueryOption("$filter", CITY + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);

            } else if (((EqualsFilter) query).getAttribute().getName().equals(COUNTRY)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof country");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("Country", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=country eq 'attribute'
                requestOptions.add(new QueryOption("$filter", COUNTRY + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);

            } else if (((EqualsFilter) query).getAttribute().getName().equals(DEPARTMENT)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof department");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("Department", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=department eq 'attribute'
                requestOptions.add(new QueryOption("$filter", DEPARTMENT + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);

            } else if (((EqualsFilter) query).getAttribute().getName().equals(DISPLAYNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("DisplayName", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=displayName eq 'attribute'
                requestOptions.add(new QueryOption("$filter", DISPLAYNAME + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(EMPLOYEEID)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof employeeid");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("employeeid", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=employeeId eq 'attribute'
                requestOptions.add(new QueryOption("$filter", EMPLOYEEID + "%20eq%20" + "'" + attributeValue + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(GIVENNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof givenName");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("givenName", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=givenName eq 'attribute'
                requestOptions.add(new QueryOption("$filter", GIVENNAME + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(JOBTITLE)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof jobTitle");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("jobTitle", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=jobTitle eq 'attribute'
                requestOptions.add(new QueryOption("$filter", JOBTITLE + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(MAIL)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof mail");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mail", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=mail eq 'attribute'
                requestOptions.add(new QueryOption("$filter", MAIL + "%20eq%20" + "'" + attributeValue + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(MAILNICKNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof mailNickname");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mailNickname", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=mailNickname eq 'attribute'
                requestOptions.add(new QueryOption("$filter", MAILNICKNAME + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(ONPREMISESIMMUTABLEID)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof onPremisesImmutableId");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("onPremisesImmutableId", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=onPremisesImmutableId eq 'attribute'
                requestOptions.add(new QueryOption("$filter", ONPREMISESIMMUTABLEID + "%20eq%20" + "'" + attributeValue + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(OTHERMAILS)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof otherMails");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("otherMails", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=filter=otherMails/any(o:o eq 'attribute')
                requestOptions.add(new QueryOption("$filter", OTHERMAILS + "/any(o:o" + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(PROXYADDRESSES)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof proxyAddresses");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("proxyAddresses", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=filter=proxyAddresses/any(o:o eq 'attribute')
                requestOptions.add(new QueryOption("$filter", PROXYADDRESSES + "/any(o:o" + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(STATE)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof state");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("state", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=state eq 'attribute'
                requestOptions.add(new QueryOption("$filter", STATE + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(SURNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof surname");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("surname", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=surname eq 'attribute'
                requestOptions.add(new QueryOption("$filter", SURNAME + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(USAGELOCATION)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof usageLocation");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("usageLocation", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=usageLocation eq 'attribute'
                requestOptions.add(new QueryOption("$filter", USAGELOCATION + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(USERTYPE)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof userType");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("userType", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=userType eq 'attribute'
                requestOptions.add(new QueryOption("$filter", USERTYPE + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            }
        } else if (query instanceof StartsWithFilter) {
            LOG.info("query instanceof StartsWithFilter");
            if (((StartsWithFilter) query).getAttribute().getName().equals(Name.NAME)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof name - userPrincipalName");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("name - userPrincipalName", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(userPrincipalName,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + USERPRINCIPALNAME + ",'" + attributeValue + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(CITY)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof city");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("city", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(city,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + CITY + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(COUNTRY)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof country");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("country", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(country,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + COUNTRY + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(DEPARTMENT)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof departmant");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("departmant", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(departmant,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + DEPARTMENT + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(DISPLAYNAME)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("displayName", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(displayName,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + DISPLAYNAME + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(EMPLOYEEID)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof employeeId");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("employeeId", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(employeeId,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + EMPLOYEEID + ",'" + attributeValue + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(GIVENNAME)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof givenName");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("givenName", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(givenName,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + GIVENNAME + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(JOBTITLE)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof jobTitle");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("jobTitle", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(jobTitle,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + JOBTITLE + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(MAIL)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof mail");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mail", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(mail,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + MAIL + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(MAILNICKNAME)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof mailNickname");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mailNickname", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(mailNickname,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + MAILNICKNAME + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(ONPREMISESIMMUTABLEID)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof onPremisesImmutableId");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("onPremisesImmutableId", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(onPremisesImmutableId,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + ONPREMISESIMMUTABLEID + ",'" + attributeValue + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(OTHERMAILS)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof otherMails");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("otherMails", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=otherMails/any(x:startswith(x,'attribute'))
                requestOptions.add(new QueryOption("$filter", OTHERMAILS + "/any" + "(x:" + STARTSWITH + "(x,'" + getEncodedAttributeValue(attributeValue) + "'))"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(PROXYADDRESSES)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof proxyAddresses");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("proxyAddresses", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=proxyAddresses/any(x:startswith(x,'attribute'))
                requestOptions.add(new QueryOption("$filter", PROXYADDRESSES + "/any" + "(x:" + STARTSWITH + "(x,'" + getEncodedAttributeValue(attributeValue) + "'))"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(STATE)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof state");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("state", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(state,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + STATE + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(SURNAME)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof surname");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("surname", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(surname,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + SURNAME + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(USAGELOCATION)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof usageLocation");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("usageLocation", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=usageLocation(usageLocation,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + USAGELOCATION + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IUserCollectionRequest request = createRequest(requestOptions, top);
                UserCollectionPageProcessing(handler, request, skip);
            }


        } else if (query == null) {
            LOG.info("query==null");
            IUserCollectionRequest request;

            //no requestOptions
            if (top != null) {
                request = getGraphServiceClient().users().buildRequest().top(top);
            } else {
                request = getGraphServiceClient().users().buildRequest();
            }
            UserCollectionPageProcessing(handler, request, skip);
        }


    }

    private IUserCollectionRequest createRequest(List<Option> requestOptions, Integer top) {
        if (top != null) {
            return getGraphServiceClient().users().buildRequest(requestOptions).top(top);
        }
        return getGraphServiceClient().users().buildRequest(requestOptions);
    }


    private void UserCollectionPageProcessing(ResultsHandler handler, IUserCollectionRequest request) {
        IUserCollectionPage page;
        IUserCollectionRequestBuilder builder;

        do {
            page = request.get();
            for (User user : page.getCurrentPage()) {
                convertUserToConnectorObject(user, handler);
            }
            builder = page.getNextPage();
            if (builder == null) {
                request = null;
            } else {
                request = builder.buildRequest();
            }
        } while (builder != null);
    }

    private void UserCollectionPageProcessing(ResultsHandler handler, IUserCollectionRequest request, Integer skip) {
        if (skip == null) {
            UserCollectionPageProcessing(handler, request);
        } else {
            IUserCollectionPage page;
            IUserCollectionRequestBuilder builder;
            int count = 1;
            do {
                page = request.get();
                if (count == skip) {
                    for (User user : page.getCurrentPage()) {
                        convertUserToConnectorObject(user, handler);
                    }
                    return;
                }
                builder = page.getNextPage();
                if (builder == null) {
                    request = null;
                } else {
                    request = builder.buildRequest();
                }
                count++;
            } while (builder != null);
        }
    }

    public void updateUser(Uid uid, Set<Attribute> attributes) {
        LOG.info("Start updateUser, attributes: {0} , uid: {1}", attributes, uid);
        if (attributes == null || attributes.isEmpty()) {
            throw new InvalidAttributeValueException("attributes not provided or empty");
        }

        final ICallback<User> callback = new ICallback<User>() {
            @Override
            public void success(final User user) {
                LOG.ok("Update user success!");
                failure = false;
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Update user failed!");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        User azureADUser = new User();
        PasswordProfile passwordProfile = new PasswordProfile();
        for (Attribute attribute : attributes) {
            if (attribute.getName().equals(Name.NAME)) {
                azureADUser.userPrincipalName = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(OperationalAttributes.ENABLE_NAME)) {
                azureADUser.accountEnabled = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(DISPLAYNAME)) {
                azureADUser.displayName = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(MAILNICKNAME)) {
                azureADUser.mailNickname = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(ONPREMISESIMMUTABLEID)) {
                azureADUser.onPremisesImmutableId = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(OperationalAttributes.FORCE_PASSWORD_CHANGE_NAME)) {
                passwordProfile.forceChangePasswordNextSignIn = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(PASSWORDPROFILE + "." + FORCECHANGEPASSWORDNEXTSIGNINWITHMFA)) {
                passwordProfile.forceChangePasswordNextSignInWithMfa = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(OperationalAttributes.PASSWORD_NAME)) {
                GuardedString guardedString = (GuardedString) AttributeUtil.getSingleValue(attribute);
                GuardedStringAccessor accessor = new GuardedStringAccessor();
                guardedString.access(accessor);
                passwordProfile.password = accessor.getClearString();
            } else if (attribute.getName().equals(AGEGROUP)) {
                azureADUser.ageGroup = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(BUSINESSPHONES)) {
                List<String> businessPhones = new ArrayList<>();
                for (Object businessPhone : attribute.getValue()) {
                    businessPhones.add((String) businessPhone);
                }
                azureADUser.businessPhones = businessPhones;
            } else if (attribute.getName().equals(CITY)) {
                azureADUser.city = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(COMPANYNAME)) {
                azureADUser.companyName = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(CONSENTPROVIDEDFORMINOR)) {
                azureADUser.consentProvidedForMinor = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(COUNTRY)) {
                azureADUser.country = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(DEPARTMENT)) {
                azureADUser.department = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(EMPLOYEEID)) {
                azureADUser.employeeId = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(FAXNUMBER)) {
                azureADUser.faxNumber = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(GIVENNAME)) {
                azureADUser.givenName = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(JOBTITLE)) {
                azureADUser.jobTitle = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(MOBILEPHONE)) {
                azureADUser.mobilePhone = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(OFFICELOCATION)) {
                azureADUser.officeLocation = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(OTHERMAILS)) {
                List<String> otherMails = new ArrayList<>();
                for (Object otherMail : attribute.getValue()) {
                    otherMails.add((String) otherMail);
                }
                azureADUser.otherMails = otherMails;
            } else if (attribute.getName().equals(PASSWORDPOLICIES)) {
                azureADUser.passwordPolicies = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(POSTALCODE)) {
                azureADUser.postalCode = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(PREFERREDLANGUAGE)) {
                azureADUser.preferredLanguage = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(SHOWINADDRESSLIST)) {
                azureADUser.showInAddressList = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(STATE)) {
                azureADUser.state = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(STREETADDRESS)) {
                azureADUser.streetAddress = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(SURNAME)) {
                azureADUser.surname = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(USAGELOCATION)) {
                azureADUser.usageLocation = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(USERTYPE)) {
                azureADUser.userType = AttributeUtil.getAsStringValue(attribute);
            }

        }

        azureADUser.passwordProfile = passwordProfile;

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().users(uid.getUidValue()).buildRequest().patch(azureADUser, callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }

        if (failure) {
            throw new ClientException("Error when updating user attributes in Azure AD!", clientException);
        }

    }


    public void addUserToGroup(Attribute attribute, Uid uid) {
        LOG.info("addUserToGroup, attributes: {0}, uid: {1}", attribute, uid);
        String userId = uid.getUidValue();
        String groupId;
        for (Object group : attribute.getValue()) {
            LOG.info("value {0}", group);
            if (!(group instanceof String)) {
                LOG.error("Not string!");
            } else {
                groupId = (String) group;
                addUserToGroup(groupId, userId);
            }
        }
    }

    private void addUserToGroup(String groupId, String userId) {
        User user = new User();
        user.id = userId;

        ICallback<DirectoryObject> callback = new ICallback<DirectoryObject>() {

            @Override
            public void success(DirectoryObject directoryObject) {
                LOG.ok("Add user to group success");
                failure = false;
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Add user to group failed");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().groups(groupId).members().references().buildRequest().post(user, callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
        if (failure) {
            throw new ClientException("Error add user to group!", clientException);
        }

    }

    public void removeUserFromGroup(Attribute attribute, Uid uid) {
        LOG.info("removeUserFromGroup, attributes: {0}, uid: {1}", attribute, uid);

        String userId = uid.getUidValue();
        String groupId;
        for (Object v : attribute.getValue()) {
            LOG.info("value {0}", v);
            if (!(v instanceof String)) {
                LOG.error("Not string!");
            } else {
                groupId = (String) v;
                removeUserFromGroup(groupId, userId);
            }
        }

    }

    public void removeUserFromGroup(String groupId, String userId) {

        ICallback<DirectoryObject> callback = new ICallback<DirectoryObject>() {
            @Override
            public void success(DirectoryObject directoryObject) {
                LOG.ok("Remove user from group success");
                failure = false;
                loginLatch.countDown();

            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Remove user from group failed");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().groups(groupId).members(userId).reference().buildRequest().delete(callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }

        if (failure) {
            throw new ClientException("Error remove user from group!", clientException);
        }

    }


    public void addUserGroupOwner(Attribute attribute, Uid uid) {
        LOG.info("addUserGroupOwner, attributes: {0}, uid: {1}", attribute, uid);

        String userId = uid.getUidValue();
        String groupId;
        for (Object group : attribute.getValue()) {
            LOG.info("value {0}", group);
            if (!(group instanceof String)) {
                LOG.error("Not string!");
            } else {
                groupId = (String) group;
                addUserGroupOwner(groupId, userId);
            }
        }
    }

    private void addUserGroupOwner(String groupId, String userId) {
        User user = new User();
        user.id = userId;

        ICallback<DirectoryObject> callback = new ICallback<DirectoryObject>() {

            @Override
            public void success(DirectoryObject directoryObject) {
                LOG.ok("Add user as group owner success");
                failure = false;
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Remove group owner failed");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().groups(groupId).owners().references().buildRequest().post(user, callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
        if (failure) {
            throw new ClientException("Error add user group owner!", clientException);
        }

    }

    public void removeUserGroupOwner(Attribute attribute, Uid uid) {
        LOG.info("removeUserGroupOwner, attributes: {0}, uid: {1}", attribute, uid);

        String userId = uid.getUidValue();
        String groupId;
        for (Object group : attribute.getValue()) {
            LOG.info("value {0}", group);
            if (!(group instanceof String)) {
                LOG.error("Not string!");
            } else {
                groupId = (String) group;
                removeUserGroupOwner(groupId, userId);
            }
        }


    }

    public void removeUserGroupOwner(String groupId, String userId) {

        ICallback<DirectoryObject> callback = new ICallback<DirectoryObject>() {

            @Override
            public void success(DirectoryObject directoryObject) {
                LOG.ok("Remove group from group success");
                failure = false;
                loginLatch.countDown();

            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Remove group from group failed");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().groups(groupId).owners(userId).reference().buildRequest().delete(callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
        if (failure) {
            throw new ClientException("Error remove group owner!", clientException);
        }

    }


    private void convertUserToConnectorObject(User user, ResultsHandler handler) {
        LOG.info("convertUserToConnectorObject, user: {0}, {1} handler {2}", user.displayName, user.city, handler);
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.ACCOUNT);

        if (user.id != null) {
            builder.setUid(new Uid(user.id));
        }
        if (user.userPrincipalName != null) {
            builder.setName(user.userPrincipalName);
            builder.addAttribute(USERPRINCIPALNAME, user.userPrincipalName);
        }
        if (user.accountEnabled != null) {
            builder.addAttribute(OperationalAttributes.ENABLE_NAME, user.accountEnabled);
        }
        if (user.ageGroup != null) {
            builder.addAttribute(AGEGROUP, user.ageGroup);
        }
        if (user.assignedLicenses != null) {
            List<String> skuIds = new ArrayList<>();
            List<String> listDisabledPlans = new ArrayList<>();
            for (AssignedLicense assignedLicense : user.assignedLicenses) {
                if (assignedLicense.disabledPlans != null) {
                    List<UUID> disabledPlans = new ArrayList<>();
                    for (UUID disabledPlan : assignedLicense.disabledPlans) {
                        disabledPlans.add(disabledPlan);
                    }
                    listDisabledPlans.add(disabledPlans.toString());
                }
                if (assignedLicense.skuId != null) {
                    skuIds.add(assignedLicense.skuId.toString());
                }
            }
            builder.addAttribute(ASSIGNEDLICENSES + "." + DISABLEDPLANS, listDisabledPlans);
            builder.addAttribute(ASSIGNEDLICENSES + "." + SKUID, skuIds);

        }
        if (user.assignedPlans != null) {
            List<String> assignedDateTimes = new ArrayList<>();
            List<String> capabilityStatuses = new ArrayList<>();
            List<String> services = new ArrayList<>();
            List<String> servicePlanIds = new ArrayList<>();

            for (AssignedPlan assignedPlan : user.assignedPlans) {
                if (assignedPlan.assignedDateTime != null) {
                    assignedDateTimes.add(assignedPlan.assignedDateTime.getTime().toString());
                }
                if (assignedPlan.capabilityStatus != null) {
                    capabilityStatuses.add(assignedPlan.capabilityStatus);
                }
                if (assignedPlan.service != null) {
                    services.add(assignedPlan.service);
                }
                if (assignedPlan.servicePlanId != null) {
                    servicePlanIds.add(assignedPlan.servicePlanId.toString());
                }
            }

            builder.addAttribute(ASSIGNEDPLANS + "." + ASSIGNEDDATETIME, assignedDateTimes);
            builder.addAttribute(ASSIGNEDPLANS + "." + CAPABILITYSTATUS, capabilityStatuses);
            builder.addAttribute(ASSIGNEDPLANS + "." + SERVICE, services);
            builder.addAttribute(ASSIGNEDPLANS + "." + SERVICEPLANID, servicePlanIds);

        }
        if (user.businessPhones != null) {
            List<String> businessPhones = new ArrayList<>();
            for (String businessPhone : user.businessPhones) {
                businessPhones.add(businessPhone);
            }
            builder.addAttribute(BUSINESSPHONES, businessPhones);
        }
        if (user.city != null) {
            builder.addAttribute(CITY, user.city);
        }
        if (user.companyName != null) {
            builder.addAttribute(COMPANYNAME, user.companyName);
        }
        if (user.consentProvidedForMinor != null) {
            builder.addAttribute(CONSENTPROVIDEDFORMINOR, user.consentProvidedForMinor);
        }
        if (user.country != null) {
            builder.addAttribute(COUNTRY, user.country);
        }
        if (user.department != null) {
            builder.addAttribute(DEPARTMENT, user.department);
        }
        if (user.displayName != null) {
            builder.addAttribute(DISPLAYNAME, user.displayName);
        }
        if (user.employeeId != null) {
            builder.addAttribute(EMPLOYEEID, user.employeeId);
        }
        if (user.faxNumber != null) {
            builder.addAttribute(FAXNUMBER, user.faxNumber);
        }
        if (user.givenName != null) {
            builder.addAttribute(GIVENNAME, user.givenName);
        }

        if (user.imAddresses != null) {
            List<String> imAddresses = new ArrayList<>();
            for (String imAddress : user.imAddresses) {
                imAddresses.add(imAddress);
            }
            builder.addAttribute(IMADDRESSES, imAddresses);
        }
        if (user.jobTitle != null) {
            builder.addAttribute(JOBTITLE, user.jobTitle);
        }
        if (user.legalAgeGroupClassification != null) {
            builder.addAttribute(LEGALAGEGROUPCLASSIFICATION, user.legalAgeGroupClassification);
        }

        if (user.mail != null) {
            builder.addAttribute(MAIL, user.mail);
        }
        if (user.mailNickname != null) {
            builder.addAttribute(MAILNICKNAME, user.mailNickname);
        }
        if (user.mobilePhone != null) {
            builder.addAttribute(MOBILEPHONE, user.mobilePhone);
        }

        if (user.officeLocation != null) {
            builder.addAttribute(OFFICELOCATION, user.officeLocation);
        }
        if (user.onPremisesImmutableId != null) {
            builder.addAttribute(ONPREMISESIMMUTABLEID, user.onPremisesImmutableId);
        }
        if (user.onPremisesLastSyncDateTime != null) {
            builder.addAttribute(ONPREMISESLASTSYNCDATETIME, user.onPremisesLastSyncDateTime.getTime().toString());
        }
        if (user.onPremisesSamAccountName != null) {
            builder.addAttribute(ONPREMISESSAMACCOUNTNAME, user.onPremisesSamAccountName);
        }
        if (user.onPremisesSecurityIdentifier != null) {
            builder.addAttribute(ONPREMISESSECURITYIDENTIFIER, user.onPremisesSecurityIdentifier);
        }
        if (user.onPremisesSyncEnabled != null) {
            builder.addAttribute(ONPREMISESSYNCENABLED, user.onPremisesSyncEnabled);
        }
        if (user.onPremisesUserPrincipalName != null) {
            builder.addAttribute(ONPREMISESUSERPRINCIPALNAME, user.onPremisesUserPrincipalName);
        }
        if (user.otherMails != null) {
            List<String> otherMails = new ArrayList<>();
            for (String otherMail : user.otherMails) {
                otherMails.add(otherMail);
            }
            builder.addAttribute(OTHERMAILS, otherMails);
        }
        if (user.passwordPolicies != null) {
            builder.addAttribute(PASSWORDPOLICIES, user.passwordPolicies);
        }
        if (user.passwordProfile != null) {
            if (user.passwordProfile.forceChangePasswordNextSignIn != null) {
                builder.addAttribute(OperationalAttributes.FORCE_PASSWORD_CHANGE_NAME, user.passwordProfile.forceChangePasswordNextSignIn);
            }
            if (user.passwordProfile.forceChangePasswordNextSignInWithMfa != null) {
                builder.addAttribute(PASSWORDPROFILE + "." + FORCECHANGEPASSWORDNEXTSIGNINWITHMFA, user.passwordProfile.forceChangePasswordNextSignInWithMfa);
            }
        }

        if (user.postalCode != null) {
            builder.addAttribute(POSTALCODE, user.postalCode);
        }
        if (user.preferredLanguage != null) {
            builder.addAttribute(PREFERREDLANGUAGE, user.preferredLanguage);
        }

        if (user.provisionedPlans != null) {
            List<String> capabilityStatus = new ArrayList<>();
            List<String> provisioningStatus = new ArrayList<>();
            List<String> service = new ArrayList<>();

            for (ProvisionedPlan provisionedPlan : user.provisionedPlans) {
                if (provisionedPlan.capabilityStatus != null) {
                    capabilityStatus.add(provisionedPlan.capabilityStatus);
                }
                if (provisionedPlan.provisioningStatus != null) {
                    provisioningStatus.add(provisionedPlan.provisioningStatus);
                }
                if (provisionedPlan.service != null) {
                    service.add(provisionedPlan.service);
                }
            }

            builder.addAttribute(PROVISIONEDPLANS + "." + CAPABILITYSTATUS, capabilityStatus);
            builder.addAttribute(PROVISIONEDPLANS + "." + PROVISIONINGSTATUS, provisioningStatus);
            builder.addAttribute(PROVISIONEDPLANS + "." + SERVICE, service);
        }
        if (user.proxyAddresses != null) {
            List<String> proxyAddresses = new ArrayList<>();
            for (String proxyAddress : user.proxyAddresses) {
                proxyAddresses.add(proxyAddress);
            }
            builder.addAttribute(PROXYADDRESSES, proxyAddresses);
        }

        if (user.showInAddressList != null) {
            builder.addAttribute(SHOWINADDRESSLIST, user.showInAddressList);
        }
        if (user.state != null) {
            builder.addAttribute(STATE, user.state);
        }
        if (user.streetAddress != null) {
            builder.addAttribute(STREETADDRESS, user.streetAddress);
        }
        if (user.surname != null) {
            builder.addAttribute(SURNAME, user.surname);
        }
        if (user.usageLocation != null) {
            builder.addAttribute(USAGELOCATION, user.usageLocation);
        }
        if (user.userType != null) {
            builder.addAttribute(USERTYPE, user.userType);
        }

        convertUserDirectoryObjectMember(user.id, builder);
        convertUserDirectoryObjectOwner(user.id, builder);

        ConnectorObject connectorObject = builder.build();
        handler.handle(connectorObject);
    }

    private void convertUserDirectoryObjectMember(String userId, ConnectorObjectBuilder builder) {
        LOG.info("convertUserDirectoryObjectMember, userId {0}", userId);

        IDirectoryObjectCollectionWithReferencesRequest request = getGraphServiceClient().users(userId).memberOf().buildRequest();
        //list with groups where user has membership
        List<String> listDirectoryObject = getListDirectoryObject(request, GROUP);

        builder.addAttribute(USERDIRECTORYOBJECTMEMBER, listDirectoryObject);

    }

    private void convertUserDirectoryObjectOwner(String userId, ConnectorObjectBuilder builder) {
        LOG.info("convertUserDirectoryObjectOwner, userId {0}", userId);

        IDirectoryObjectCollectionWithReferencesRequest request = getGraphServiceClient().users(userId).ownedObjects().buildRequest();
        List<String> listDirectoryObject = getListDirectoryObject(request, GROUP);

        builder.addAttribute(USERDIRECTORYOBJECTOWNER, listDirectoryObject);

    }


    private void invalidAttributeValue(String attrName, Filter query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Value of").append(attrName).append("attribute not provided for query: ").append(query);
        throw new InvalidAttributeValueException(sb.toString());
    }
}