package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.*;

public class SPOUserTest extends BasicConfigurationForTests {

    Uid testUid;

    @BeforeClass
    public void setUp() {
        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (testUid != null) {
            deleteWaitAndRetry(ObjectClass.ACCOUNT, testUid, new OperationOptions(new HashMap<>()));
        }
        msGraphConnector.dispose();
    }

    @Test(priority = 10)
    public void updateSPOAttributes() throws Exception {
        if (!spoLicenseRequired) {
            throw new SkipException("SPO License is required, skipping");
        }

        // Create user with SPO attributes
        OperationOptions options = getSPOAccountOperationOptions();

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "create_testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "create_testing"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "create_testing@" + domain));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        testUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        // When updating SPO attributes after creating a user, sufficient time should be needed.
        // Otherwise, the update will return 500 error.
        Thread.sleep(_WAIT_INTERVAL * 2);

        // Check the result
        TestSearchResultsHandler resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassAccount, new EqualsFilter(testUid), resultHandler, options);

        List<ConnectorObject> result = resultHandler.getResult();
        assertEquals(1, result.size());

        // Update user
        Set<AttributeDelta> updateUser = new HashSet<>();
        updateUser.add(AttributeDeltaBuilder.build("displayName", "update_testing"));
        updateUser.add(AttributeDeltaBuilder.build("aboutMe", "About me"));
        updateUser.add(AttributeDeltaBuilder.build("mySite", "https://example.com"));
        updateUser.add(AttributeDeltaBuilder.build("schools", list("A", "B"), null));

        Set<AttributeDelta> sideEffects = msGraphConnector.updateDelta(objectClassAccount, testUid, updateUser, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        // Check the result
        resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassAccount, new EqualsFilter(testUid), resultHandler, options);

        result = resultHandler.getResult();
        assertEquals(1, result.size());
        assertEquals("update_testing", AttributeUtil.getStringValue(result.get(0).getAttributeByName("displayName")));
        assertEquals("About me", AttributeUtil.getStringValue(result.get(0).getAttributeByName("aboutMe")));
        assertEquals("https://example.com", AttributeUtil.getStringValue(result.get(0).getAttributeByName("mySite")));
        Attribute schools = result.get(0).getAttributeByName("schools");
        assertNotNull(schools);
        assertEquals(2, schools.getValue().size());
        assertEquals("A", schools.getValue().get(0));
        assertEquals("B", schools.getValue().get(1));

        // Update user again
        updateUser = new HashSet<>();
        updateUser.add(AttributeDeltaBuilder.build("schools", list("B", "C"), list("A", "D")));

        sideEffects = msGraphConnector.updateDelta(objectClassAccount, testUid, updateUser, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        // Check the result
        resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassAccount, new EqualsFilter(testUid), resultHandler, options);

        result = resultHandler.getResult();
        assertEquals(1, result.size());
        schools = result.get(0).getAttributeByName("schools");
        assertNotNull(schools);
        assertEquals(2, schools.getValue().size());
        assertEquals("B", schools.getValue().get(0));
        assertEquals("C", schools.getValue().get(1));

        // Update user again
        updateUser = new HashSet<>();
        updateUser.add(AttributeDeltaBuilder.build("schools", null, list("B", "C", "D")));

        sideEffects = msGraphConnector.updateDelta(objectClassAccount, testUid, updateUser, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        // Check the result
        resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassAccount, new EqualsFilter(testUid), resultHandler, options);

        result = resultHandler.getResult();
        assertEquals(1, result.size());
        schools = result.get(0).getAttributeByName("schools");
        assertNotNull(schools);
        assertEquals(0, schools.getValue().size());
    }
}

