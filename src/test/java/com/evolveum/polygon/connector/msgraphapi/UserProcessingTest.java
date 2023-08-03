package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.connector.msgraphapi.integration.BasicConfigurationForTests;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test case for {@link UserProcessing}
 */
@Test(groups = "unit")
public class UserProcessingTest extends BasicConfigurationForTests {

    @Test
    public void testGetAttributesToGet() throws Exception {
        UserProcessing userProcessing = new UserProcessing(null, null);
        Assert.assertNotNull(userProcessing.getAttributesToGet(null));

        OperationOptions emptyOptions = new OperationOptionsBuilder().build();
        Assert.assertNotNull(userProcessing.getAttributesToGet(emptyOptions));

        OperationOptions options = new OperationOptionsBuilder().setAttributesToGet("manager").build();

        Assert.assertTrue(userProcessing.getAttributesToGet(options).contains("manager"));
        Assert.assertFalse(userProcessing.getAttributesToGet(options).contains("signIn"));
    }
}
