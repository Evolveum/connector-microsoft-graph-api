package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;
import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.objects.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class CreateActionTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void creteTestNotSupportedObjectClass() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        OperationOptions options = new OperationOptions(new HashMap<>());
        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing1"));
        ObjectClass objectClassAccount = new ObjectClass("notExistingObjectClass");

        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }


    @Test(expectedExceptions = InvalidAttributeValueException.class, priority = 6)
    public void creteTestWithNotFilledMandatoryAttributeForAccount() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("city", "Kosice"));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = InvalidAttributeValueException.class, priority = 7)
    public void creteTestWithoutPasswordForAccount() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "testing@TENANTID"));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }


    @Test(expectedExceptions = AlreadyExistsException.class, priority = 10)
    public void creteTestUserWithExistingUserPrincipalName() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "testing@TENANTID"));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        Uid testUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            msGraphConnector.delete(objectClassAccount, testUid, options);
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = InvalidAttributeValueException.class, priority = 4)
    public void creteTestWithNotFilledMandatoryAttributeForGroup() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<>());
        Set<Attribute> attributesGroup = new HashSet<>();

        attributesGroup.add(AttributeBuilder.build("description", "Group description"));

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        try {
            msGraphConnector.create(objectClassGroup, attributesGroup, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = InvalidPasswordException.class, priority = 9)
    public void creteTestUserWithWrongCredentials() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "testing@TENANTID"));
        GuardedString pass = new GuardedString("passw".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {

            msGraphConnector.dispose();
        }
    }

}
