package com.evolveum.polygon.connector.msconnector.rest;

import com.microsoft.graph.core.ClientException;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

import java.util.*;

public class FilteringTest extends BasicConfigurationForTests {


    @Test
    public void filteringTestAccountObjectClass() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);


        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", false));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink Panda"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "PinkBear"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "Pinkes@TENANT.onmicrosoft.com"));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        attributesAccount.add(AttributeBuilder.build("city", "New York"));
        attributesAccount.add(AttributeBuilder.build("country", "Great Britain"));
        attributesAccount.add(AttributeBuilder.build("department", "IT Departmant New York"));
        attributesAccount.add(AttributeBuilder.build("employeeId", "007"));
        attributesAccount.add(AttributeBuilder.build("jobTitle", "Human Resources"));
        ArrayList<String> otherMails = new ArrayList<>();
        otherMails.add("mysupermail@gmail.com");
        otherMails.add("midpoint.mail@evo.com");
        attributesAccount.add(AttributeBuilder.build("otherMails", otherMails));
        attributesAccount.add(AttributeBuilder.build("state", "California State"));
        attributesAccount.add(AttributeBuilder.build("surname", "Smith Pink"));
        attributesAccount.add(AttributeBuilder.build("usageLocation", "US"));
        Uid firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        Set<Attribute> attributesAccount1 = new HashSet<>();
        attributesAccount1.add(AttributeBuilder.build("__ENABLE__", false));
        attributesAccount1.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount1.add(AttributeBuilder.build("displayName", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("mailNickname", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("__NAME__", "PinkAndGreen@TENANT.onmicrosoft.com"));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount1.add(AttributeBuilder.build("__PASSWORD__", pass1));
        attributesAccount1.add(AttributeBuilder.build("city", "New York"));
        attributesAccount1.add(AttributeBuilder.build("country", "Slovakia"));
        attributesAccount1.add(AttributeBuilder.build("department", "IT"));
        attributesAccount1.add(AttributeBuilder.build("employeeId", "007"));
        attributesAccount1.add(AttributeBuilder.build("givenName", "Peter The Biggest"));
        attributesAccount1.add(AttributeBuilder.build("jobTitle", "Human Resources"));
        attributesAccount1.add(AttributeBuilder.build("state", "California"));
        attributesAccount1.add(AttributeBuilder.build("surname", "Smith"));
        attributesAccount1.add(AttributeBuilder.build("userType", "Member"));

        Uid secondUser = msGraphConnector.create(objectClassAccount, attributesAccount1, options);
//
//        AttributeFilter containsFilterAccount;
//        containsFilterAccount = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "Pink"));

        final ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        SearchResultsHandler handlerAccount = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsAccount.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };


        //get not enabled users
        AttributeFilter equalsFilterAccountEnabled;
        equalsFilterAccountEnabled = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, false));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterAccountEnabled, handlerAccount, options);
        ArrayList<Uid> listUidAccountEnabled = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidAccountEnabled.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user enable " + obj.getUid().getUidValue());
        }


        try {
            if (!listUidAccountEnabled.contains(firstUser) || !listUidAccountEnabled.contains(secondUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return Enable.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterCity;
        equalsFilterCity = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("city", "New York"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterCity, handlerAccount, options);
        ArrayList<Uid> listUidCity = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidCity.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user city " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidCity.contains(firstUser) || !listUidCity.contains(secondUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return city.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterCountry;
        equalsFilterCountry = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("country", "Great Britain"));
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterCountry, handlerAccount, options);
        ArrayList<Uid> listUidCountry = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidCountry.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user country  " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidCountry.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return country.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterDepartment;
        equalsFilterDepartment = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("department", "IT Departmant New York"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterDepartment, handlerAccount, options);
        ArrayList<Uid> listUidDepartment = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidDepartment.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user department  " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidDepartment.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return department.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterDisplayName;
        equalsFilterDisplayName = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("displayName", "Pink Panda"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterDisplayName, handlerAccount, options);
        ArrayList<Uid> listUidDisplayName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidDisplayName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user displayname " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidDisplayName.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return displayName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterEmployeeId;
        equalsFilterEmployeeId = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("employeeId", "007"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterEmployeeId, handlerAccount, options);
        ArrayList<Uid> listUidEmployeeId = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidEmployeeId.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user employeeId " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidEmployeeId.contains(firstUser) || !listUidEmployeeId.contains(secondUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return employeeId.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterGivenName;
        equalsFilterGivenName = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("givenName", "Peter The Biggest"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterGivenName, handlerAccount, options);
        ArrayList<Uid> listUidGivenName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidGivenName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user givenName " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidGivenName.contains(secondUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return givenName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterJobTitle;
        equalsFilterJobTitle = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("jobTitle", "Human Resources"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterJobTitle, handlerAccount, options);
        ArrayList<Uid> listUidJobTitle = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidJobTitle.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user jobTitle " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidJobTitle.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return jobTitle.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterMailNickName;
        equalsFilterMailNickName = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("mailNickname", "PinkBear"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterMailNickName, handlerAccount, options);
        ArrayList<Uid> listUidMailNickName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidMailNickName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user mailNickname " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidMailNickName.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return mailNickname.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterOtherMails;
        equalsFilterOtherMails = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("otherMails", "mysupermail@gmail.com"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterOtherMails, handlerAccount, options);
        ArrayList<Uid> listUidOtherMails = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidOtherMails.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user otherMails " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidOtherMails.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return otherMails.");
            }
        } catch (ClientException e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterState;
        equalsFilterState = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("state", "California State"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterState, handlerAccount, options);
        ArrayList<Uid> listUidState = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidState.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user state " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidState.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return state.");
            }
        } catch (ClientException e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterSurname;
        equalsFilterSurname = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("surname", "Smith Pink"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterSurname, handlerAccount, options);
        ArrayList<Uid> listUid11 = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid11.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user surname " + obj.getUid().getUidValue());
        }
        try {
            if (!listUid11.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return surname.");
            }
        } catch (ClientException e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterUsageLocation;
        equalsFilterUsageLocation = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("usageLocation", "US"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterUsageLocation, handlerAccount, options);
        ArrayList<Uid> listUidUsageLocation = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidUsageLocation.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user usageLocation " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidUsageLocation.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return usageLocation.");
            }
        } catch (ClientException e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterName;
        equalsFilterName = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("__NAME__", "PinkAndGreen@TENANT.onmicrosoft.com"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterName, handlerAccount, options);
        ArrayList<Uid> listUidName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user userPrincipalName " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidName.contains(secondUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return userPrincipalName.");
            }
        } catch (ClientException e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterUserType;
        equalsFilterUserType = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("userType", "Member"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterUserType, handlerAccount, options);
        ArrayList<Uid> listUidUserType = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidUserType.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user userType " + obj.getUid().getUidValue());
        }
        try {
            if (!listUidUserType.contains(secondUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return userType.");
            }
        } catch (ClientException e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        } finally {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


    }

    @Test
    public void equalFilterTestGroupObjectClass() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("__NAME__", "DisplayName GroupYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "GroupYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("groupTypes", "Unified"));

        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("__NAME__", "DisplayName GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);


        final ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        SearchResultsHandler handlerAccount = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsAccount.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };


        AttributeFilter equalsFilterDisplayName;
        equalsFilterDisplayName = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("__NAME__", "DisplayName GroupYellow"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, equalsFilterDisplayName, handlerAccount, options);
        ArrayList<Uid> listUidDisplayName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidDisplayName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group displayName " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidDisplayName.contains(groupYellow)) {
                throw new InvalidAttributeValueException("EqualsFilter not return displayName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterGroupTypes;
        equalsFilterGroupTypes = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("groupTypes", "Unified"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, equalsFilterGroupTypes, handlerAccount, options);
        ArrayList<Uid> listUidGroupTypes = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidGroupTypes.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group groupTypes " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidGroupTypes.contains(groupYellow)) {
                throw new InvalidAttributeValueException("EqualsFilter not return groupTypes.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterMail;
        equalsFilterMail = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("mail", "GroupYellow@TENANT.onmicrosoft.com"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, equalsFilterMail, handlerAccount, options);
        ArrayList<Uid> listUidMail = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidMail.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group mail " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidMail.contains(groupYellow)) {
                throw new InvalidAttributeValueException("EqualsFilter not return mail.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterMailNickName;
        equalsFilterMailNickName = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("mailNickname", "GroupYellow"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, equalsFilterMailNickName, handlerAccount, options);
        ArrayList<Uid> listUidMailNickName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidMailNickName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group mailNickname " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidMailNickName.contains(groupYellow)) {
                throw new InvalidAttributeValueException("EqualsFilter not return mailNickname.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterProxyAddresses;
        equalsFilterProxyAddresses = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("proxyAddresses", "SMTP:GroupYellow@TENANT.onmicrosoft.com"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, equalsFilterProxyAddresses, handlerAccount, options);
        ArrayList<Uid> listUidProxyAddresses = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidProxyAddresses.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group proxyAddresses " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidProxyAddresses.contains(groupYellow)) {
                throw new InvalidAttributeValueException("EqualsFilter not return proxyAddresses.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equalsFilterAccountSecurityEnabled;
        equalsFilterAccountSecurityEnabled = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("securityEnabled", false));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, equalsFilterAccountSecurityEnabled, handlerAccount, options);
        ArrayList<Uid> listUidSecurityEnabled = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidSecurityEnabled.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group securityEnabled " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidSecurityEnabled.contains(groupYellow)) {
                throw new InvalidAttributeValueException("EqualsFilter not return securityEnabled.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        } finally {
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.dispose();
        }
    }


    @Test
    public void startsWithFilterTestAccountObject() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", false));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "Pink@TENANT.onmicrosoft.com"));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        attributesAccount.add(AttributeBuilder.build("city", "Senec"));
        attributesAccount.add(AttributeBuilder.build("country", "Slovakia"));
        attributesAccount.add(AttributeBuilder.build("department", "Departmant of driver"));
        attributesAccount.add(AttributeBuilder.build("employeeId", "9968"));
        attributesAccount.add(AttributeBuilder.build("jobTitle", "Dr."));
        ArrayList<String> otherMails = new ArrayList<>();
        otherMails.add("mysupermail@gmail.com");
        otherMails.add("midpoint.mail@evo.com");
        attributesAccount.add(AttributeBuilder.build("otherMails", otherMails));
        attributesAccount.add(AttributeBuilder.build("state", "California"));
        attributesAccount.add(AttributeBuilder.build("surname", "Smith"));
        attributesAccount.add(AttributeBuilder.build("usageLocation", "US"));
        attributesAccount.add(AttributeBuilder.build("givenName", "George"));


        Uid firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        Set<Attribute> attributesAccount1 = new HashSet<>();
        attributesAccount1.add(AttributeBuilder.build("__ENABLE__", false));
        attributesAccount1.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount1.add(AttributeBuilder.build("displayName", "Green"));
        attributesAccount1.add(AttributeBuilder.build("mailNickname", "Green"));
        attributesAccount1.add(AttributeBuilder.build("__NAME__", "green@TENANT.onmicrosoft.com"));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount1.add(AttributeBuilder.build("__PASSWORD__", pass1));
        attributesAccount1.add(AttributeBuilder.build("city", "Bratislava"));
        attributesAccount1.add(AttributeBuilder.build("country", "USA"));
        attributesAccount1.add(AttributeBuilder.build("department", "IT"));
        attributesAccount1.add(AttributeBuilder.build("employeeId", "7779"));
        attributesAccount1.add(AttributeBuilder.build("givenName", "Peter The Biggest"));
        attributesAccount1.add(AttributeBuilder.build("jobTitle", "Human Resources"));
        attributesAccount1.add(AttributeBuilder.build("state", "Texas"));
        attributesAccount1.add(AttributeBuilder.build("surname", "Kovac"));
        attributesAccount1.add(AttributeBuilder.build("userType", "Member"));

        Uid secondUser = msGraphConnector.create(objectClassAccount, attributesAccount1, options);

        final ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        SearchResultsHandler handlerAccount = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsAccount.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };


        AttributeFilter startsWithFilterCity;
        startsWithFilterCity = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("city", "S"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterCity, handlerAccount, options);
        ArrayList<Uid> listUidCity = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidCity.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user city " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidCity.contains(firstUser) || listUidCity.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return city.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterCountry;
        startsWithFilterCountry = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("country", "S"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterCountry, handlerAccount, options);
        ArrayList<Uid> listUidCountry = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidCountry.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user country " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidCountry.contains(firstUser) || listUidCountry.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return country.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterDepartment;
        startsWithFilterDepartment = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("department", "d"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterDepartment, handlerAccount, options);
        ArrayList<Uid> listUidDepartment = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidDepartment.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user department " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidDepartment.contains(firstUser) || listUidDepartment.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return department.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterDisplayName;
        startsWithFilterDisplayName = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("displayName", "p"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterDisplayName, handlerAccount, options);
        ArrayList<Uid> listUidDisplayName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidDisplayName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user displayName " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidDisplayName.contains(firstUser) || listUidDisplayName.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return displayName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterEmployeeId;
        startsWithFilterEmployeeId = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("employeeId", "9"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterEmployeeId, handlerAccount, options);
        ArrayList<Uid> listUidEmployeeId = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidEmployeeId.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user employeeId " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidEmployeeId.contains(firstUser) || listUidEmployeeId.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return employeeId.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterGivenName;
        startsWithFilterGivenName = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("givenName", "G"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterGivenName, handlerAccount, options);
        ArrayList<Uid> listUidGivenName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidGivenName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user givenName " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidGivenName.contains(firstUser) || listUidGivenName.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return givenName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterJobTitle;
        startsWithFilterJobTitle = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("jobTitle", "Dr"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterJobTitle, handlerAccount, options);
        ArrayList<Uid> listUidJobTitle = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidJobTitle.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user jobTitle" + obj.getUid().getUidValue());
        }

        try {
            if (!listUidJobTitle.contains(firstUser) || listUidJobTitle.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return jobTitle.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter startsWithFilterMailNickname;
        startsWithFilterMailNickname = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mailNickname", "P"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterMailNickname, handlerAccount, options);
        ArrayList<Uid> listUidMailNickname = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidMailNickname.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user mailNickname" + obj.getUid().getUidValue());
        }

        try {
            if (!listUidMailNickname.contains(firstUser) || listUidMailNickname.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return mailNickname.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterMail;
        startsWithFilterMail = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("otherMails", "m"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterMail, handlerAccount, options);
        ArrayList<Uid> listUidMail = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidMail.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user otherMails " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidMail.contains(firstUser) || listUidMail.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return otherMails.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterState;
        startsWithFilterState = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("state", "California"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterState, handlerAccount, options);
        ArrayList<Uid> listUidState = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidState.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user state " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidState.contains(firstUser) || listUidState.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return state.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterSurname;
        startsWithFilterSurname = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("surname", "Smith"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterSurname, handlerAccount, options);
        ArrayList<Uid> listUidSurname = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidSurname.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user surname " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidSurname.contains(firstUser) || listUidSurname.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return surname.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterUsageLocation;
        startsWithFilterUsageLocation = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("usageLocation", "US"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterUsageLocation, handlerAccount, options);
        ArrayList<Uid> listUidUsageLocation = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidUsageLocation.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user usageLocation " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidUsageLocation.contains(firstUser) || listUidUsageLocation.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return usageLocation.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterName;
        startsWithFilterName = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build(Name.NAME, "p"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, startsWithFilterName, handlerAccount, options);
        ArrayList<Uid> listUidName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD user name - userPrincipalName " + obj.getUid().getUidValue());
        }

        try {
            if (!listUidName.contains(firstUser) || listUidName.contains(secondUser)) {
                throw new InvalidAttributeValueException("StartsWithFilter not return name - userPrincipalName.");
            }
        } finally {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }
    }


    @Test
    public void startsWithTestGroup() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("__NAME__", "Yellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "Yellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("groupTypes", "Unified"));

        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("__NAME__", "Blue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "Blue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);


        final ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        SearchResultsHandler handlerAccount = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsAccount.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };


        AttributeFilter startsWithFilterDisplayName;
        startsWithFilterDisplayName = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("__NAME__", "Y"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, startsWithFilterDisplayName, handlerAccount, options);
        ArrayList<Uid> listUidDisplayName = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidDisplayName.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group displayName " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidDisplayName.contains(groupYellow) || listUidDisplayName.contains(groupBlue)) {
                throw new InvalidAttributeValueException("EqualsFilter not return displayName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterMail;
        startsWithFilterMail = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mail", "Y"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, startsWithFilterMail, handlerAccount, options);
        ArrayList<Uid> listUidMail = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidMail.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group mail " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidMail.contains(groupYellow) || listUidMail.contains(groupBlue)) {
                throw new InvalidAttributeValueException("EqualsFilter not return mail.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }

        AttributeFilter startsWithFilterMailNickname;
        startsWithFilterMailNickname = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mailNickname", "Y"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, startsWithFilterMailNickname, handlerAccount, options);
        ArrayList<Uid> listUidMailNickname = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUidMailNickname.add((Uid) obj.getAttributeByName(Uid.NAME));
            System.out.println("ADD group mailNickname " + obj.getUid().getUidValue());

        }
        try {
            if (!listUidMailNickname.contains(groupYellow) || listUidMailNickname.contains(groupBlue)) {
                throw new InvalidAttributeValueException("EqualsFilter not return mailNickname.");
            }
        } finally {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }


    }
}
