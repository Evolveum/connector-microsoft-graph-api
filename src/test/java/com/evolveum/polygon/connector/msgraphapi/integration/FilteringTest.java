package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import static com.evolveum.polygon.connector.msgraphapi.RoleProcessing.ROLE_NAME;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class FilteringTest extends BasicConfigurationForTests {

    private static final Log LOG = Log.getLog(FilteringTest.class);

    @Test(priority = 15)
    public void findAccountEqualsUidWithLastLogin() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "RED"));
        attributesAccount.add(AttributeBuilder.build("mail", "RED@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "RED"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "RED@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));

        Uid tstUsr = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        AttributeFilter equalsFilterAccount;
        equalsFilterAccount = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, tstUsr.getUidValue()));
        TestSearchResultsHandler handler = getResultHandler();

        queryWaitAndRetry(objectClassAccount, equalsFilterAccount, handler, options, tstUsr);

        ArrayList<ConnectorObject> results = handler.getResult();

        if (results.isEmpty()) {

            Assert.fail("Result set empty");
        } else {
            for (ConnectorObject o : results) {

                if (o.getUid().equals(tstUsr)) {

                    deleteWaitAndRetry(ObjectClass.ACCOUNT, tstUsr, options);
                    return;
                }
            }
        }
        Assert.fail("Object not among result set");
    }


    @Test(priority = 20)
    public void filteringTestAccountObjectClass() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

         firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False netId is invalid error again, ignoring.");
                firstUser = fetchUidAtFalseValidationException("Pink@" + domain);
            }
        }
        Set<Attribute> attributesAccount1 = new HashSet<>();
        attributesAccount1.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount1.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount1.add(AttributeBuilder.build("displayName", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("mail", "PinkAndGreen@" + domain));
        attributesAccount1.add(AttributeBuilder.build("mailNickname", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount1.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser = msGraphConnector.create(objectClassAccount, attributesAccount1, options);


        AttributeFilter containsFilterAccount;
        containsFilterAccount = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "Pink"));

        List<Uid> returnedObjectUid;
        if (firstUser != null && secondUser != null) {
            returnedObjectUid = Arrays.asList(firstUser, secondUser);
        } else {

            throw new InvalidAttributeValueException("No UID returned after account creation.");
        }


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, containsFilterAccount, handlerAccount, options, returnedObjectUid);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(firstUser) || !listUid.contains(secondUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("ContainsFilter not return both user.");
        }


        AttributeFilter equalsFilterAccount1;
        equalsFilterAccount1 = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, "Pink@" + domain));
        resultsAccount.clear();

        handlerAccount = getResultHandler();
        queryWaitAndRetry(objectClassAccount, equalsFilterAccount1, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        if (!resultsAccount.isEmpty()) {
            if (!resultsAccount.get(0).getAttributes().contains(firstUser)) {
                /// Best effort cleanup
                deleteWaitAndRetry(objectClassAccount, firstUser, options);
                deleteWaitAndRetry(objectClassAccount, secondUser, options);
                Assert.fail("EqualsFilter not return searched user. Search with userPrincipalName.");
            }
        } else {

            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("EqualsFilter not return searched user. Search with userPrincipalName.");
        }


        AttributeFilter equelsFilterAccount2;
        equelsFilterAccount2 = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("displayName", "Pink"));
        resultsAccount.clear();

        handlerAccount = getResultHandler();
        queryWaitAndRetry(objectClassAccount, equelsFilterAccount2, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid1 = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid1.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid1.contains(firstUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("EqualsFilter not return displayName.");
        }

        //cleanup
        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }

    @Test(priority = 21)
    public void filteringTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "GroupYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("displayName", "GroupXBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);

        AttributeFilter containsFilterGroup;
        containsFilterGroup = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "GroupX"));

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();

        List<Uid> returnedObjectUid = null;
        if (groupBlue != null && groupYellow != null) {
            returnedObjectUid = Arrays.asList(groupBlue, groupYellow);
        } else {

            throw new InvalidAttributeValueException("No UID returned after account creation.");
        }

        queryWaitAndRetry(objectClassGroup, containsFilterGroup, handlerGroup, options, returnedObjectUid);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupBlue) || !listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupBlue, options);
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("ContainsFilter not return both group.");
        }


        AttributeFilter equelsFilterAccount2;
        equelsFilterAccount2 = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("mailNickname", "GroupYellow"));
        resultsGroup.clear();
        handlerGroup = getResultHandler();
        queryWaitAndRetry(objectClassGroup, equelsFilterAccount2, handlerGroup, options, returnedObjectUid);
        resultsGroup = handlerGroup.getResult();

        ArrayList<Uid> listUid1 = new ArrayList<>();
        for (ConnectorObject obj : resultsGroup) {
            listUid1.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid1.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupBlue, options);
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("EqualsFilter not return mailNickname.");
        }

        deleteWaitAndRetry(objectClassGroup, groupBlue, options);
        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }



    @Test(priority = 31)
    public void equalsFilterTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("groupTypes", "Unified"));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);


        AttributeFilter equalsFilter;
        equalsFilter = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("mail",
                "diffusion-x"+"@"+domain));

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();


        queryWaitAndRetry(objectClassGroup, equalsFilter, handlerGroup, options, groupYellow);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("ContainsFilter did not return group: "+ groupYellow);
        }

        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }
    @Test(priority = 32)
    public void startsWithFilterTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("groupTypes", "Unified"));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);


        AttributeFilter startsWithFilterGroup;
        startsWithFilterGroup = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mail",
                "diffusion"));

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();


        queryWaitAndRetry(objectClassGroup, startsWithFilterGroup, handlerGroup, options, groupYellow);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("ContainsFilter did not return group: "+ groupYellow);
        }

        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }

    @Test(priority = 33)
    public void notStartsWithFilterTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("groupTypes", "Unified"));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);


        AttributeFilter startsWithFilterGroup;
        startsWithFilterGroup = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mail",
                "diffusion"));

        NotFilter notFilter;

        notFilter = (NotFilter) FilterBuilder.not(startsWithFilterGroup);

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();


        queryWaitAndRetry(objectClassGroup, notFilter, handlerGroup, options, groupYellow);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("Not filter did return group: "+ groupYellow);
        }

        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }


    @Test(priority = 34)
    public void containsFilterTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "diffusion-x"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("groupTypes", "Unified"));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);


        AttributeFilter containsFilter;
        containsFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME,
                "diffusion"));

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();


        queryWaitAndRetry(objectClassGroup, containsFilter, handlerGroup, options, groupYellow);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("Not filter did return group: "+ groupYellow);
        }

        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }

    @Test(priority = 35)
    public void compositeAndSearchTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("displayName", "GroupXBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupXBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);

        AttributeFilter containsFilterGroupB;
        containsFilterGroupB = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "GroupX"));

        AttributeFilter containsFilterGroupY;
        containsFilterGroupY = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("mailNickname", "Group"));

        AndFilter andFilter = (AndFilter) FilterBuilder.and(containsFilterGroupB, containsFilterGroupY);

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();

        List<Uid> returnedObjectUid = null;
        if (groupBlue != null && groupYellow != null) {
            returnedObjectUid = Arrays.asList(groupBlue, groupYellow);
        } else {

            throw new InvalidAttributeValueException("No UID returned after account creation.");
        }

        queryWaitAndRetry(objectClassGroup, andFilter, handlerGroup, options, returnedObjectUid);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupBlue) || !listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupBlue, options);
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("ContainsFilter not return both group.");
        }


        deleteWaitAndRetry(objectClassGroup, groupBlue, options);
        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }

    @Test(priority = 35)
    public void compositeOrSearchTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("displayName", "GroupXBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupXBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);

        AttributeFilter containsFilterGroupB;
        containsFilterGroupB = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "GroupXBlue"));

        AttributeFilter containsFilterGroupY;
        containsFilterGroupY = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("mailNickname", "GroupXYellow"));

        OrFilter orFilter = (OrFilter) FilterBuilder.or(containsFilterGroupB, containsFilterGroupY);

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();

        List<Uid> returnedObjectUid = null;
        if (groupBlue != null && groupYellow != null) {
            returnedObjectUid = Arrays.asList(groupBlue, groupYellow);
        } else {

            throw new InvalidAttributeValueException("No UID returned after account creation.");
        }

        queryWaitAndRetry(objectClassGroup, orFilter, handlerGroup, options, returnedObjectUid);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupBlue) || !listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupBlue, options);
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("ContainsFilter not return both group.");
        }


        deleteWaitAndRetry(objectClassGroup, groupBlue, options);
        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }

    @Test(priority = 35)
    public void compositeAndFilteringTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);


        AttributeFilter equals;
        equals = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("displayName", "GroupXYellow"));

        AttributeFilter startsW;
        startsW = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mailNickname", "GroupX"));

        AndFilter andFilter = (AndFilter) FilterBuilder.and(equals, startsW);

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();


        queryWaitAndRetry(objectClassGroup, andFilter, handlerGroup, options, groupYellow);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupYellow, options);
            Assert.fail("ContainsFilter not return both group.");
        }


        deleteWaitAndRetry(objectClassGroup, groupYellow, options);
    }

    @Test(priority = 36)
    public void compositeFilteringAndSearchTestGroupObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("displayName", "GroupXBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupXBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);

        AttributeFilter containsFilter;
        containsFilter = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "XBlue"));

        AttributeFilter startsWithFilter;
        startsWithFilter = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mailNickname", "Group"));

        AndFilter andFilter = (AndFilter) FilterBuilder.and(containsFilter, startsWithFilter);

        ArrayList<ConnectorObject> resultsGroup;
        TestSearchResultsHandler handlerGroup = getResultHandler();


        queryWaitAndRetry(objectClassGroup, andFilter, handlerGroup, options, groupBlue);
        ArrayList<Uid> listUid = new ArrayList<>();
        resultsGroup = handlerGroup.getResult();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupBlue)) {
            deleteWaitAndRetry(objectClassGroup, groupBlue, options);
            Assert.fail("ContainsFilter not return both group.");
        }


        deleteWaitAndRetry(objectClassGroup, groupBlue, options);
    }
    @Test(priority = 22)
    public void filteringRoleObjectClass() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultRoleOperationOptions();

        ObjectClass objectClassRole = new ObjectClass(ROLE_NAME);

        AttributeFilter containsFilterRole;
        containsFilterRole = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", roleWhichExistsInTenantDisplayName));

        TestSearchResultsHandler handlerRole = getResultHandler();
        
        msGraphConnector.executeQuery(objectClassRole, containsFilterRole, handlerRole, options);
        
        ArrayList<ConnectorObject> resultsRole = handlerRole.getResult();
        Assert.assertTrue(!resultsRole.isEmpty());
    }

    @Test(priority = 23)
    public void filteringGroupContainsAllValues() throws Exception {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions goptions = getDefaultGroupOperationOptions();

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        OperationOptions aoptions = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "GroupXYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "GroupYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, goptions);



        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;

        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, aoptions);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False netId is invalid error again, ignoring.");
                firstUser = fetchUidAtFalseValidationException("Pink@" + domain);
            }
        }

        Set<AttributeDelta> attributesUpdateGroup = new HashSet<>();

        attributesUpdateGroup.add(AttributeDeltaBuilder.build("members", CollectionUtil.newList(firstUser.getUidValue()),null));
        TestSearchResultsHandler handlerGroup = getResultHandler();
        queryWaitAndRetry(objectClassGroup, new EqualsFilter(AttributeBuilder.build(Uid.NAME, "GroupXYellow")),
                handlerGroup , goptions, groupYellow);

        msGraphConnector.updateDelta(ObjectClass.GROUP, groupYellow, attributesUpdateGroup, goptions);


        AttributeFilter containsAll;
        containsAll = (ContainsAllValuesFilter) FilterBuilder.containsAllValues(AttributeBuilder.build("members", firstUser.getUidValue()));

        ArrayList<ConnectorObject> resultsGroup;
        handlerGroup = getResultHandler();

        queryWaitAndRetry(objectClassGroup, containsAll, handlerGroup, goptions, groupYellow);
        resultsGroup = handlerGroup.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsGroup) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }


        if (!listUid.contains(groupYellow)) {
            deleteWaitAndRetry(objectClassGroup, groupYellow, goptions);
            Assert.fail("Contains all values filter did not return group");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, aoptions);
        deleteWaitAndRetry(objectClassGroup, groupYellow, goptions);
    }

    @Test(priority = 24)
    public void filteringAccountCompositeFilterAndSearch() throws Exception {


        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink-Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink-Dev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }
        Set<Attribute> attributesAccountPg = new HashSet<>();
        attributesAccountPg.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccountPg.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccountPg.add(AttributeBuilder.build("displayName", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("mail", "PinkAndGreen-support@" + domain));
        attributesAccountPg.add(AttributeBuilder.build("mailNickname", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("department", "IT Support"));
        attributesAccountPg.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen-support@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccountPg.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser =null;


        try{

            secondUser = msGraphConnector.create(objectClassAccount, attributesAccountPg, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                secondUser = fetchUidAtFalseValidationException("PinkAndGreen-support@" + domain);
            }
        }

        AttributeFilter endsWith;
        endsWith = (EndsWithFilter) FilterBuilder.endsWith(AttributeBuilder.build("mail",
                "support@"+domain));

        AttributeFilter contains;
        contains = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME, "Pink"));


        AttributeFilter negatedContains;
        negatedContains = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("userPrincipalName", "Blue"));

        AttributeFilter containsEx;
        containsEx = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("department", "IT"));


        NotFilter not = (NotFilter) FilterBuilder.not(negatedContains);

        OrFilter or = (OrFilter) FilterBuilder.or(not,contains);

        AndFilter andEx = (AndFilter) FilterBuilder.and(endsWith,containsEx);

        AndFilter and = (AndFilter) FilterBuilder.and(or,andEx);


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, and, handlerAccount, options, secondUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(secondUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("Composite filter + search did not return user");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }



    @Test(priority = 25)
    public void filteringAccountCompositeFilter() throws Exception {


        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink-Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink-Dev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }
        Set<Attribute> attributesAccountPg = new HashSet<>();
        attributesAccountPg.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccountPg.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccountPg.add(AttributeBuilder.build("displayName", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("mail", "PinkAndGreen-support@" + domain));
        attributesAccountPg.add(AttributeBuilder.build("mailNickname", "PinkAndGreen-support"));
        attributesAccount.add(AttributeBuilder.build("department", "Support"));
        attributesAccountPg.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen-support@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccountPg.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser =null;


        try{

            secondUser = msGraphConnector.create(objectClassAccount, attributesAccountPg, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                secondUser = fetchUidAtFalseValidationException("PinkAndGreen-support@" + domain);
            }
        }

        AttributeFilter endsWith;
        endsWith = (EndsWithFilter) FilterBuilder.endsWith(AttributeBuilder.build(Name.NAME,
                "Dev@"+domain));

        AttributeFilter sWfilter;
        sWfilter = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build(Name.NAME, "Pink"));

        AttributeFilter negatedEquals;
        negatedEquals = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("department", "Support"));

        NotFilter not = (NotFilter) FilterBuilder.not(negatedEquals);

        OrFilter or = (OrFilter) FilterBuilder.or(not,sWfilter);

        AndFilter and = (AndFilter) FilterBuilder.and(or,endsWith);


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, and, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(firstUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("Composite filter did not return both users");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }

    @Test(priority = 25)
    public void filteringAccountCompositeSearch() throws Exception {


        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development team"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "PinkDev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }
        Set<Attribute> attributesAccountPg = new HashSet<>();
        attributesAccountPg.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccountPg.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccountPg.add(AttributeBuilder.build("displayName", "PinkAndGreen support"));
        attributesAccountPg.add(AttributeBuilder.build("mail", "PinkAndGreen-support@" + domain));
        attributesAccountPg.add(AttributeBuilder.build("mailNickname", "PinkAndGreen-support"));
        attributesAccount.add(AttributeBuilder.build("department", "Support team"));
        attributesAccountPg.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen-support@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccountPg.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser =null;


        try{

            secondUser = msGraphConnector.create(objectClassAccount, attributesAccountPg, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                secondUser = fetchUidAtFalseValidationException("PinkAndGreen-support@" + domain);
            }
        }

        AttributeFilter contains;
        contains = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME,
                "Pink"));

        AttributeFilter containsTwo;
        containsTwo = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "support"));

        AttributeFilter containsNegated;
        containsNegated = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "dev"));

        NotFilter not = (NotFilter) FilterBuilder.not(containsNegated);

        OrFilter or = (OrFilter) FilterBuilder.or(not,containsTwo);

        AndFilter and = (AndFilter) FilterBuilder.and(or,contains);


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, and, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(secondUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("Composite search did not return both users");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }


    @Test(priority = 26)
    public void filteringAccountFilterGreaterOrEqual() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development team"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "PinkDev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }

        AttributeFilter ge;
        ge = (GreaterThanOrEqualFilter) FilterBuilder.greaterThanOrEqualTo(AttributeBuilder.build("createdDateTime",
                "2021-11-01T00:00:00Z"));

        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, ge, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(firstUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            Assert.fail("Greater or equal filter did not return user");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);

    }

    @Test(priority = 27)
    public void filteringAccountFilterLessOrEqual() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development team"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "PinkDev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }

        AttributeFilter le;
        // TODO might need to update time value in the future :)
        le = (LessThanOrEqualFilter) FilterBuilder.lessThanOrEqualTo(AttributeBuilder.build("createdDateTime",
                "2100-11-01T00:00:00Z"));

        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, le, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(firstUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            Assert.fail("Less or equal filter did not return user");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);

    }

    @Test(priority = 28)
    public void filteringAccountFilterNotEqual() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development team"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "PinkDev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }

        AttributeFilter eq;

        eq = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Name.NAME,
                "Blue@"+domain));

        NotFilter not = (NotFilter) FilterBuilder.not(eq);

        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, not, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(firstUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            Assert.fail("Not equal filter did not return user");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);

    }

    @Test(priority = 29)
    public void filteringAccountFilterStartsWith() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development team"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "PinkDev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }

        AttributeFilter sw;

        sw = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build(Name.NAME,
                "PinkDev"));

        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, sw, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(firstUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            Assert.fail("Starts with filter did not return user.");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);

    }

    @Test(priority = 30)
    public void filteringAccountFilterEndsWith() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development team"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "PinkDev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }

        AttributeFilter ew;

        ew = (EndsWithFilter) FilterBuilder.endsWith(AttributeBuilder.build(Name.NAME,
                "Dev@"+domain));

        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, ew, handlerAccount, options, firstUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(firstUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            Assert.fail("Ends with filter did not return user.");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);

    }

    @Test(priority = 37)
    public void filteringAccountCompositeSearchAndAggregation() throws Exception {


        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink-Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink-Dev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }
        Set<Attribute> attributesAccountPg = new HashSet<>();
        attributesAccountPg.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccountPg.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccountPg.add(AttributeBuilder.build("displayName", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("mail", "PinkAndGreen-support@" + domain));
        attributesAccountPg.add(AttributeBuilder.build("mailNickname", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("department", "IT Support"));
        attributesAccountPg.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen-support@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccountPg.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser =null;


        try{

            secondUser = msGraphConnector.create(objectClassAccount, attributesAccountPg, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                secondUser = fetchUidAtFalseValidationException("PinkAndGreen-support@" + domain);
            }
        }

        AttributeFilter co1;
        co1 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("mail",
                "PinkAndGreen"));

        AttributeFilter co2;
        co2 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("mail",
                "Pink"));

        AttributeFilter co3;
        co3 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME, "Pink"));


        AttributeFilter co4;
        co4 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("department", "IT"));


        AndFilter first = (AndFilter) FilterBuilder.and(co1,co2);

        AndFilter second = (AndFilter) FilterBuilder.and(co3,co4);

        AndFilter third = (AndFilter) FilterBuilder.and(first,second);


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, third, handlerAccount, options, secondUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(secondUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("Composite filter + search did not return user");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }

    @Test(priority = 38)
    public void filteringAccountCompositeSearchOrAggregation() throws Exception {


        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink-Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink-Dev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }
        Set<Attribute> attributesAccountPg = new HashSet<>();
        attributesAccountPg.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccountPg.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccountPg.add(AttributeBuilder.build("displayName", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("mail", "PinkAndGreen-support@" + domain));
        attributesAccountPg.add(AttributeBuilder.build("mailNickname", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("department", "IT Support"));
        attributesAccountPg.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen-support@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccountPg.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser =null;


        try{

            secondUser = msGraphConnector.create(objectClassAccount, attributesAccountPg, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                secondUser = fetchUidAtFalseValidationException("PinkAndGreen-support@" + domain);
            }
        }

        AttributeFilter co1;
        co1 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("mail",
                "PinkAndGreen"));

        AttributeFilter co2;
        co2 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("mail",
                "Pink"));

        AttributeFilter co3;
        co3 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME, "Pink"));


        AttributeFilter co4;
        co4 = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("department", "IT"));

        OrFilter first = (OrFilter) FilterBuilder.or(co1,co2);

        OrFilter second = (OrFilter) FilterBuilder.or(co3,co4);

        OrFilter third = (OrFilter) FilterBuilder.or(first,second);


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, third, handlerAccount, options, secondUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(secondUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("Composite filter + search did not return user");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }

    @Test(priority = 39)
    public void filteringAccountAggregateCompositeFilterAndSearch() throws Exception {


        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink-Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink-Dev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }
        Set<Attribute> attributesAccountPg = new HashSet<>();
        attributesAccountPg.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccountPg.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccountPg.add(AttributeBuilder.build("displayName", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("mail", "PinkAndGreen-support@" + domain));
        attributesAccountPg.add(AttributeBuilder.build("mailNickname", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("department", "IT Support"));
        attributesAccountPg.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen-support@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccountPg.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser =null;


        try{

            secondUser = msGraphConnector.create(objectClassAccount, attributesAccountPg, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                secondUser = fetchUidAtFalseValidationException("PinkAndGreen-support@" + domain);
            }
        }

        AttributeFilter endsWith;
        endsWith = (EndsWithFilter) FilterBuilder.endsWith(AttributeBuilder.build("mail",
                "support@"+domain));

        AttributeFilter sWith;
        sWith = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("mail",
                "Pink@"+domain));

        AttributeFilter eq;
        eq = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("department",
                "IT Support"));

        AttributeFilter contains;
        contains = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME, "Pink"));


       // AttributeFilter negatedContains;
       // negatedContains = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("userPrincipalName", "Blue"));

        AttributeFilter containsEx;
        containsEx = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("department", "IT"));


        //NotFilter not = (NotFilter) FilterBuilder.not(negatedContains);

        OrFilter or = (OrFilter) FilterBuilder.or(sWith,endsWith);

        AndFilter first = (AndFilter) FilterBuilder.and(or,contains);

        AndFilter second = (AndFilter) FilterBuilder.and(first,eq);

        AndFilter third = (AndFilter) FilterBuilder.and(second,containsEx);


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, third, handlerAccount, options, secondUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(secondUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("Composite filter + search did not return user");
        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }

    @Test(priority = 40)
    public void filteringCustomAggregateAccountCompositeFilterAndSearch() throws Exception {


        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = getDefaultAccountOperationOptions();

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink-Dev"));
        // attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink-Dev@" + domain));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink-Dev"));
        attributesAccount.add(AttributeBuilder.build("department", "Development"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink-Dev@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        Uid firstUser = null;
        try{

            firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                firstUser = fetchUidAtFalseValidationException("Pink-Dev@" + domain);
            }
        }
        Set<Attribute> attributesAccountPg = new HashSet<>();
        attributesAccountPg.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccountPg.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccountPg.add(AttributeBuilder.build("displayName", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("mail", "PinkAndGreen-support@" + domain));
        attributesAccountPg.add(AttributeBuilder.build("mailNickname", "PinkAndGreen-support"));
        attributesAccountPg.add(AttributeBuilder.build("department", "IT Support"));
        attributesAccountPg.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen-support@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccountPg.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser =null;


        try{

            secondUser = msGraphConnector.create(objectClassAccount, attributesAccountPg, options);

        } catch (InvalidAttributeValueException e){
            if (e.getLocalizedMessage().contains("Property netId is invalid")){

                LOG.warn("False 'netId is invalid error' again, ignoring. Executing search to retrieve UID.");
                secondUser = fetchUidAtFalseValidationException("PinkAndGreen-support@" + domain);
            }
        }

        ///

        AttributeFilter ew;
        ew = (EndsWithFilter) FilterBuilder.endsWith(AttributeBuilder.build("userPrincipalName", "Dev@" + domain));

        NotFilter noew;
        noew=(NotFilter) FilterBuilder.not(ew);

        AttributeFilter sw;
        sw = (StartsWithFilter) FilterBuilder.startsWith(AttributeBuilder.build("userPrincipalName", "Pink-Dev"));

        NotFilter nosw;
        nosw=(NotFilter) FilterBuilder.not(sw);

        AttributeFilter containsDeptF;
        containsDeptF = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("department", "Devel"));

        NotFilter noDeptF;
        noDeptF=(NotFilter) FilterBuilder.not(containsDeptF);

        AttributeFilter containsDeptB;
        containsDeptB = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("department", "Development"));

        NotFilter noDeptB;
        noDeptB=(NotFilter) FilterBuilder.not(containsDeptB);

        AttributeFilter containsA;
        containsA = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME, "PinkAndGreen"));

        AttributeFilter containsB;
        containsB = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build(Name.NAME, "Pink"));


        AndFilter andNeg = (AndFilter) FilterBuilder.and(noew,nosw);

        AndFilter andContNegF = (AndFilter) FilterBuilder.and(andNeg,noDeptF);

        AndFilter andContNegB = (AndFilter) FilterBuilder.and(andContNegF,noDeptB);

        AndFilter andCont = (AndFilter) FilterBuilder.and(containsA,containsB);


        AndFilter finalAnd = (AndFilter) FilterBuilder.and(andContNegB,andCont);
        ///


        ArrayList<ConnectorObject> resultsAccount;
        TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(objectClassAccount, finalAnd, handlerAccount, options, secondUser);
        resultsAccount = handlerAccount.getResult();

        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        if (!listUid.contains(secondUser)) {
            /// Best effort cleanup
            deleteWaitAndRetry(objectClassAccount, firstUser, options);
            deleteWaitAndRetry(objectClassAccount, secondUser, options);
            Assert.fail("Composite filter + search did not return user");

        }

        deleteWaitAndRetry(objectClassAccount, firstUser, options);
        deleteWaitAndRetry(objectClassAccount, secondUser, options);

    }

   private Uid fetchUidAtFalseValidationException(String upn) throws InterruptedException {


        ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        AttributeFilter equalsFilterAccount;
        equalsFilterAccount = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, upn));

       TestSearchResultsHandler handlerAccount = getResultHandler();

        queryWaitAndRetry(ObjectClass.ACCOUNT,(EqualsFilter) equalsFilterAccount, handlerAccount, getDefaultAccountOperationOptions());
        resultsAccount = handlerAccount.getResult();

        if (!resultsAccount.isEmpty()) {
            Iterator<Attribute> it = resultsAccount.get(0).getAttributes().iterator();

            while(it.hasNext()){
             Attribute attr = it.next();

             if (attr.getName().equals(Uid.NAME)){
                 return new Uid(attr.getValue().get(0).toString());
             }

            }
        }

        throw new ConnectorException("Object not found");
    }
}
