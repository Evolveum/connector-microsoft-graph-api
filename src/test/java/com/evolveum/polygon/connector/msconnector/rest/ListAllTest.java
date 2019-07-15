package com.evolveum.polygon.connector.msconnector.rest;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

import java.util.*;

public class ListAllTest extends BasicConfigurationForTests {

    @Test(priority = 22)
    public void filteringEmptyPageTestGroupObjectClass() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("__NAME__", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);

        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
        operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 10);
        operationOptions.put(OperationOptions.OP_PAGE_SIZE, 20);
        options = new OperationOptions(operationOptions);


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

        try {
            if (!resultsGroup.isEmpty()) {
                throw new InvalidAttributeValueException("Searched page is not empty.");
            }
        } finally {
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }
    }

    @Test
    public void filteringEmptyPageTestUserObjectClass() {

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
        attributesAccount.add(AttributeBuilder.build("__NAME__", "testin7g@TENANT.onmicrosoft.com"));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        Uid testUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
        operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 10);
        operationOptions.put(OperationOptions.OP_PAGE_SIZE, 20);
        options = new OperationOptions(operationOptions);


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

        msGraphConnector.executeQuery(objectClassAccount, null, handlerAccount, options);

        try {
            if (!resultsAccount.isEmpty()) {
                throw new InvalidAttributeValueException("Searched page is not empty.");
            }
        } finally {
            msGraphConnector.delete(objectClassAccount, testUid, options);
            msGraphConnector.dispose();
        }

    }
}

