package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import static com.evolveum.polygon.connector.msgraphapi.RoleProcessing.ROLE_NAME;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.framework.common.objects.*;
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


        msGraphConnector.executeQuery(objectClassGroup, null, handlerGroup, options);

        resultsGroup = handlerGroup.getResult();
        deleteWaitAndRetry(ObjectClass.GROUP, groupBlue, options);
        Assert.assertTrue(!resultsGroup.isEmpty());

    }
    
    @Test(priority = 22)
    public void filteringEmptyPageTestRoleObjectClass() throws Exception {
        msGraphConnector = new MSGraphConnector();

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);
        OperationOptions options = new OperationOptions(new HashMap<>());
        ObjectClass objectClass = new ObjectClass(ROLE_NAME);

        Map<String, Object> operationOptions = new HashMap<>();
        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
        operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 900);
        operationOptions.put(OperationOptions.OP_PAGE_SIZE, 100);
        options = new OperationOptions(operationOptions);

        ArrayList<ConnectorObject> resultsRoles = new ArrayList<>();
        TestSearchResultsHandler handlerRole = new TestSearchResultsHandler();

        msGraphConnector.executeQuery(objectClass, null, handlerRole, options);

        resultsRoles = handlerRole.getResult();
        Assert.assertTrue(!resultsRoles.isEmpty());

    }
}
