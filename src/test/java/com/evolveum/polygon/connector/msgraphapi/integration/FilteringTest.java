package com.evolveum.polygon.connector.msgraphapi.integration;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;
import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class FilteringTest extends BasicConfigurationForTests {


    @Test(priority = 20)
    public void filteringTestAccountObjectClass() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;


        ObjectClass objectClassAccount = ObjectClass.ACCOUNT;

        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "Pink"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", "Pink"));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", "Pink@TENANTID"));
        GuardedString pass = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", pass));

        Uid firstUser = msGraphConnector.create(objectClassAccount, attributesAccount, options);

        Set<Attribute> attributesAccount1 = new HashSet<>();
        attributesAccount1.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount1.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attributesAccount1.add(AttributeBuilder.build("displayName", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("mailNickname", "PinkAndGreen"));
        attributesAccount1.add(AttributeBuilder.build("userPrincipalName", "PinkAndGreen@TENANTID"));
        GuardedString pass1 = new GuardedString("HelloPassword99".toCharArray());
        attributesAccount1.add(AttributeBuilder.build("__PASSWORD__", pass1));

        Uid secondUser = msGraphConnector.create(objectClassAccount, attributesAccount1, options);

        AttributeFilter containsFilterAccount;
        containsFilterAccount = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "Pink"));

        final ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        SearchResultsHandler handlerAccount = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsAccount.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };

        msGraphConnector.executeQuery(objectClassAccount, containsFilterAccount, handlerAccount, options);
        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        try {
            if (!listUid.contains(firstUser) || !listUid.contains(secondUser)) {
                throw new InvalidAttributeValueException("ContainsFilter not return both user.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equalsFilterAccount1;
        equalsFilterAccount1 = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("userPrincipalName", "Pink@TENANTID"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equalsFilterAccount1, handlerAccount, options);
        try {
            if (!resultsAccount.get(0).getAttributes().contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return searched user. Search with userPrincipalName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }

        AttributeFilter equelsFilterAccount2;
        equelsFilterAccount2 = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("displayName", "Pink"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassAccount, equelsFilterAccount2, handlerAccount, options);
        ArrayList<Uid> listUid1 = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid1.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        try {
            if (!listUid1.contains(firstUser)) {
                throw new InvalidAttributeValueException("EqualsFilter not return displayName.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        } finally {
            msGraphConnector.delete(objectClassAccount, firstUser, options);
            msGraphConnector.delete(objectClassAccount, secondUser, options);
            msGraphConnector.dispose();
        }


    }

    @Test(priority = 21)
    public void filteringTestGroupObjectClass() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();

        MSGraphConfiguration conf = getConfiguration();
        msGraphConnector.init(conf);
        OperationOptions options = new OperationOptions(new HashMap<>());

        ObjectClass objectClassGroup = ObjectClass.GROUP;

        Set<Attribute> attributesCreatedGroup = new HashSet<>();
        attributesCreatedGroup.add(AttributeBuilder.build("displayName", "GroupYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup.add(AttributeBuilder.build("mailNickname", "GroupYellow"));
        attributesCreatedGroup.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupYellow = msGraphConnector.create(objectClassGroup, attributesCreatedGroup, options);

        Set<Attribute> attributesCreatedGroup1 = new HashSet<>();
        attributesCreatedGroup1.add(AttributeBuilder.build("displayName", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailEnabled", false));
        attributesCreatedGroup1.add(AttributeBuilder.build("mailNickname", "GroupBlue"));
        attributesCreatedGroup1.add(AttributeBuilder.build("securityEnabled", true));
        Uid groupBlue = msGraphConnector.create(objectClassGroup, attributesCreatedGroup1, options);

        AttributeFilter containsFilterGroup;
        containsFilterGroup = (ContainsFilter) FilterBuilder.contains(AttributeBuilder.build("displayName", "Group"));

        final ArrayList<ConnectorObject> resultsAccount = new ArrayList<>();
        SearchResultsHandler handlerAccount = new SearchResultsHandler() {

            @Override
            public boolean handle(ConnectorObject connectorObject) {
                resultsAccount.add(connectorObject);
                return true;
            }

            @Override
            public void handleResult(SearchResult result) {
            }
        };

        msGraphConnector.executeQuery(objectClassGroup, containsFilterGroup, handlerAccount, options);
        ArrayList<Uid> listUid = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        try {
            if (!listUid.contains(groupBlue) || !listUid.contains(groupYellow)) {
                throw new InvalidAttributeValueException("ContainsFilter not return both group.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.dispose();
        }


        AttributeFilter equelsFilterAccount2;
        equelsFilterAccount2 = (EqualsFilter) FilterBuilder.equalTo(AttributeBuilder.build("mailNickname", "GroupYellow"));
        resultsAccount.clear();
        msGraphConnector.executeQuery(objectClassGroup, equelsFilterAccount2, handlerAccount, options);
        ArrayList<Uid> listUid1 = new ArrayList<>();
        for (ConnectorObject obj : resultsAccount) {
            listUid1.add((Uid) obj.getAttributeByName(Uid.NAME));
        }

        try {
            if (!listUid1.contains(groupYellow)) {
                throw new InvalidAttributeValueException("EqualsFilter not return mailNickname.");
            }
        } catch (Exception e) {
            msGraphConnector.delete(objectClassGroup, groupYellow, options);
            msGraphConnector.delete(objectClassGroup, groupBlue, options);
            msGraphConnector.dispose();
        }
        msGraphConnector.delete(objectClassGroup, groupBlue, options);
        msGraphConnector.delete(objectClassGroup, groupYellow, options);
        msGraphConnector.dispose();
    }

}
