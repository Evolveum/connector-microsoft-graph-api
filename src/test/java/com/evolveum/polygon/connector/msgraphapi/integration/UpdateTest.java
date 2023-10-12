package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.*;

public class UpdateTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = UnknownUidException.class, priority = 14)
    public void updateTestGroupWithUnknownUid() {
        msGraphConnector = new MSGraphConnector();

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<String, Object>());

        Set<AttributeDelta> attributesUpdateGroup = new HashSet<>();
        attributesUpdateGroup.add(AttributeDeltaBuilder.build("description", "The blue one"));

        ObjectClass objectClassAccount = ObjectClass.GROUP;

        msGraphConnector.updateDelta(objectClassAccount, new Uid("9999999999999999999999999999999999999999999"), attributesUpdateGroup, options);
    }

    @Test(expectedExceptions = UnknownUidException.class, priority = 13)
    public void updateTestUserWithUnknownUid() {
         msGraphConnector = new MSGraphConnector();

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<String, Object>());

        Set<AttributeDelta> attributesUpdateUser = new HashSet<>();
        attributesUpdateUser.add(AttributeDeltaBuilder.build("city", "Las Vegas"));

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        msGraphConnector.updateDelta(objectClassAccount, new Uid("9999999999999999999999999999999999999999999"), attributesUpdateUser, options);
    }

    @Test(priority = 12)
    public void updateTestUser() throws Exception {
        msGraphConnector = new MSGraphConnector();

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing1"));
        attributesAccount.add(AttributeBuilder.build("mail", "testing1@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing1"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "tes1tin1g991@" + domain));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Uid testUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        Set<AttributeDelta> updateAccount = new HashSet<>();
        updateAccount.add(AttributeDeltaBuilder.build("jobTitle", "it"));
        updateAccount.add(AttributeDeltaBuilder.build("surName", "Peter"));
        updateAccount.add(AttributeDeltaBuilder.build("givenName", "Jackie"));
        updateAccount.add(AttributeDeltaBuilder.build("businessPhones", Stream.of("+1 425 555 0109").collect(Collectors.toList()), null));

        Set<AttributeDelta> sideEffects = msGraphConnector.updateDelta(objectClassAccount, testUid, updateAccount, options);
        assertNull(sideEffects);

        Thread.sleep(_WAIT_INTERVAL);

        TestSearchResultsHandler resultHandler = getResultHandler();
        msGraphConnector.executeQuery(objectClassAccount, new EqualsFilter(testUid), resultHandler, getDefaultAccountOperationOptions());

        ArrayList<ConnectorObject> result = resultHandler.getResult();
        assertEquals(1, result.size());
        assertEquals("it", AttributeUtil.getStringValue(result.get(0).getAttributeByName("jobTitle")));
        assertEquals("Peter", AttributeUtil.getStringValue(result.get(0).getAttributeByName("surName")));
        assertEquals("Jackie", AttributeUtil.getStringValue(result.get(0).getAttributeByName("givenName")));
        assertEquals(1, result.get(0).getAttributeByName("businessPhones").getValue().size());
        assertEquals("+1 425 555 0109",result.get(0).getAttributeByName("businessPhones").getValue().get(0).toString());

        deleteWaitAndRetry(objectClassAccount, testUid, options);
    }
}

