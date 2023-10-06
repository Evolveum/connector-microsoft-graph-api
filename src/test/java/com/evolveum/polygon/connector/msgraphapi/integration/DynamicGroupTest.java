package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.*;

public class DynamicGroupTest extends BasicConfigurationForTests {

    Uid testUid;

    @BeforeClass
    public void setUp() {
        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);
    }

    @AfterClass
    public void tearDown() {
        msGraphConnector.dispose();
    }

    @AfterMethod
    public void cleanup() throws Exception {
        if (testUid != null) {
            deleteWaitAndRetry(ObjectClass.GROUP, testUid, new OperationOptions(new HashMap<>()));
        }
    }

    @Test(priority = 10)
    public void createAndUpdateMicrosoft365Group() throws Exception {
        if (!aadPremiumLicenseRequired) {
            throw new SkipException("Azure AD Premium License is required, skipping");
        }

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesGroup = new HashSet<>();
        attributesGroup.add(AttributeBuilder.build("displayName", "testing1"));
        attributesGroup.add(AttributeBuilder.build("mailEnabled", true));
        attributesGroup.add(AttributeBuilder.build("securityEnabled", false));
        attributesGroup.add(AttributeBuilder.build("mailNickname", "testing1"));
        attributesGroup.add(AttributeBuilder.build("description", "testing1 description"));
        attributesGroup.add(AttributeBuilder.build("groupTypes", "Unified"));
        ObjectClass objectClassGroup = ObjectClass.GROUP;

        testUid = msGraphConnector.create(objectClassGroup, attributesGroup, options);

        Thread.sleep(_WAIT_INTERVAL);

        // Update groupTypes
        Set<AttributeDelta> updateGroup = new HashSet<>();
        updateGroup.add(AttributeDeltaBuilder.build("description", "changed"));
        updateGroup.add(AttributeDeltaBuilder.build("groupTypes", list("DynamicMembership"), null));
        updateGroup.add(AttributeDeltaBuilder.build("membershipRule", "user.department -eq \"Marketing\""));
        updateGroup.add(AttributeDeltaBuilder.build("membershipRuleProcessingState", "On"));

        Set<AttributeDelta> sideEffects = msGraphConnector.updateDelta(objectClassGroup, testUid, updateGroup, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        // Check the update result
        TestSearchResultsHandler resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassGroup, new EqualsFilter(testUid), resultHandler, getDefaultGroupOperationOptions());

        List<ConnectorObject> result = resultHandler.getResult();
        assertEquals(1, result.size());
        assertEquals("changed", AttributeUtil.getStringValue(result.get(0).getAttributeByName("description")));
        Attribute groupTypes = result.get(0).getAttributeByName("groupTypes");
        assertNotNull(groupTypes);
        assertEquals(2, groupTypes.getValue().size());
        assertTrue(groupTypes.getValue().contains("Unified"));
        assertTrue(groupTypes.getValue().contains("DynamicMembership"));
        assertEquals("user.department -eq \"Marketing\"", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRule")));
        assertEquals("On", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRuleProcessingState")));

        // Update groupTypes again
        updateGroup = new HashSet<>();
        updateGroup.add(AttributeDeltaBuilder.build("groupTypes", null, list("DynamicMembership")));
        updateGroup.add(AttributeDeltaBuilder.build("membershipRuleProcessingState", "Paused"));

        sideEffects = msGraphConnector.updateDelta(objectClassGroup, testUid, updateGroup, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        // Check the update result
        resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassGroup, new EqualsFilter(testUid), resultHandler, getDefaultGroupOperationOptions());

        result = resultHandler.getResult();
        assertEquals(1, result.size());
        groupTypes = result.get(0).getAttributeByName("groupTypes");
        assertNotNull(groupTypes);
        assertEquals(1, groupTypes.getValue().size());
        assertTrue(groupTypes.getValue().contains("Unified"));
        assertEquals("user.department -eq \"Marketing\"", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRule")));
        assertEquals("Paused", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRuleProcessingState")));
    }

    @Test(priority = 20)
    public void createAndUpdateSecurityGroup() throws Exception {
        if (!aadPremiumLicenseRequired) {
            throw new SkipException("Azure AD Premium License is required, skipping");
        }

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesGroup = new HashSet<>();
        attributesGroup.add(AttributeBuilder.build("displayName", "testing1"));
        attributesGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesGroup.add(AttributeBuilder.build("securityEnabled", true));
        attributesGroup.add(AttributeBuilder.build("mailNickname", "testing1"));
        attributesGroup.add(AttributeBuilder.build("description", "testing1 description"));
        ObjectClass objectClassGroup = ObjectClass.GROUP;

        testUid = msGraphConnector.create(objectClassGroup, attributesGroup, options);

        Thread.sleep(_WAIT_INTERVAL);

        // Update groupTypes
        Set<AttributeDelta> updateGroup = new HashSet<>();
        updateGroup.add(AttributeDeltaBuilder.build("description", "changed"));
        updateGroup.add(AttributeDeltaBuilder.build("groupTypes", list("DynamicMembership"), null));
        updateGroup.add(AttributeDeltaBuilder.build("membershipRule", "user.department -eq \"Marketing\""));
        updateGroup.add(AttributeDeltaBuilder.build("membershipRuleProcessingState", "On"));

        Set<AttributeDelta> sideEffects = msGraphConnector.updateDelta(objectClassGroup, testUid, updateGroup, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        // Check the update result
        TestSearchResultsHandler resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassGroup, new EqualsFilter(testUid), resultHandler, getDefaultGroupOperationOptions());

        List<ConnectorObject> result = resultHandler.getResult();
        assertEquals(1, result.size());
        assertEquals("changed", AttributeUtil.getStringValue(result.get(0).getAttributeByName("description")));
        Attribute groupTypes = result.get(0).getAttributeByName("groupTypes");
        assertNotNull(groupTypes);
        assertEquals(1, groupTypes.getValue().size());
        assertTrue(groupTypes.getValue().contains("DynamicMembership"));
        assertEquals("user.department -eq \"Marketing\"", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRule")));
        assertEquals("On", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRuleProcessingState")));

        // Update groupTypes again
        updateGroup = new HashSet<>();
        updateGroup.add(AttributeDeltaBuilder.build("groupTypes", null, list("DynamicMembership")));
        updateGroup.add(AttributeDeltaBuilder.build("membershipRuleProcessingState", "Paused"));

        sideEffects = msGraphConnector.updateDelta(objectClassGroup, testUid, updateGroup, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        // Check the update result
        resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassGroup, new EqualsFilter(testUid), resultHandler, getDefaultGroupOperationOptions());

        result = resultHandler.getResult();
        assertEquals(1, result.size());
        groupTypes = result.get(0).getAttributeByName("groupTypes");
        assertNotNull(groupTypes);
        assertEquals(0, groupTypes.getValue().size());
        assertEquals("user.department -eq \"Marketing\"", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRule")));
        assertEquals("Paused", AttributeUtil.getStringValue(result.get(0).getAttributeByName("membershipRuleProcessingState")));
    }
}

