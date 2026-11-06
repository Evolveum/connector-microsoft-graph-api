package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.connector.msgraphapi.integration.BasicConfigurationForTests;
import org.identityconnectors.common.security.GuardedString;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.polygon.connector.msgraphapi.UserProcessing.EXCLUDE_ATTRS_OF_USER;
import static com.evolveum.polygon.connector.msgraphapi.UserProcessing.SPO_ATTRS;
import static org.testng.AssertJUnit.*;

/**
 * Test case for {@link UserProcessing}
 */
@Test(groups = "unit")
public class UserProcessingTest extends BasicConfigurationForTests {

    private static class RecordingGraphEndpoint extends MockGraphEndpoint {
        private JSONObject lastPayload;
        private String lastPath;

        RecordingGraphEndpoint(MSGraphConfiguration configuration) {
            super(configuration);
        }

        @Override
        public JSONObject callRequest(HttpEntityEnclosingRequestBase request, JSONObject json, Boolean parseResult) {
            this.lastPath = request.getURI().getPath();
            this.lastPayload = json;
            return new JSONObject().put("invitedUser", new JSONObject().put("id", "invited-user-id"));
        }
    }

    @Test
    public void testGetAttributesToGet() throws Exception {
        UserProcessing userProcessing = new UserProcessing(null, null);
        assertNotNull(userProcessing.getAttributesToGet(null));

        OperationOptions emptyOptions = new OperationOptionsBuilder().build();
        assertNotNull(userProcessing.getAttributesToGet(emptyOptions));

        OperationOptions options = new OperationOptionsBuilder().setAttributesToGet("manager").build();

        assertTrue(userProcessing.getAttributesToGet(options).contains("manager"));
        assertFalse(userProcessing.getAttributesToGet(options).contains("signIn"));
    }

    @Test
    public void testOtherMailsSchemaAndSelectors() {
        MockGraphEndpoint mockGraphEndpoint = new MockGraphEndpoint(null);
        UserProcessing userProcessing = new UserProcessing(mockGraphEndpoint, mockGraphEndpoint.getSchemaTranslator());

        AttributeInfo otherMails = userProcessing.objectClassInfo().getAttributeInfo().stream()
                .filter(info -> "otherMails".equals(info.getName()))
                .findFirst().orElseThrow();

        assertEquals(String.class, otherMails.getType());
        assertTrue(otherMails.isMultiValued());
        assertTrue(userProcessing.getSelectorSingle(null).contains("otherMails"));
        assertTrue(userProcessing.getSelectorSingle(new OperationOptionsBuilder()
                .setAttributesToGet("otherMails").build()).contains("otherMails"));
    }

    @Test
    public void testOtherMailsConversionAndArrayPayload() {
        MockGraphEndpoint mockGraphEndpoint = new MockGraphEndpoint(null);
        UserProcessing userProcessing = new UserProcessing(mockGraphEndpoint, mockGraphEndpoint.getSchemaTranslator());
        JSONObject userJson = new JSONObject()
                .put("id", "user-id")
                .put("userPrincipalName", "user@example.com")
                .put("otherMails", new JSONArray().put("first@example.com").put("second@example.com"));

        Attribute otherMails = userProcessing.convertUserJSONObjectToConnectorObject(userJson)
                .build().getAttributeByName("otherMails");
        assertEquals(list("first@example.com", "second@example.com"), otherMails.getValue());

        JSONObject payload = userProcessing.buildLayeredAttributeJSON(
                Collections.singleton(AttributeBuilder.build("otherMails", list("first@example.com", "second@example.com"))),
                EXCLUDE_ATTRS_OF_USER);
        assertEquals(new JSONArray().put("first@example.com").put("second@example.com").toString(),
                payload.getJSONArray("otherMails").toString());
    }

    @Test
    public void testBuildLayeredAttributeJSON() {
        MockGraphEndpoint mockGraphEndpoint = new MockGraphEndpoint(null);
        SchemaTranslator schemaTranslator = mockGraphEndpoint.getSchemaTranslator();
        UserProcessing userProcessing = new UserProcessing(mockGraphEndpoint, schemaTranslator);

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("userPrincipalName", "foo@example.com"));
        attrs.add(AttributeBuilder.build("mailNickname", "foo"));
        attrs.add(AttributeBuilder.build("displayName", "Foo Bar"));
        attrs.add(AttributeBuilder.build("mail", "foo@example.com"));
        attrs.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attrs.add(AttributeBuilder.build(ATTR_ICF_PASSWORD, new GuardedString("xWwvJ]6NMw+bWH-d".toCharArray())));
        attrs.add(AttributeBuilder.build(ATTR_ICF_ENABLED, true));
        // Excluded
        attrs.add(AttributeBuilder.build("manager.id", "08fa38e4-cbfa-4488-94ed-c834da6539df"));
        attrs.add(AttributeBuilder.build("assignedLicenses.skuId", "dummySkuId"));
        attrs.add(AttributeBuilder.build("photo", new byte[]{0, 1, 2}));

        JSONObject jsonObject = userProcessing.buildLayeredAttributeJSON(attrs, EXCLUDE_ATTRS_OF_USER);

        assertEquals(6, jsonObject.length());
        assertEquals("foo@example.com", jsonObject.getString("userPrincipalName"));
        assertEquals("foo", jsonObject.getString("mailNickname"));
        assertEquals("Foo Bar", jsonObject.getString("displayName"));
        assertEquals("foo@example.com", jsonObject.getString("mail"));
        assertTrue(jsonObject.getBoolean("accountEnabled"));
        JSONObject passwordProfile = jsonObject.getJSONObject("passwordProfile");
        assertNotNull(passwordProfile);
        assertEquals(2, passwordProfile.length());
        assertTrue(passwordProfile.getBoolean("forceChangePasswordNextSignIn"));
        assertEquals("xWwvJ]6NMw+bWH-d", passwordProfile.getString("password"));
    }

    @Test
    public void testBuildLayeredAttribute() {
        MockGraphEndpoint mockGraphEndpoint = new MockGraphEndpoint(null);
        SchemaTranslator schemaTranslator = mockGraphEndpoint.getSchemaTranslator();
        UserProcessing userProcessing = new UserProcessing(mockGraphEndpoint, schemaTranslator);

        Set<AttributeDelta> attrs = new HashSet<>();
        attrs.add(AttributeDeltaBuilder.build("businessPhones", list("+1 425 555 0109"), null));
        attrs.add(AttributeDeltaBuilder.build("officeLocation", "18/2111"));
        attrs.add(AttributeDeltaBuilder.build("givenName", Collections.emptyList())); // Case of value removal
        attrs.add(AttributeDeltaBuilder.build("onPremisesExtensionAttributes.extensionAttribute1", "ext1"));
        attrs.add(AttributeDeltaBuilder.build("onPremisesExtensionAttributes.extensionAttribute15", "ext15"));
        attrs.add(AttributeDeltaBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attrs.add(AttributeDeltaBuilder.build(ATTR_ICF_PASSWORD, new GuardedString("xWwvJ]6NMw+bWH-d".toCharArray())));
        attrs.add(AttributeDeltaBuilder.build(ATTR_ICF_ENABLED, false));
        // Excluded
        attrs.add(AttributeDeltaBuilder.build("manager.id", "08fa38e4-cbfa-4488-94ed-c834da6539df"));
        attrs.add(AttributeDeltaBuilder.build("assignedLicenses.skuId", list("dummySkuId"), null));
        attrs.add(AttributeDeltaBuilder.build("photo", new byte[]{0, 1, 2}));

        List<JSONObject> jsonObjects = userProcessing.buildLayeredAttribute(null, attrs, EXCLUDE_ATTRS_OF_USER, SPO_ATTRS);

        assertEquals(1, jsonObjects.size());
        JSONObject jsonObject = jsonObjects.get(0);
        assertEquals(6, jsonObject.length());
        JSONArray businessPhones = jsonObject.getJSONArray("businessPhones");
        assertEquals(1, businessPhones.length());
        assertEquals("+1 425 555 0109", businessPhones.getString(0));
        assertTrue(jsonObject.isNull("givenName"));
        assertEquals("18/2111", jsonObject.getString("officeLocation"));
        JSONObject onPremisesExtensionAttributes = jsonObject.getJSONObject("onPremisesExtensionAttributes");
        assertNotNull(onPremisesExtensionAttributes);
        assertEquals("ext1", onPremisesExtensionAttributes.getString("extensionAttribute1"));
        assertEquals("ext15", onPremisesExtensionAttributes.getString("extensionAttribute15"));
        JSONObject passwordProfile = jsonObject.getJSONObject("passwordProfile");
        assertNotNull(passwordProfile);
        assertEquals(2, passwordProfile.length());
        assertTrue(passwordProfile.getBoolean("forceChangePasswordNextSignIn"));
        assertEquals("xWwvJ]6NMw+bWH-d", passwordProfile.getString("password"));
        assertFalse(jsonObject.getBoolean("accountEnabled"));
    }

    @Test
    public void testBuildLayeredAttributeWithSPO() {
        MockGraphEndpoint mockGraphEndpoint = new MockGraphEndpoint(null);
        SchemaTranslator schemaTranslator = mockGraphEndpoint.getSchemaTranslator();
        UserProcessing userProcessing = new UserProcessing(mockGraphEndpoint, schemaTranslator);

        Set<AttributeDelta> attrs = new HashSet<>();
        attrs.add(AttributeDeltaBuilder.build("businessPhones", list("+1 425 555 0109"), null));
        attrs.add(AttributeDeltaBuilder.build("officeLocation", "18/2111"));
        attrs.add(AttributeDeltaBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        // SPO
        attrs.add(AttributeDeltaBuilder.build("aboutMe", "About me"));
        attrs.add(AttributeDeltaBuilder.build("mySite", "https://example.com"));
        attrs.add(AttributeDeltaBuilder.build("schools", list("A", "B"), null));
        // Excluded
        attrs.add(AttributeDeltaBuilder.build("manager.id", "08fa38e4-cbfa-4488-94ed-c834da6539df"));
        attrs.add(AttributeDeltaBuilder.build("assignedLicenses.skuId", list("dummySkuId"), null));
        attrs.add(AttributeDeltaBuilder.build("photo", new byte[]{0, 1, 2}));

        List<JSONObject> jsonObjects = userProcessing.buildLayeredAttribute(null, attrs, EXCLUDE_ATTRS_OF_USER, SPO_ATTRS);

        assertEquals(2, jsonObjects.size());
        JSONObject jsonObject = jsonObjects.get(0);
        assertEquals(3, jsonObject.length());
        JSONArray businessPhones = jsonObject.getJSONArray("businessPhones");
        assertEquals(1, businessPhones.length());
        assertEquals("+1 425 555 0109", businessPhones.getString(0));
        assertEquals("18/2111", jsonObject.getString("officeLocation"));
        JSONObject passwordProfile = jsonObject.getJSONObject("passwordProfile");
        assertNotNull(passwordProfile);
        assertEquals(1, passwordProfile.length());
        assertTrue(passwordProfile.getBoolean("forceChangePasswordNextSignIn"));

        JSONObject spoJsonObject = jsonObjects.get(1);
        assertEquals(3, spoJsonObject.length());
        assertEquals("About me", spoJsonObject.getString("aboutMe"));
        assertEquals("https://example.com", spoJsonObject.getString("mySite"));
        JSONArray schools = spoJsonObject.getJSONArray("schools");
        assertEquals(2, schools.length());
        assertEquals("A", schools.getString(0));
        assertEquals("B", schools.getString(1));
    }

    @Test
    public void testBuildLayeredAttributeSPOOnly() {
        MockGraphEndpoint mockGraphEndpoint = new MockGraphEndpoint(null);
        SchemaTranslator schemaTranslator = mockGraphEndpoint.getSchemaTranslator();
        UserProcessing userProcessing = new UserProcessing(mockGraphEndpoint, schemaTranslator);

        Set<AttributeDelta> attrs = new HashSet<>();
        attrs.add(AttributeDeltaBuilder.build("aboutMe", "About me"));
        attrs.add(AttributeDeltaBuilder.build("mySite", "https://example.com"));

        List<JSONObject> jsonObjects = userProcessing.buildLayeredAttribute(null, attrs, EXCLUDE_ATTRS_OF_USER, SPO_ATTRS);

        assertEquals(1, jsonObjects.size());
        JSONObject spoJsonObject = jsonObjects.get(0);
        assertEquals(2, spoJsonObject.length());
        assertEquals("About me", spoJsonObject.getString("aboutMe"));
        assertEquals("https://example.com", spoJsonObject.getString("mySite"));
    }

    @Test
    public void testBuildLayeredAttributeWithOld() {
        MockGraphEndpoint mockGraphEndpoint = new MockGraphEndpoint(null);
        SchemaTranslator schemaTranslator = mockGraphEndpoint.getSchemaTranslator();
        UserProcessing userProcessing = new UserProcessing(mockGraphEndpoint, schemaTranslator);

        Set<AttributeDelta> attrs = new HashSet<>();
        attrs.add(AttributeDeltaBuilder.build("businessPhones", list("+1 425 555 0109"), list("+1 858 555 0110")));
        attrs.add(AttributeDeltaBuilder.build("officeLocation", "18/2111"));
        attrs.add(AttributeDeltaBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        // SPO
        attrs.add(AttributeDeltaBuilder.build("schools", list("A", "B"), list("C", "D")));
        attrs.add(AttributeDeltaBuilder.build("interests", list("a"), list("c")));
        // Old json
        JSONObject oldJson = new JSONObject();
        oldJson.append("businessPhones", "+1 858 555 0110");
        oldJson.append("schools", "C");
        oldJson.append("schools", "D");
        oldJson.append("schools", "E");
        oldJson.append("interests", "a");
        oldJson.append("interests", "b");

        List<JSONObject> jsonObjects = userProcessing.buildLayeredAttribute(oldJson, attrs, EXCLUDE_ATTRS_OF_USER, SPO_ATTRS);

        assertEquals(2, jsonObjects.size());
        JSONObject jsonObject = jsonObjects.get(0);
        assertEquals(3, jsonObject.length());
        JSONArray businessPhones = jsonObject.getJSONArray("businessPhones");
        assertEquals(1, businessPhones.length());
        assertEquals("+1 425 555 0109", businessPhones.getString(0));
        assertEquals("18/2111", jsonObject.getString("officeLocation"));
        JSONObject passwordProfile = jsonObject.getJSONObject("passwordProfile");
        assertNotNull(passwordProfile);
        assertEquals(1, passwordProfile.length());
        assertTrue(passwordProfile.getBoolean("forceChangePasswordNextSignIn"));

        JSONObject spoJsonObject = jsonObjects.get(1);
        assertEquals(2, spoJsonObject.length());
        JSONArray schools = spoJsonObject.getJSONArray("schools");
        assertEquals(3, schools.length());
        assertEquals("E", schools.getString(0));
        assertEquals("A", schools.getString(1));
        assertEquals("B", schools.getString(2));
        JSONArray interests = spoJsonObject.getJSONArray("interests");
        assertEquals(2, interests.length());
        assertEquals("a", interests.getString(0));
        assertEquals("b", interests.getString(1));
    }

    @Test
    public void testCreateGuestUsesInvitationWhenUpnAndPasswordAreMissing() {
        MSGraphConfiguration configuration = new MSGraphConfiguration();
        configuration.setTenantId("example.com");
        configuration.setInviteGuests(true);
        configuration.setSendInviteMail(true);
        configuration.setInviteRedirectUrl("https://myapps.microsoft.com");

        RecordingGraphEndpoint endpoint = new RecordingGraphEndpoint(configuration);
        UserProcessing userProcessing = new UserProcessing(endpoint, endpoint.getSchemaTranslator());

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("mail", "external@example.com"));
        attrs.add(AttributeBuilder.build("displayName", "External Guest"));
        attrs.add(AttributeBuilder.build("userType", "Guest"));

        Uid uid = userProcessing.createUser(attrs);

        assertEquals("invited-user-id", uid.getUidValue());
        assertEquals("/v1.0/invitations", endpoint.lastPath);
        assertNotNull(endpoint.lastPayload);
        assertEquals("external@example.com", endpoint.lastPayload.getString("invitedUserEmailAddress"));
        assertEquals("External Guest", endpoint.lastPayload.getString("invitedUserDisplayName"));
        assertEquals("Guest", endpoint.lastPayload.getString("invitedUserType"));
        assertTrue(endpoint.lastPayload.getBoolean("sendInvitationMessage"));
        assertEquals("https://myapps.microsoft.com", endpoint.lastPayload.getString("inviteRedirectUrl"));
        assertFalse(endpoint.lastPayload.has("invitedUserMessageInfo"));
    }

    @Test(expectedExceptions = InvalidAttributeValueException.class,
            expectedExceptionsMessageRegExp = ".*userPrincipalName must not be provided.*")
    public void testCreateGuestInvitationRejectsUserPrincipalName() {
        MSGraphConfiguration configuration = new MSGraphConfiguration();
        configuration.setTenantId("example.com");
        configuration.setInviteGuests(true);
        configuration.setInviteRedirectUrl("https://myapps.microsoft.com");

        RecordingGraphEndpoint endpoint = new RecordingGraphEndpoint(configuration);
        UserProcessing userProcessing = new UserProcessing(endpoint, endpoint.getSchemaTranslator());

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("mail", "external@example.com"));
        attrs.add(AttributeBuilder.build("displayName", "External Guest"));
        attrs.add(AttributeBuilder.build("userType", "Guest"));
        attrs.add(AttributeBuilder.build("userPrincipalName", "external_example.com#EXT#@tenant.onmicrosoft.com"));

        userProcessing.createUser(attrs);
    }

    @Test
    public void testCreateGuestInvitationWrapsCustomMessage() {
        MSGraphConfiguration configuration = invitationConfiguration();
        configuration.setSendInviteMail(true);
        configuration.setInviteMessage("Welcome to the organization.");

        RecordingGraphEndpoint endpoint =
                new RecordingGraphEndpoint(configuration);

        UserProcessing userProcessing =
                new UserProcessing(
                        endpoint,
                        endpoint.getSchemaTranslator());

        userProcessing.createUser(guestAttributes());

        JSONObject payload = endpoint.lastPayload;

        assertNotNull(payload);
        assertTrue(payload.has("invitedUserMessageInfo"));

        Object messageInfoValue =
                payload.get("invitedUserMessageInfo");

        assertTrue(
                "invitedUserMessageInfo must be a JSON object",
                messageInfoValue instanceof JSONObject);

        JSONObject messageInfo =
                (JSONObject) messageInfoValue;

        assertEquals(
                "Welcome to the organization.",
                messageInfo.getString(
                        "customizedMessageBody"));

        assertTrue(
                payload.getBoolean(
                        "sendInvitationMessage"));
    }

    @Test
    public void testCreateGuestInvitationOmitsEmptyMessageInfo() {
        MSGraphConfiguration configuration = invitationConfiguration();
        configuration.setSendInviteMail(true);
        configuration.setInviteMessage("");

        RecordingGraphEndpoint endpoint =
                new RecordingGraphEndpoint(configuration);

        UserProcessing userProcessing =
                new UserProcessing(
                        endpoint,
                        endpoint.getSchemaTranslator());

        userProcessing.createUser(guestAttributes());

        JSONObject payload = endpoint.lastPayload;

        assertNotNull(payload);

        assertFalse(
                payload.has(
                        "invitedUserMessageInfo"));

        assertTrue(
                payload.getBoolean(
                        "sendInvitationMessage"));
    }

    @Test(expectedExceptions = InvalidAttributeValueException.class,
            expectedExceptionsMessageRegExp = ".*Password must not be provided.*")
    public void testCreateGuestInvitationRejectsPassword() {
        MSGraphConfiguration configuration = new MSGraphConfiguration();
        configuration.setTenantId("example.com");
        configuration.setInviteGuests(true);
        configuration.setInviteRedirectUrl("https://myapps.microsoft.com");

        RecordingGraphEndpoint endpoint = new RecordingGraphEndpoint(configuration);
        UserProcessing userProcessing = new UserProcessing(endpoint, endpoint.getSchemaTranslator());

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("mail", "external@example.com"));
        attrs.add(AttributeBuilder.build("displayName", "External Guest"));
        attrs.add(AttributeBuilder.build("userType", "Guest"));
        attrs.add(AttributeBuilder.build(ATTR_ICF_PASSWORD, new GuardedString("DoNotUseThisPassword1".toCharArray())));

        userProcessing.createUser(attrs);
    }

    private MSGraphConfiguration invitationConfiguration() {
        MSGraphConfiguration configuration =
                new MSGraphConfiguration();

        configuration.setTenantId("example.com");
        configuration.setInviteGuests(true);
        configuration.setInviteRedirectUrl(
                "https://myapplications.microsoft.com");

        return configuration;
    }

    private Set<Attribute> guestAttributes() {
        Set<Attribute> attributes = new HashSet<>();

        attributes.add(
                AttributeBuilder.build(
                        "mail",
                        "external@example.com"));

        attributes.add(
                AttributeBuilder.build(
                        "displayName",
                        "External Guest"));

        attributes.add(
                AttributeBuilder.build(
                        "userType",
                        "Guest"));

        return attributes;
    }
}
