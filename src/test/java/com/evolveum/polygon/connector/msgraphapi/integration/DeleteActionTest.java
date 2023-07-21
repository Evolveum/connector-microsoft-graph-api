package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class DeleteActionTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = UnknownUidException.class, priority = 24)
    public void deleteUserTest() throws InterruptedException {

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = getDefaultAccountOperationOptions();

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "del_testing"));
        attributesAccount.add(AttributeBuilder.build("mail", "del_testing@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "del_testing"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "del_testing@" + domain));
        GuardedString pass = new GuardedString("Password99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;


        Uid testUserUid = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        msGraphConnector.delete(objectClassAccount, testUserUid, options);


        ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        TestSearchResultsHandler handlerAccount = getResultHandler();

        AttributeFilter equalsFilter;
        equalsFilter = (EqualsFilter) FilterBuilder.equalTo(testUserUid);

        TimeUnit.SECONDS.sleep(_WAIT_INTERVAL);

        msGraphConnector.executeQuery(objectClassAccount, equalsFilter, handlerAccount, options);

        resultsAccount = handlerAccount.getResult();

        for (ConnectorObject o : resultsAccount) {
            if (o.getUid().equals(testUserUid)) {

                Assert.fail();
                break;
            }
        }
    }

    @Test(expectedExceptions = UnknownUidException.class, priority = 25)
    public void deleteGroupTest() throws InterruptedException {

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);

        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<Attribute> groupAttributes = new HashSet<>();
        groupAttributes.add(AttributeBuilder.build("displayName", "testGroup1"));
        groupAttributes.add(AttributeBuilder.build("mailEnabled", false));
        groupAttributes.add(AttributeBuilder.build("mailNickname", "testGroup1"));
        groupAttributes.add(AttributeBuilder.build("securityEnabled", true));
        ObjectClass groupObject = ObjectClass.GROUP;


        Uid testGroupUid = msGraphConnector.create(groupObject, groupAttributes, options);
        msGraphConnector.delete(groupObject, testGroupUid, options);


        ArrayList<ConnectorObject> resultsGroup = new ArrayList<>();
        TestSearchResultsHandler handlergroup = new TestSearchResultsHandler();

        AttributeFilter equalsFilter;
        equalsFilter = (EqualsFilter) FilterBuilder.equalTo(testGroupUid);

        TimeUnit.SECONDS.sleep(_WAIT_INTERVAL);
        msGraphConnector.executeQuery(groupObject, equalsFilter, handlergroup, options);

        resultsGroup = handlergroup.getResult();

        for (ConnectorObject o : resultsGroup) {
            if (o.getUid().equals(testGroupUid)) {

                Assert.fail();
                break;
            }
        }
    }
}
