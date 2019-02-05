package com.evolveum.polygon;

import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.testng.annotations.Test;


public class ConfigurationValidityTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = RuntimeException.class)
    public void creteTestBadConfiguration() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        conf.setTenantId("None");
        msGraphConnector.init(conf);
        try {
            msGraphConnector.test();
        } finally {
            msGraphConnector.dispose();
        }
    }

    @Test(expectedExceptions = ConfigurationException.class)
    public void creteTestEmptyConfiguration() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        conf.setClientId("");
        msGraphConnector.init(conf);
        try {
            msGraphConnector.test();
        } finally {
            msGraphConnector.dispose();
        }
    }

}
