package com.evolveum.polygon.connector.msgraphapi.integration;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.InvalidPasswordException;
import org.identityconnectors.framework.common.objects.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class CreateActionTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void creteTestNotSupportedObjectClass() {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);
        OperationOptions options = new OperationOptions(new HashMap<>());
        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing1"));
        ObjectClass objectClassAccount = new ObjectClass("notExistingObjectClass");

        msGraphConnector.create(objectClassAccount, attributesAccount, options);

    }


    @Test(expectedExceptions = InvalidAttributeValueException.class, priority = 6)
    public void creteTestWithNotFilledMandatoryAttributeForAccount() {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("city", "Kosice"));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        msGraphConnector.create(objectClassAccount, attributesAccount, options);

    }

    @Test(expectedExceptions = InvalidAttributeValueException.class, priority = 7)
    public void creteTestWithoutPasswordForAccount() {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing_noPass"));
        attributesAccount.add(AttributeBuilder.build("mail", "testing_noPass@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing_noPass"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "testing_noPass@" + domain));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        msGraphConnector.create(objectClassAccount, attributesAccount, options);

    }


    @Test(expectedExceptions = AlreadyExistsException.class, priority = 10)
    public void creteTestUserWithExistingUserPrincipalName() throws Exception {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        Map<String, Object> operationOptions = new HashMap<>();
        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
        operationOptions.put(OperationOptions.OP_PAGED_RESULTS_OFFSET, 1);
        operationOptions.put(OperationOptions.OP_PAGE_SIZE, 10);
        OperationOptions options = new OperationOptions(operationOptions);

//        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "create_testing"));
        attributesAccount.add(AttributeBuilder.build("mail", "create_testing@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "create_testing"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "create_testing@" + domain));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        Uid testUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            deleteWaitAndRetry(objectClassAccount, testUid, options);
        }
    }

    @Test(expectedExceptions = InvalidAttributeValueException.class, priority = 4)
    public void creteTestWithNotFilledMandatoryAttributeForGroup() {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());
        Set<Attribute> attributesGroup = new HashSet<>();

        attributesGroup.add(AttributeBuilder.build("description", "Group description"));

        ObjectClass objectClassGroup = ObjectClass.GROUP;


        msGraphConnector.create(objectClassGroup, attributesGroup, options);

    }

    @Test(expectedExceptions = InvalidPasswordException.class, priority = 9)
    public void creteTestUserWithWrongCredentials() {

        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "create_testing"));
        attributesAccount.add(AttributeBuilder.build("mail", "create_testing@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "create_testing"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "create_testing@" + domain));
        GuardedString pass = new GuardedString("passw".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        msGraphConnector.create(objectClassAccount, attributesAccount, options);

    }

}
