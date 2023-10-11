package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

        Set<AttributeDelta> uid = msGraphConnector.updateDelta(objectClassAccount, testUid, updateAccount, options);
        deleteWaitAndRetry(objectClassAccount, testUid, options);
        Assert.assertNull(uid);
    }
}

