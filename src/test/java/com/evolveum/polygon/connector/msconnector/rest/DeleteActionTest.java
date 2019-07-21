package com.evolveum.polygon.connector.msconnector.rest;

import com.microsoft.graph.core.ClientException;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

import java.util.*;


public class DeleteActionTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = ClientException.class)
    public void deleteUserTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);


        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "testingFromConn5@TENANTID"));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        Uid testUserUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        msGraphConnector.delete(objectClassAccount, testUserUid, options);


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

        try {
            AttributeFilter equalsFilter;
            equalsFilter = (EqualsFilter) FilterBuilder.equalTo(testUserUid);
            msGraphConnector.executeQuery(objectClassAccount, equalsFilter, handlerAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = ClientException.class)
    public void deleteGroupTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> groupAttributes = new HashSet<>();
        groupAttributes.add(AttributeBuilder.build("__NAME__", "testGroup1"));
        groupAttributes.add(AttributeBuilder.build("mailEnabled", false));
        groupAttributes.add(AttributeBuilder.build("mailNickname", "testGroup1"));
        groupAttributes.add(AttributeBuilder.build("securityEnabled", true));
        ObjectClass groupObject = ObjectClass.GROUP;


        Uid testGroupUid = msGraphConnector.create(groupObject, groupAttributes, options);
        msGraphConnector.delete(groupObject, testGroupUid, options);


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

        try {
            AttributeFilter equalsFilter;
            equalsFilter = (EqualsFilter) FilterBuilder.equalTo(testGroupUid);
            msGraphConnector.executeQuery(groupObject, equalsFilter, handlerAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }
}
