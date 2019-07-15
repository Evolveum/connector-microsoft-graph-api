package com.evolveum.polygon.connector.msconnector.rest;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

import java.util.*;

public class UserPerformanceTest extends BasicConfigurationForTests {
    private Uid testGroupUid;

    private Set<Uid> usersUid = new HashSet<Uid>();


    @Test(priority = 15)
    public void CreateGroupAnd500UsersTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        Map<String, Object> operationOptions = new HashMap<>();

        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassGroup = ObjectClass.GROUP;


        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("__NAME__", "testGroupdisplayName"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "testGroupmailNickname"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));

        msGraphConnector.init(conf);
        testGroupUid = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);
        msGraphConnector.dispose();

        for (int i = 0; i < 500; i++) {
            Set<Attribute> attributesAccount = new HashSet<>();
            attributesAccount.add(AttributeBuilder.build("__ENABLE__", true));
            attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
            attributesAccount.add(AttributeBuilder.build("displayName", "Yellow" + i));
            attributesAccount.add(AttributeBuilder.build("mailNickname", "Yellow" + i));
            attributesAccount.add(AttributeBuilder.build("__NAME__", "Yellowq1" + i + "@TENANT.onmicrosoft.com"));
            GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
            attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
            msGraphConnector.init(conf);
            Uid userUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);

            Set<Attribute> attributesAddUserToGroup = new HashSet<>();
            attributesAddUserToGroup.add(AttributeBuilder.build("usergroups", testGroupUid.getUidValue()));
            msGraphConnector.addAttributeValues(objectClassAccount, userUid, attributesAddUserToGroup, options);
            msGraphConnector.dispose();
            usersUid.add(userUid);
        }

    }

    @Test(priority = 16)
    public void Search500UsersTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        ObjectClass objectClassAccout = ObjectClass.ACCOUNT;

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        final ArrayList<ConnectorObject> resultsGroup = new ArrayList<>();
        SearchResultsHandler handleUser = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsGroup.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };

        msGraphConnector.executeQuery(objectClassAccout, null, handleUser, options);

        msGraphConnector.dispose();

        if (resultsGroup.size() < 500) {
            throw new InvalidAttributeValueException("Non exist 500 users.");
        }
    }

    @Test(priority = 17)
    public void Update500UsersTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;
        int i = 0;
        for (Uid user : usersUid) {
            Set<Attribute> attributesUpdateGroup = new HashSet<>();
            AttributeBuilder attr = new AttributeBuilder();
            attr.setName("city");
            attr.addValue("Kosice" + i);
            attributesUpdateGroup.add(attr.build());
            msGraphConnector.init(conf);
            msGraphConnector.update(objectClassAccount, user, attributesUpdateGroup, options);
            msGraphConnector.dispose();
            i++;
        }

    }

    @Test(priority = 18)
    public void Delete500UserTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        ObjectClass objectClassGroup = ObjectClass.GROUP;
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        for (Uid user : usersUid) {
            msGraphConnector.init(conf);

            Set<Attribute> attributesRemoveUserFromGroup = new HashSet<>();
            attributesRemoveUserFromGroup.add(AttributeBuilder.build("usergroups", testGroupUid.getUidValue()));
            msGraphConnector.removeAttributeValues(objectClassAccount, user, attributesRemoveUserFromGroup, options);

            msGraphConnector.delete(objectClassAccount, user, options);
            msGraphConnector.dispose();
        }

        msGraphConnector.init(conf);
        msGraphConnector.delete(objectClassGroup, testGroupUid, options);
        msGraphConnector.dispose();

    }

}
