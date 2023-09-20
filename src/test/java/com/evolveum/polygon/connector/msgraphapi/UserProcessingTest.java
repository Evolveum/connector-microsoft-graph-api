package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.connector.msgraphapi.integration.BasicConfigurationForTests;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.polygon.connector.msgraphapi.UserProcessing.SPO_ATTRS;
import static org.testng.AssertJUnit.*;

/**
 * Test case for {@link UserProcessing}
 */
@Test(groups = "unit")
public class UserProcessingTest extends BasicConfigurationForTests {

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

        JSONObject jsonObject = userProcessing.buildLayeredAttributeJSON(attrs);

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

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("businessPhones", "+1 425 555 0109"));
        attrs.add(AttributeBuilder.build("officeLocation", "18/2111"));
        attrs.add(AttributeBuilder.build("givenName", Collections.emptyList())); // Case of value removal
        attrs.add(AttributeBuilder.build("onPremisesExtensionAttributes.extensionAttribute1", "ext1"));
        attrs.add(AttributeBuilder.build("onPremisesExtensionAttributes.extensionAttribute15", "ext15"));
        attrs.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        attrs.add(AttributeBuilder.build(ATTR_ICF_PASSWORD, new GuardedString("xWwvJ]6NMw+bWH-d".toCharArray())));
        attrs.add(AttributeBuilder.build(ATTR_ICF_ENABLED, false));

        List<JSONObject> jsonObjects = userProcessing.buildLayeredAttribute(attrs, SPO_ATTRS);

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

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("businessPhones", "+1 425 555 0109"));
        attrs.add(AttributeBuilder.build("officeLocation", "18/2111"));
        attrs.add(AttributeBuilder.build("passwordProfile.forceChangePasswordNextSignIn", true));
        // SPO
        attrs.add(AttributeBuilder.build("aboutMe", "About me"));
        attrs.add(AttributeBuilder.build("mySite", "https://example.com"));
        attrs.add(AttributeBuilder.build("schools", "A", "B"));

        List<JSONObject> jsonObjects = userProcessing.buildLayeredAttribute(attrs, SPO_ATTRS);

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

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("aboutMe", "About me"));
        attrs.add(AttributeBuilder.build("mySite", "https://example.com"));

        List<JSONObject> jsonObjects = userProcessing.buildLayeredAttribute(attrs, SPO_ATTRS);

        assertEquals(1, jsonObjects.size());
        JSONObject spoJsonObject = jsonObjects.get(0);
        assertEquals(2, spoJsonObject.length());
        assertEquals("About me", spoJsonObject.getString("aboutMe"));
        assertEquals("https://example.com", spoJsonObject.getString("mySite"));
    }
}
