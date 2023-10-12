package com.evolveum.polygon.connector.msgraphapi.integration;

import static com.evolveum.polygon.connector.msgraphapi.RoleProcessing.ROLE_NAME;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author jan.mokracek
 */
public class RoleMembershipManagementTest extends BasicConfigurationForTests {

    private Uid roleTestUser;
    private Uid roleTestUser1;
    private Uid roleTestUser2;

    @Test(priority = 10)
    public void createUserAndAddUserToAlreadyExistedRoleInTenant() throws Exception {
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Yellow221122232457"));
        attributesAccount.add(AttributeBuilder.build("mail", "Yellow22112223457@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Yellow22112223457"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Yellow22112223457@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));

        roleTestUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        ConnectorObject role = getAlreadyExistedRoleInTenant();

        addUserToRole(roleTestUser, role);

        Assert.assertTrue(isUserMemberOfRoleWaitAndRetry(roleTestUser, true));
    }

    @Test(priority = 20)
    public void createUserAndCheckNonExistentRoleMembership() throws Exception {
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Yellow88"));
        attributesAccount.add(AttributeBuilder.build("mail", "Yellow88@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Yellow88"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Yellow88@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));

        roleTestUser1 = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        Assert.assertTrue(!isUserMemberOfRoleWaitAndRetry(roleTestUser1, false));
    }

    @Test(priority = 30)
    public void createUserRemoveRoleMembership() throws Exception {
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Yellow182822222211222212111"));
        attributesAccount.add(AttributeBuilder.build("mail", "Yellow8811221222221122222112@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Yellow812212222228122221111"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Yellow128212222281222122111@" + domain));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));

        roleTestUser2 = msGraphConnector.create(objectClassAccount, attributesAccount, options);
        ConnectorObject role = getAlreadyExistedRoleInTenant();

        addUserToRole(roleTestUser2, role);

        if (isUserMemberOfRoleWaitAndRetry(roleTestUser2, true)) {
            removeUserFromRole(roleTestUser2, role);
            Assert.assertFalse(isUserMemberOfRoleWaitAndRetry(roleTestUser2, false));
        } else {
            Assert.fail("User wasn't added to the role.");
        }
    }

    private ConnectorObject getAlreadyExistedRoleInTenant() {
        OperationOptions options = getDefaultRoleOperationOptions();

        ObjectClass objectClassRole = new ObjectClass(ROLE_NAME);

        AttributeFilter containsFilterRole;
        containsFilterRole = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", roleWhichExistsInTenantDisplayName));
        TestSearchResultsHandler handlerRole = getResultHandler();
        msGraphConnector.executeQuery(objectClassRole, containsFilterRole, handlerRole, options);
        ArrayList<ConnectorObject> resultsRole = handlerRole.getResult();

        if (resultsRole.size() != 1) {
            Assert.fail("Chosen role displayName is not unique or doesn't even exists");
        }

        return resultsRole.get(0);
    }

    private void removeUserFromRole(Uid userUid, ConnectorObject role) throws Exception {
        ObjectClass objectClassRole = new ObjectClass(ROLE_NAME);
        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<AttributeDelta> removeUserUid = new HashSet<>();
        AttributeDeltaBuilder attr = new AttributeDeltaBuilder();
        attr.setName(ATTR_MEMBERS);
        attr.addValueToRemove(userUid.getUidValue());
        removeUserUid.add(attr.build());
        int iterator = 0;
        while (_REPEAT_COUNT > iterator) {
            try {
                role = getAlreadyExistedRoleInTenant();
                updateWaitAndRetry(objectClassRole, role.getUid(), removeUserUid, options);
                break;
            } catch (InvalidAttributeValueException e) {
                Thread.sleep(_REPEAT_INTERVAL);
            }
            iterator++;
        }
    }

    private void addUserToRole(Uid userUid, ConnectorObject role) throws Exception {
        ObjectClass objectClassRole = new ObjectClass(ROLE_NAME);
        OperationOptions options = new OperationOptions(new HashMap<>());

        Set<AttributeDelta> addedUserUid = new HashSet<>();
        AttributeDeltaBuilder attr = new AttributeDeltaBuilder();
        attr.setName(ATTR_MEMBERS);
        attr.addValueToAdd(userUid.getUidValue());
        addedUserUid.add(attr.build());
        updateWaitAndRetry(objectClassRole, role.getUid(), addedUserUid, options);
    }

    protected boolean isUserMemberOfRoleWaitAndRetry(Uid userUid, boolean userShouldBeAMember) throws InterruptedException {
        boolean isUserMemberOfRole = false;
        int iterator = 0;
        while (_REPEAT_COUNT > iterator) {
            ConnectorObject role = null;
            try {
                role = getAlreadyExistedRoleInTenant();
            } catch (UnknownUidException e) {

            }
            isUserMemberOfRole = role.getAttributeByName(ATTR_MEMBERS).getValue().contains(userUid.getUidValue());
            if (isUserMemberOfRole != userShouldBeAMember) {
                Thread.sleep(_REPEAT_INTERVAL);
            } else {
                break;
            }
            iterator++;
        }
        return isUserMemberOfRole;
    }

    @Test(priority = 40)
    public void DeleteUserTest() {
        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;
        OperationOptions options = new OperationOptions(new HashMap<>());

        msGraphConnector.delete(objectClassAccount, roleTestUser, options);
        msGraphConnector.delete(objectClassAccount, roleTestUser1, options);
        msGraphConnector.delete(objectClassAccount, roleTestUser2, options);
        msGraphConnector.dispose();
    }
}
