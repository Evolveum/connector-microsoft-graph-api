package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

@Test(groups = "unit")
public class SchemaTranslatorFilterTest {
    SchemaTranslator schemaTranslator = new SchemaTranslator(null);

    @Test
    void testFilterWithAttrToGetOperationOptions() {
        OperationOptions options = new OperationOptions(Collections.singletonMap(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[]{"attr1"}));
        String[] attrs = schemaTranslator.filter(ObjectClass.ACCOUNT_NAME, options, "attr1", "attr2");

        Assert.assertEquals(1, attrs.length);
        Assert.assertEquals("attr1", attrs[0]);
    }

    @Test
    void testFilterWithReturnDefaultAttrsOperationOptions() {
        OperationOptions options = new OperationOptions(Collections.singletonMap(OperationOptions.OP_RETURN_DEFAULT_ATTRIBUTES, true));
        String[] attrs = schemaTranslator.filter(ObjectClass.ACCOUNT_NAME, options, "attr");

        // no attributes are define in schema
        Assert.assertEquals(0, attrs.length);
    }

    @Test
    void testEmptyFilter() {
        OperationOptions options = new OperationOptions(Collections.emptyMap());
        String[] attrs = schemaTranslator.filter(ObjectClass.ACCOUNT_NAME, options, "attr");

        Assert.assertEquals(1, attrs.length);
        Assert.assertEquals("attr", attrs[0]);
    }
}
