package com.evolveum.polygon.connector.msgraphapi.integration;

import org.identityconnectors.framework.common.objects.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class GenericSchemaTest extends BasicConfigurationForTests {
    private PropertiesParser parser = new PropertiesParser();

    @Test(priority = 10)
    public void processSchemaTest() {
        msGraphConfiguration = getConfiguration();
        msGraphConfiguration.setDiscoverSchema(parser.isDiscoverSchame());
        msGraphConnector.init(msGraphConfiguration);

        Schema schema = msGraphConnector.schema();
        assertFalse(schema.getObjectClassInfo().size() > 3); // By default account, group, license
    }

    @Test(priority = 20)
    public void genericObjectsTest() {
        String projectName = parser.getGenericSchemaAttrInfoName();
        msGraphConfiguration = getConfiguration();
        msGraphConfiguration.setDiscoverSchema(parser.isDiscoverSchame());
        msGraphConnector.init(msGraphConfiguration);

        ResultsHandler resultsHandler = (object) -> {
            assertNotNull(object);
            return true;
        };

        Map<String, Object> operationOptions = new HashMap<>();
        msGraphConnector.executeQuery(new ObjectClass(projectName), null, resultsHandler, new OperationOptions(operationOptions));
    }
}

