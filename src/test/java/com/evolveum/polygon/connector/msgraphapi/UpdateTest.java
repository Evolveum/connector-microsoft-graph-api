package com.evolveum.polygon;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UpdateTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = UnknownUidException.class, priority = 14)
    public void updateTestGroupWithUnknownUid() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<String, Object>());

        Set<Attribute> attributesUpdateGroup = new HashSet<Attribute>();
        attributesUpdateGroup.add(AttributeBuilder.build("description", "The blue one"));

        ObjectClass objectClassAccount = ObjectClass.GROUP;

        try {
            msGraphConnector.update(objectClassAccount, new Uid("9999999999999999999999999999999999999999999"), attributesUpdateGroup, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = UnknownUidException.class, priority = 13)
    public void updateTestUserWithUnknownUid() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<String, Object>());

        Set<Attribute> attributesUpdateUser = new HashSet<Attribute>();
        attributesUpdateUser.add(AttributeBuilder.build("city", "Las Vegas"));

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        try {
            msGraphConnector.update(objectClassAccount, new Uid("9999999999999999999999999999999999999999999"), attributesUpdateUser, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(priority = 12)
    public void updateTestUser() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing1"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing1"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "tes1tin1g991@TENANTID"));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Uid testUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);


        Set<Attribute> updateAccount = new HashSet<>();
        updateAccount.add(AttributeBuilder.build("jobTitle", "it"));
        updateAccount.add(AttributeBuilder.build("surName", "Peter"));
        updateAccount.add(AttributeBuilder.build("givenName", "Jackie"));

        try {
            msGraphConnector.update(objectClassAccount, testUid, updateAccount, options);
        } finally {
            msGraphConnector.delete(objectClassAccount, testUid, options);
            msGraphConnector.dispose();
        }
    }




}

