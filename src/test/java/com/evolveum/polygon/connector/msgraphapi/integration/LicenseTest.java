package com.evolveum.polygon.connector.msgraphapi.integration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.identityconnectors.test.common.ToListResultsHandler;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.polygon.connector.msgraphapi.LicenseProcessing;
import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;
import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;


public class LicenseTest extends BasicConfigurationForTests {
    private static final Log LOG = Log.getLog(LicenseTest.class);

    MSGraphConnector conn;
    final String nickname = "testing";
    final String ATTR_LICENSES = "assignedLicenses.skuId";
    Set<Uid> users = new HashSet<>();

    @BeforeClass
    public void setUp() {
        LOG.info("==== setUp ==== ");
        conn = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        conn.init(conf);
    }

    @AfterClass
    public void tearDown() {
        users.forEach(uid -> {
            try {
                deleteUser(uid);
            } catch (UnknownUidException e) {}
        });
        conn.dispose();
    }

    Uid createUser(Collection<Attribute> initialAttributes) {
        LOG.info("creating user, nickname {0}", nickname);
        Set<Attribute> attributesAccount = new HashSet<>();
        attributesAccount.add(AttributeBuilder.build("accountEnabled", true));
        attributesAccount.add(AttributeBuilder.build("displayName", "License Testing"));
        attributesAccount.add(AttributeBuilder.build("mail", nickname + "@example.com"));
        attributesAccount.add(AttributeBuilder.build("mailNickname", nickname));
        attributesAccount.add(AttributeBuilder.build("userPrincipalName", nickname + "@" + tenantId));
        attributesAccount.add(AttributeBuilder.build("usageLocation", "US")); // required for licenses
        GuardedString secret = new GuardedString("SpaghettiMonstersAreReal!".toCharArray());
        attributesAccount.add(AttributeBuilder.build("__PASSWORD__", secret));
        if (initialAttributes != null)
            attributesAccount.addAll(initialAttributes);
        Uid uid = conn.create(ObjectClass.ACCOUNT, attributesAccount, null);
        if (uid != null)
            users.add(uid);
        return uid;
    }

    void deleteUser(Uid uid) {
        LOG.info("deleting user {0}", uid.getUidValue());
        conn.delete(ObjectClass.ACCOUNT, uid, null);
        users.remove(uid);
    }

    ConnectorObject getUser(Uid uid) {
        ToListResultsHandler handler = new ToListResultsHandler();
        Map<String,Object> options = new HashMap<>();
        conn.executeQuery(ObjectClass.ACCOUNT, FilterBuilder.equalTo(uid), handler, new OperationOptions(options));
        assertNotNull(handler.getObjects(), "result objects");
        assertEquals(handler.getObjects().size(), 1, "result objects size");
        return handler.getObjects().get(0);
    }

    void check(ConnectorObject co, String skuId, boolean has) {
        Attribute attr = co.getAttributeByName(ATTR_LICENSES);
        boolean found;
        if (attr == null)
            found = false;
        else
            found = attr.getValue().stream().anyMatch(it -> skuId.equals(it.toString()));
        LOG.info("check(co, {0}, {1}) => attr {2}, found {3}", skuId, has, attr, found);
        if (has)
            assertTrue(found, "License " + skuId + " should be there");
        else
            assertFalse(found, "License " + skuId + " should not be there");
    }

    @Test
    public void listAllTest() {
        LOG.info("==== listAllTest ==== ");

        ToListResultsHandler handler = new ToListResultsHandler();
        Map<String,Object> options = new HashMap<>();
        conn.executeQuery(LicenseProcessing.OBJECT_CLASS, null, handler, new OperationOptions(options));

        Set<String> remains = CollectionUtil.newSet(licenses);
        if (handler.getObjects() != null) {
            for (ConnectorObject co : handler.getObjects()) {
                assertNotNull(co.getUid(), Uid.NAME);
                assertNotNull(co.getName(), Name.NAME);
                Attribute attrSkuId = co.getAttributeByName(LicenseProcessing.ATTR_SKUID);
                String skuId = attrSkuId == null ? null : AttributeUtil.getAsStringValue(attrSkuId);
                LOG.info("License: skuId {0}, skuPartNumber {1}", skuId, co.getName().getNameValue());
                if (skuId != null)
                    remains.remove(skuId);
            }
        }
        if (!remains.isEmpty())
            LOG.error("Following licenses not found: {0}", remains);
        assertTrue(remains.isEmpty(), "All licenses found");
    }

    @Test
    public void addRemoveLicenseTest() {
        LOG.info("==== addRemoveLicenseTest ==== ");
        if (licenses.isEmpty())
            throw new SkipException("License skuIds not specified, skipping");

        ConnectorObject co;
        Set<AttributeDelta> changes;

        Uid uid = createUser(null);
        Set<AttributeDelta> deltas = new HashSet<>();
        //String skuId = licenses.stream().findFirst().get();
        OperationOptions options = new OperationOptions(new HashMap<>());

        LOG.info("==== addRemoveLicenseTest (add) ==== ");
        deltas.clear();
        deltas.add(AttributeDeltaBuilder.build("assignedLicenses.skuId", licenses, null));
        changes = conn.updateDelta(ObjectClass.ACCOUNT, uid, deltas, options);
        assertTrue(changes == null || changes.isEmpty(), "no side changes");
        co = getUser(uid);
        for (String skuId : licenses)
            check(co, skuId, true);

        LOG.info("==== addRemoveLicenseTest (remove) ==== ");
        deltas.clear();
        deltas.add(AttributeDeltaBuilder.build("assignedLicenses.skuId", null, licenses));
        changes = conn.updateDelta(ObjectClass.ACCOUNT, uid, deltas, options);
        assertTrue(changes == null || changes.isEmpty(), "no side changes");
        co = getUser(uid);
        for (String skuId : licenses)
            check(co, skuId, false);

        deleteUser(uid);
    }

    @Test
    public void createWithLicenseTest() {
        LOG.info("==== createWithLicenseTest ==== ");
        if (licenses.isEmpty())
            throw new SkipException("Licenses not specified, skipping");

        Uid uid;
        ConnectorObject co;

        Set<Attribute> attributes = new HashSet<>();

        attributes.add(AttributeBuilder.build(ATTR_LICENSES, licenses));
        uid = createUser(attributes);
        assertNotNull(uid, "UID of created user");
        co = getUser(uid);
        for (String skuId : licenses)
            check(co, skuId, true);

      deleteUser(uid);
    }

    @Test
    public void updateTest() {
        LOG.info("==== updateTest ==== ");
        if (licenses.isEmpty())
            throw new SkipException("Licenses not specified, skipping");

        Uid uid;
        ConnectorObject co;
        OperationOptions options = new OperationOptions(new HashMap<>());

        uid = createUser(null);
        assertNotNull(uid, "UID of created user");
        co = getUser(uid);
        for (String skuId : licenses)
            check(co, skuId, false);

        Set<Attribute> attributes = new HashSet<>();
        attributes.add(AttributeBuilder.build(ATTR_LICENSES, licenses));
        conn.update(ObjectClass.ACCOUNT, uid, attributes, options);
        co = getUser(uid);
        for (String skuId : licenses)
            check(co, skuId, true);

        attributes.clear();
        attributes.add(AttributeBuilder.build(ATTR_LICENSES, licenses2));
        conn.update(ObjectClass.ACCOUNT, uid, attributes, options);
        co = getUser(uid);
        Set<String> removedLicenses = CollectionUtil.newSet(licenses);
        removedLicenses.removeAll(licenses2);
        for (String skuId : removedLicenses)
            check(co, skuId, false);
        for (String skuId : licenses2)
            check(co, skuId, true);

        deleteUser(uid);
    }
}
