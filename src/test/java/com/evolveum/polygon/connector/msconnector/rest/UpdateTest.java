package com.evolveum.polygon.connector.msconnector.rest;

import com.microsoft.graph.core.ClientException;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = ClientException.class)
    public void updateTestGroupWithUnknownUid() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<String, Object>());

        Set<Attribute> attributesUpdateGroup = new HashSet<Attribute>();
        attributesUpdateGroup.add(AttributeBuilder.build("description", "The blue one"));

        ObjectClass objectClassAccount = ObjectClass.GROUP;

        try {
            msGraphConnector.update(objectClassAccount, new Uid("99999999999999999999999999999999999999"), attributesUpdateGroup, options);
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = ClientException.class)
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

    @Test
    public void updateTestUser() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("__ENABLE__", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "testing1"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "testing1"));
        attributesAccount.add(AttributeBuilder.build("__NAME__", "tes1tin1g9191@TENANT.onmicrosoft.com"));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Uid testUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);


        Set<Attribute> updateAccount = new HashSet<>();
        updateAccount.add(AttributeBuilder.build("jobTitle", "it"));
        updateAccount.add(AttributeBuilder.build("surName", "Peter"));
        updateAccount.add(AttributeBuilder.build("givenName", "Jackie"));
        GuardedString pass1 = new GuardedString("Password99cba".toCharArray());
        updateAccount.add(AttributeBuilder.build("__PASSWORD__", pass1));

        // Uid uid = new Uid("0cb9b6f9-143c-4ae5-8e09-4647f8199557");
        try {
            msGraphConnector.update(objectClassAccount, testUid, updateAccount, options);
        } finally {
            msGraphConnector.delete(objectClassAccount, testUid, options);
            msGraphConnector.dispose();
        }
    }

    @Test
    public void createGroupTest() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        Map<String, Object> operationOptions = new HashMap<>();
        OperationOptions options = new OperationOptions(operationOptions);

        Set<Attribute> groupAttributes = new HashSet<>();
        groupAttributes.add(AttributeBuilder.build("__NAME__", "testGroup2"));
        groupAttributes.add(AttributeBuilder.build("mailEnabled", false));
        groupAttributes.add(AttributeBuilder.build("mailNickname", "testGroup2"));
        groupAttributes.add(AttributeBuilder.build("securityEnabled", true));
        ObjectClass groupObject = ObjectClass.GROUP;

        Uid testGroupUid = msGraphConnector.create(groupObject, groupAttributes, options);

        Set<Attribute> updateGroup = new HashSet<>();
        updateGroup.add(AttributeBuilder.build("description", "Group description"));

        try {
            msGraphConnector.update(groupObject, testGroupUid, updateGroup, options);
        } finally {
            msGraphConnector.delete(groupObject, testGroupUid, options);
            msGraphConnector.dispose();
        }
    }


}

