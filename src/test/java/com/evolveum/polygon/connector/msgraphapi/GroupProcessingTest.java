package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.connector.msgraphapi.integration.BasicConfigurationForTests;
import org.apache.commons.io.IOUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.polygon.connector.msgraphapi.GroupProcessing.EXCLUDE_ATTRS_OF_GROUP;
import static com.evolveum.polygon.connector.msgraphapi.GroupProcessing.UPDATABLE_MULTIPLE_VALUE_ATTRS_OF_GROUP;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Test case for {@link GroupProcessing}
 */
@Test(groups = "unit")
public class GroupProcessingTest extends BasicConfigurationForTests {

    private JSONObject parseResource(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            return new JSONObject(IOUtils.toString(is));
        }
    }

    final GroupProcessing groupProcessing = new GroupProcessing(new MockGraphEndpoint(null));

    @Test
    public void testParseGroupMembers() throws Exception {
        final JSONObject membersJson = parseResource("groupMembers.json");
        final JSONArray jarr = groupProcessing.getJSONArray(membersJson.getJSONArray("value"), "id");
        final List<Object> ids = jarr.toList();
        assertEquals(2, ids.size());
        assertEquals("9639bcbc-0089-4855-a793-44b940e52286", ids.get(0));
        assertEquals("f034f71e-22a8-489b-8492-f5f7133559c1", ids.get(1));
    }

    @Test
    public void testBuildLayeredAttributeJSON() {
        Set<Attribute> attrs = new HashSet<>();
        attrs.add(AttributeBuilder.build("displayName", "group1"));
        attrs.add(AttributeBuilder.build("description", "This is\r\n\"group1\"."));
        // Exclude
        attrs.add(AttributeBuilder.build("members", "9639bcbc-0089-4855-a793-44b940e52286"));
        attrs.add(AttributeBuilder.build("owners", "9639bcbc-0089-4855-a793-44b940e52286"));

        JSONObject jsonObject = groupProcessing.buildLayeredAttributeJSON(attrs, EXCLUDE_ATTRS_OF_GROUP);

        assertEquals("group1", jsonObject.getString("displayName"));
        assertEquals("This is\r\n\"group1\".", jsonObject.getString("description"));
    }

    @Test
    public void testBuildLayeredAttribute() {
        Set<AttributeDelta> attrs = new HashSet<>();
        attrs.add(AttributeDeltaBuilder.build("displayName", "group1"));
        attrs.add(AttributeDeltaBuilder.build("description", "This is\r\n\"group1\"."));
        // Exclude
        attrs.add(AttributeDeltaBuilder.build("members", list("9639bcbc-0089-4855-a793-44b940e52286"), null));
        attrs.add(AttributeDeltaBuilder.build("owners", list("9639bcbc-0089-4855-a793-44b940e52286"), null));

        List<JSONObject> jsonObjects = groupProcessing.buildLayeredAttribute(null, attrs, EXCLUDE_ATTRS_OF_GROUP, Collections.emptySet());

        assertEquals(1, jsonObjects.size());
        JSONObject jsonObject = jsonObjects.get(0);
        assertEquals(2, jsonObject.length());
        assertEquals("group1", jsonObject.getString("displayName"));
        assertEquals("This is\r\n\"group1\".", jsonObject.getString("description"));
    }

    @Test
    public void testBuildLayeredAttributeWithOld() {
        Set<AttributeDelta> attrs = new HashSet<>();
        attrs.add(AttributeDeltaBuilder.build("displayName", "group1"));
        attrs.add(AttributeDeltaBuilder.build("description", "This is\r\n\"group1\"."));
        attrs.add(AttributeDeltaBuilder.build("groupTypes", list("DynamicMembership"), list("Unified")));
        // Exclude
        attrs.add(AttributeDeltaBuilder.build("members", list("9639bcbc-0089-4855-a793-44b940e52286"), null));
        attrs.add(AttributeDeltaBuilder.build("owners", list("9639bcbc-0089-4855-a793-44b940e52286"), null));
        // Old json
        JSONObject oldJson = new JSONObject();
        oldJson.append("groupTypes", "Unified");

        List<JSONObject> jsonObjects = groupProcessing.buildLayeredAttribute(oldJson, attrs, EXCLUDE_ATTRS_OF_GROUP, Collections.emptySet());

        assertEquals(1, jsonObjects.size());
        JSONObject jsonObject = jsonObjects.get(0);
        assertEquals(3, jsonObject.length());
        assertEquals("group1", jsonObject.getString("displayName"));
        assertEquals("This is\r\n\"group1\".", jsonObject.getString("description"));
        JSONArray groupTypes = jsonObject.getJSONArray("groupTypes");
        assertEquals(1, groupTypes.length());
        assertEquals("DynamicMembership", groupTypes.getString(0));
    }
}
