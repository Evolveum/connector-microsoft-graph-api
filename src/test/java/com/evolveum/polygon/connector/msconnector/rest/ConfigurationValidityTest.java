package com.evolveum.polygon.connector.msconnector.rest;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.testng.annotations.Test;


public class ConfigurationValidityTest extends BasicConfigurationForTests {

    @Test(expectedExceptions = ConfigurationException.class)
    public void creteTestBadConfiguration() {
        MSGraphConnector msGraphConnector = new MSGraphConnector();
        MSGraphConfiguration conf = getConfiguration();
        conf.setTenantId("");
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
        GuardedString pass = new GuardedString("".toCharArray());
        conf.setClientSecret(pass);
        msGraphConnector.init(conf);
        try {
            msGraphConnector.test();
        } finally {
            msGraphConnector.dispose();
        }
    }

}
