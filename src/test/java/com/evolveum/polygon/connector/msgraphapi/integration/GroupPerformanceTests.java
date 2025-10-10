package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;
import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

import java.util.*;

//// TODO Exclude
public class GroupPerformanceTests extends BasicConfigurationForTests {

    private Uid groupTestUser;
    private Uid groupTestUser1;
    private Uid groupTestUser2;
    private Uid groupTestUser3;

    private Set<Uid> groupsUid = new HashSet<>();

    @Test(priority = 30)
    public void Create500Groups() {
        MSGraphConnector gitlabRestConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        for (int i = 0; i < 500; i++) {

            Set<Attribute> attributesCreatedGroup = new HashSet<>();
            attributesCreatedGroup.add(AttributeBuilder.build("displayName", "testGroupdisplayName" + i));
            attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
            attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "testGroupmailNickname" + i));
            attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));

            gitlabRestConnector.init(conf);
            Uid groupUid = gitlabRestConnector.create(objectClassGroup, attributesCreatedGroup, options);
            gitlabRestConnector.dispose();
            groupsUid.add(groupUid);

        }

    }

    @Test(priority = 31)
    public void Update500GroupsTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        int i = 0;
        for (Uid groupUid : groupsUid) {

            Set<AttributeDelta> attributesUpdateGroup = new HashSet<>();

            AttributeDeltaBuilder attr = new AttributeDeltaBuilder();
            attr.setName("description");
            attr.addValueToReplace("this is the " + i + " description update");
            attributesUpdateGroup.add(attr.build());
            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, groupUid, attributesUpdateGroup, options);
            msGraphConnector.dispose();
            i++;
        }
    }


    @Test(priority = 32)
    public void CreateUserAndAddUserToEachGroupTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Yellow"));
        attributesAccount.add(AttributeBuilder.build("mail", "Yellow@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Yellow"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Yellow@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));


        msGraphConnector.init(conf);
        groupTestUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        msGraphConnector.dispose();

        for (Uid groupUid : groupsUid) {
            Set<AttributeDelta> attrAdd = new HashSet<>();
            AttributeDeltaBuilder attr = new AttributeDeltaBuilder();
            attr.setName("members");
            attr.addValueToAdd(groupTestUser.getUidValue());
            attrAdd.add(attr.build());
            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, groupUid, attrAdd, options);
            msGraphConnector.dispose();
        }
    }

    @Test(priority = 33)
    public void CreateUsersAndAddUserToEachGroupAsOwnerTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount3 = new HashSet<>();
        attributesAccount3.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount3.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount3.add(AttributeBuilder.build("displayName", "Blue"));
        attributesAccount3.add(AttributeBuilder.build("mail", "Blue@example.com"));
        attributesAccount3.add(AttributeBuilder.build("mailNickname", "Blue"));
        attributesAccount3.add(AttributeBuilder.build("userPrincipalName", "Blue@" + domain));
        GuardedString pass3 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount3.add(AttributeBuilder.build("__PASSWORD__", pass3));

        Set<Attribute> attributesAccount1 = new HashSet<>();
        attributesAccount1.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount1.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount1.add(AttributeBuilder.build("displayName", "Red"));
        attributesAccount1.add(AttributeBuilder.build("mail", "Red@example.com"));
        attributesAccount1.add(AttributeBuilder.build("mailNickname", "Red"));
        attributesAccount1.add(AttributeBuilder.build("userPrincipalName", "Red@" + domain));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount1.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Set<Attribute> attributesAccount2 = new HashSet<>();
        attributesAccount2.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount2.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount2.add(AttributeBuilder.build("displayName", "Black"));
        attributesAccount2.add(AttributeBuilder.build("mail", "Black@example.com"));
        attributesAccount2.add(AttributeBuilder.build("mailNickname", "Black"));
        attributesAccount2.add(AttributeBuilder.build("userPrincipalName", "Black@" + domain));
        GuardedString pass2 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount2.add(AttributeBuilder.build("__PASSWORD__", pass2));


        msGraphConnector.init(conf);
        groupTestUser3 = msGraphConnector.create(objectClassAccount, attributesAccount3, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        groupTestUser1 = msGraphConnector.create(objectClassAccount, attributesAccount1, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        groupTestUser2 = msGraphConnector.create(objectClassAccount, attributesAccount2, options);
        msGraphConnector.dispose();

        for (Uid groupUid : groupsUid) {
            Set<AttributeDelta> attrAdd = new HashSet<>();
            AttributeDeltaBuilder attr = new AttributeDeltaBuilder();
            attr.setName("owners");
            attr.addValueToAdd(groupTestUser.getUidValue());

            Set<AttributeDelta> attrAdd1 = new HashSet<>();
            AttributeDeltaBuilder attr1 = new AttributeDeltaBuilder();
            attr1.setName("owners");
            attr1.addValueToAdd(groupTestUser1.getUidValue());

            Set<AttributeDelta> attrAdd2 = new HashSet<>();
            AttributeDeltaBuilder attr2 = new AttributeDeltaBuilder();
            attr2.setName("owners");
            attr2.addValueToAdd(groupTestUser2.getUidValue());

            Set<AttributeDelta> attrAdd3 = new HashSet<>();
            AttributeDeltaBuilder attr3 = new AttributeDeltaBuilder();
            attr3.setName("owners");
            attr3.addValueToAdd(groupTestUser3.getUidValue());

            attrAdd.add(attr.build());
            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, groupUid, attrAdd, options);
            msGraphConnector.dispose();

            attrAdd1.add(attr1.build());
            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, groupUid, attrAdd1, options);
            msGraphConnector.dispose();

            attrAdd2.add(attr2.build());
            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, groupUid, attrAdd2, options);
            msGraphConnector.dispose();

            attrAdd3.add(attr3.build());
            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, groupUid, attrAdd3, options);
            msGraphConnector.dispose();
        }
    }

    //must be remain 1 owner
    @Test(priority = 34)
    public void RemoveOwnerFromEachGroup() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<AttributeDelta> attrAdd = new HashSet<>();
        AttributeDeltaBuilder attr = new AttributeDeltaBuilder();
        attr.setName("owners");
        attr.addValueToRemove(groupTestUser.getUidValue());
        attrAdd.add(attr.build());


        Set<AttributeDelta> attrAdd1 = new HashSet<>();
        AttributeDeltaBuilder attr1 = new AttributeDeltaBuilder();
        attr1.setName("owners");
        attr1.addValueToRemove(groupTestUser1.getUidValue());
        attrAdd1.add(attr1.build());

        Set<AttributeDelta> attrAdd2 = new HashSet<>();
        AttributeDeltaBuilder attr2 = new AttributeDeltaBuilder();
        attr2.setName("owners");
        attr2.addValueToRemove(groupTestUser2.getUidValue());
        attrAdd2.add(attr2.build());


        for (Uid group : groupsUid) {


            msGraphConnector.init(getConfiguration());
            msGraphConnector.updateDelta(objectClassGroup, group, attrAdd, options);
            msGraphConnector.dispose();

            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, group, attrAdd1, options);
            msGraphConnector.dispose();

            msGraphConnector.init(conf);
            msGraphConnector.updateDelta(objectClassGroup, group, attrAdd2, options);
            msGraphConnector.dispose();

        }
    }


    @Test(priority = 35)
    public void RemoveMembersFromEachGroup() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<AttributeDelta> attrAdd = new HashSet<>();
        AttributeDeltaBuilder attr = new AttributeDeltaBuilder();
        attr.setName("members");
        attr.addValueToRemove(groupTestUser.getUidValue());
        attrAdd.add(attr.build());


        for (Uid group : groupsUid) {


            msGraphConnector.init(getConfiguration());
            msGraphConnector.updateDelta(objectClassGroup, group, attrAdd, options);
            msGraphConnector.dispose();


        }


    }


    @Test(priority = 36)
    public void Search100GroupsTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Map<String, Object> operationOptions = new HashMap<>();
        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
        operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 1);
        operationOptions.put(OperationOptions.OP_PAGE_SIZE, 10);
        OperationOptions options = new OperationOptions(operationOptions);

        final ArrayList<ConnectorObject> resultsGroup = new ArrayList<>();
        SearchResultsHandler handlerGroup = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsGroup.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };

        msGraphConnector.executeQuery(objectClassGroup, null, handlerGroup, options);

        msGraphConnector.dispose();

        if (resultsGroup.size() < 10) {
            throw new InvalidAttributeValueException("Non exist 100 groups.");
        }
    }


    @Test(priority = 37)
    public void Delete500GroupAndUserTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;
        OperationOptions options = new OperationOptions(new HashMap<>());

        for (Uid group : groupsUid) {
            msGraphConnector.init(conf);
            msGraphConnector.delete(objectClassGroup, group, options);
            msGraphConnector.dispose();
        }

        msGraphConnector.init(conf);
        msGraphConnector.delete(objectClassAccount, groupTestUser, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.delete(objectClassAccount, groupTestUser1, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.delete(objectClassAccount, groupTestUser2, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.delete(objectClassAccount, groupTestUser3, options);
        msGraphConnector.dispose();
    }

    @Test(priority = 30)
    public void CreateSecurityGroupTest() {
        MSGraphConnector connector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "testGroupdisplayName1"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "testGroupmailNickname1"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        attributesCreatedGroup.add(AttributeBuilder.build("owners", Collections.singletonList(testUserId)));

        connector.init(conf);
        Uid groupUid = connector.create(objectClassGroup, attributesCreatedGroup, options);
        connector.delete(objectClassGroup, groupUid, options);
        connector.dispose();
    }
}
