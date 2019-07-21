package com.evolveum.polygon.connector.msconnector.rest;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class GroupPerformanceTests extends BasicConfigurationForTests {

    private Uid groupTestUser;
    private Uid groupTestUser1;
    private Uid groupTestUser2;
    private Uid groupTestUser3;

    private Set<Uid> groupsUid = new HashSet<>();
    Set<String> groupsUidList = new HashSet<>();


    @Test(priority = 30)
    public void Create500Groups() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);
        ObjectClass objectClassGroup = ObjectClass.GROUP;

        for (int i = 0; i < 500; i++) {

            Set<Attribute> attributesCreatedGroup = new HashSet<>();
            attributesCreatedGroup.add(AttributeBuilder.build("__NAME__", "testGroupdisplayName" + i));
            attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
            attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "testGroupmailNickname" + i));
            attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));

            msGraphConnector.init(conf);
            Uid groupUid = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);
            msGraphConnector.dispose();
            groupsUid.add(groupUid);

        }

    }

    @Test(priority = 31)
    public void Update500GroupsTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        int i = 0;
        for (Uid groupUid : groupsUid) {

            Set<Attribute> attributesUpdateGroup = new HashSet<>();

            AttributeBuilder attr = new AttributeBuilder();
            attr.setName("description");
            attr.addValue("this is the " + i + " description update");
            attributesUpdateGroup.add(attr.build());
            msGraphConnector.init(conf);
            msGraphConnector.update(objectClassGroup, groupUid, attributesUpdateGroup, options);
            msGraphConnector.dispose();
            i++;
        }
    }


    @Test(priority = 32)
    public void CreateUserAndAddUserToEachGroupTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Yellow"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Yellow"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "Yellow@TENANTID"));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));


        msGraphConnector.init(conf);
        groupTestUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        msGraphConnector.dispose();

        for (Uid groupUid : groupsUid) {
            groupsUidList.add(groupUid.getUidValue());
        }
        Set<Attribute> simbaAttributeUid = new HashSet<Attribute>();
        simbaAttributeUid.add(AttributeBuilder.build("usergroups", groupsUidList));
        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser, simbaAttributeUid, options);
        msGraphConnector.dispose();


    }

    @Test(priority = 33)
    public void Search500GroupsTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        final ArrayList<ConnectorObject> resultsGroup = new ArrayList<>();
        SearchResultsHandler handleGroup = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsGroup.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };

        msGraphConnector.executeQuery(objectClassGroup, null, handleGroup, options);

        msGraphConnector.dispose();

        if (resultsGroup.size() < 500) {
            throw new InvalidAttributeValueException("Non exist 500 groups.");
        }
    }

    @Test(priority = 34)
    public void CreateUsersAndAddUserToEachGroupAsOwnerAndAsMemberTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount3 = new HashSet<>();
        attributesAccount3.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount3.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount3.add(AttributeBuilder.build("displayName", "Blue"));
        attributesAccount3.add(AttributeBuilder.build("mailNickname", "Blue"));
        attributesAccount3.add(AttributeBuilder.build("__NAME__", "Blue@TENANTID"));
        GuardedString pass3 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount3.add(AttributeBuilder.build("__PASSWORD__", pass3));

        Set<Attribute> attributesAccount1 = new HashSet<>();
        attributesAccount1.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount1.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount1.add(AttributeBuilder.build("displayName", "Red"));
        attributesAccount1.add(AttributeBuilder.build("mailNickname", "Red"));
        attributesAccount1.add(AttributeBuilder.build("__NAME__", "Red@TENANTID"));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount1.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Set<Attribute> attributesAccount2 = new HashSet<>();
        attributesAccount2.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount2.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount2.add(AttributeBuilder.build("displayName", "Black"));
        attributesAccount2.add(AttributeBuilder.build("mailNickname", "Black"));
        attributesAccount2.add(AttributeBuilder.build("__NAME__", "Black@TENANTID"));
        GuardedString pass2 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount2.add(AttributeBuilder.build("__PASSWORD__", pass2));

        msGraphConnector.init(conf);
        groupTestUser1 = msGraphConnector.create(objectClassAccount, attributesAccount1, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        groupTestUser2 = msGraphConnector.create(objectClassAccount, attributesAccount2, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        groupTestUser3 = msGraphConnector.create(objectClassAccount, attributesAccount3, options);
        msGraphConnector.dispose();


        Set<String> groupsUidList = new HashSet<>();
        for (Uid groupUid : groupsUid) {
            groupsUidList.add(groupUid.getUidValue());

        }

        Set<Attribute> ownerAttributeUid = new HashSet<>();
        ownerAttributeUid.add(AttributeBuilder.build("userDirectoryObjectOwner", groupsUidList));


        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser, ownerAttributeUid, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser1, ownerAttributeUid, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser2, ownerAttributeUid, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser3, ownerAttributeUid, options);
        msGraphConnector.dispose();

        Set<Attribute> memberAttributeUid = new HashSet<>();
        memberAttributeUid.add(AttributeBuilder.build("userDirectoryObjectMember", groupsUidList));


        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser, memberAttributeUid, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser1, memberAttributeUid, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser2, memberAttributeUid, options);
        msGraphConnector.dispose();

        msGraphConnector.init(conf);
        msGraphConnector.addAttributeValues(objectClassAccount, groupTestUser3, memberAttributeUid, options);
        msGraphConnector.dispose();
    }

    //must be remain 1 owner for each group
    @Test(priority = 35)
    public void RemoveOwnerFromEachGroup() {

        MSGraphConnector msGraphConnector = new MSGraphConnector();


        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        Set<Attribute> attrRemove = new HashSet<>();
        attrRemove.add(AttributeBuilder.build("userDirectoryObjectOwner",groupsUidList ));


        msGraphConnector.init(getConfiguration());
        msGraphConnector.removeAttributeValues(objectClassAccount, groupTestUser, attrRemove, options);
        msGraphConnector.dispose();

        msGraphConnector.init(getConfiguration());
        msGraphConnector.removeAttributeValues(objectClassAccount, groupTestUser1, attrRemove, options);
        msGraphConnector.dispose();

        msGraphConnector.init(getConfiguration());
        msGraphConnector.removeAttributeValues(objectClassAccount, groupTestUser2, attrRemove, options);
        msGraphConnector.dispose();
    }


    @Test(priority = 36)
    public void RemoveMembersFromEachGroup() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;



        Set<Attribute> attrRemove = new HashSet<>();
        attrRemove.add(AttributeBuilder.build("userDirectoryObjectMember", groupsUidList));


        msGraphConnector.init(getConfiguration());
        msGraphConnector.removeAttributeValues(objectClassAccount, groupTestUser, attrRemove, options);
        msGraphConnector.dispose();

        msGraphConnector.init(getConfiguration());
        msGraphConnector.removeAttributeValues(objectClassAccount, groupTestUser1, attrRemove, options);
        msGraphConnector.dispose();

        msGraphConnector.init(getConfiguration());
        msGraphConnector.removeAttributeValues(objectClassAccount, groupTestUser2, attrRemove, options);
        msGraphConnector.dispose();

        msGraphConnector.init(getConfiguration());
        msGraphConnector.removeAttributeValues(objectClassAccount, groupTestUser3, attrRemove, options);
        msGraphConnector.dispose();

    }



    @Test(priority = 37)
    public void Delete500GroupAndUserTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;
        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

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

}
