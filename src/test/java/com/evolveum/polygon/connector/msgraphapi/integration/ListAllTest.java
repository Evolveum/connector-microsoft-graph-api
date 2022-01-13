package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;
import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class ListAllTest extends BasicConfigurationForTests {

    @Test(priority = 22)
    public void filteringEmptyPageTestGroupObjectClass() throws Exception {
        msGraphConnector = new MSGraphConnector();

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);
        OperationOptions options = new OperationOptions(new HashMap<String, Object>());
        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("displayName", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);

        Map<String, Object> operationOptions = new HashMap<>();
        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
        operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 900);
        operationOptions.put(OperationOptions.OP_PAGE_SIZE, 100);
        options = new OperationOptions(operationOptions);


        ArrayList<ConnectorObject> resultsGroup = new ArrayList<>();
        TestSearchResultsHandler handlerGroup = new TestSearchResultsHandler();
//        SearchResultsHandler handlerGroup = new SearchResultsHandler() {
//
//            @Override
//            public boolean handle(ConnectorObject connectorObject) {
//                resultsGroup.add(connectorObject);
//                return true;
//            }
//
//            @Override
//            public void handleResult(SearchResult result) {
//            }
//        };

        msGraphConnector.executeQuery(objectClassGroup, null, handlerGroup, options);

//        try {
//            if (!resultsGroup.isEmpty()) {
//                throw new InvalidAttributeValueException("Searched page is not empty.");
//            }
//        } finally {
//            msGraphConnector.delete(objectClassGroup, groupBlue, options);
//            msGraphConnector.dispose();
//        }
        // could be a part of the after method
        resultsGroup = handlerGroup.getResult();
        deleteWaitAndRetry(ObjectClass.GROUP, groupBlue, options);
        Assert.assertTrue(!resultsGroup.isEmpty());

    }
}
