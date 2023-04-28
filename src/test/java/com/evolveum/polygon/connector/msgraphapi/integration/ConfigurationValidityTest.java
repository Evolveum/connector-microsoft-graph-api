package com.evolveum.polygon.connector.msgraphapi.integration;

import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.testng.annotations.Test;

import java.util.Properties;


public class ConfigurationValidityTest extends BasicConfigurationForTests {


    @Test(expectedExceptions = RuntimeException.class)
    public void creteTestBadConfiguration() {
        msGraphConfiguration = getConfiguration();
        msGraphConfiguration.setTenantId("None");
        msGraphConnector.init(msGraphConfiguration);

            msGraphConnector.test();
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void creteTestEmptyConfiguration() {
        msGraphConfiguration = getConfiguration();
        msGraphConfiguration.setClientId("");
        msGraphConnector.init(msGraphConfiguration);

            msGraphConnector.test();
    }

    @Test
    public void testPartialConfiguration() {
        msGraphConfiguration = getConfiguration();
        msGraphConnector.init(msGraphConfiguration);

        msGraphConnector.testPartialConfiguration();
    }
}
