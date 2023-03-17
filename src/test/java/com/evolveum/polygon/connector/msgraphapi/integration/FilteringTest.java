package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import static com.evolveum.polygon.connector.msgraphapi.RoleProcessing.ROLE_NAME;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class FilteringTest extends BasicConfigurationForTests {

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
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "RED@" + tenantId));
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
        attributesAccount.add(AttributeBuilder.build("manager.id", "f7febd3d-8123-4abb-b38e-4a3a2ab1080d"));
        attributesAccount.add(AttributeBuilder.build("mail", "Pink@" + tenantId));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink@" + tenantId));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));

        Uid firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        Set<Attribute> attributesAccount1 = new HashSet<>();
        attributesAccount1.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount1.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount1.add(AttributeBuilder.build("displayName", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("mail", "PinkAndGreen@" + tenantId));
        attributesAccount1.add(AttributeBuilder.build("mailNickname", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen@" + tenantId));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount1.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser = msGraphConnector.create(objectClassAccount, attributesAccount1, options);


        AttributeFilter containsFilterAccount;
        containsFilterAccount = (StartsWithFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "Pink"));

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
        equalsFilterAccount1 = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build(Uid.NAME, "Pink@" + tenantId));
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
}
