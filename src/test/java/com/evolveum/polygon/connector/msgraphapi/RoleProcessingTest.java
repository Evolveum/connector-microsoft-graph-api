package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.connector.msgraphapi.integration.BasicConfigurationForTests;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Test case for {@link RoleProcessing}
 */
@Test(groups = "unit")
public class RoleProcessingTest extends BasicConfigurationForTests {

    private JSONObject parseResource(String fileName) throws IOException  {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            return new JSONObject(IOUtils.toString(is));
        }
    }

    final RoleProcessing roleProcessing = new RoleProcessing(new MockGraphEndpoint(null));

    @Test
    public void testParseGroupMembers() throws Exception {
        final JSONObject membersJson = parseResource("groupMembers.json");
        final JSONArray jarr = roleProcessing.getJSONArray(membersJson.getJSONArray("value"), "id");
        final List<Object> ids = jarr.toList();
        assertEquals(2, ids.size());
        assertEquals("9639bcbc-0089-4855-a793-44b940e52286", ids.get(0));
        assertEquals("f034f71e-22a8-489b-8492-f5f7133559c1", ids.get(1));
    }
}
