package com.evolveum.polygon.connector.msconnector.rest;

import com.microsoft.graph.core.ClientException;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class CreateActionTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void creteTestNotSupportedObjectClass() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing1"));
        ObjectClass objectClassAccount = new ObjectClass("notExistingObjectClass");

        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }


    @Test(expectedExceptions = ClientException.class, priority = 6)
    public void creteTestWithNotFilledMandatoryAttributeForAccount() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("city", "Kosice"));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = ClientException.class, priority = 7)
    public void creteTestWithoutPasswordForAccount() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "testing@TENANTID"));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        try {
            msGraphConnector.create(objectClassAccount, attributesAccount, options);
        } finally {
            msGraphConnector.dispose();
        }
    }


    @Test(expectedExceptions = ClientException.class, priority = 10)
    public void creteTestUserWithExistingUserPrincipalName() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "testing@TENANTID"));
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

    @Test(expectedExceptions = ClientException.class, priority = 4)
    public void creteTestWithNotFilledMandatoryAttributeForGroup() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> attributesGroup = new HashSet<>();

        attributesGroup.add(AttributeBuilder.build("description", "Group description"));

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        try {
            msGraphConnector.create(objectClassGroup, attributesGroup, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = ClientException.class, priority = 9)
    public void creteTestUserWithWrongCredentials() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "testing@TENANTID"));
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
