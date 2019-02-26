package com.evolveum.polygon.connector.msgraphapi;

public class BasicConfigurationForTests {

    private PropertiesParser parser = new PropertiesParser();

    protected MSGraphConfiguration getConfiguration() {
        MSGraphConfiguration msGraphConfiguration = new MSGraphConfiguration();
        msGraphConfiguration.setClientSecret(parser.getClientSecret());
        msGraphConfiguration.setClientId(parser.getClientId());
        msGraphConfiguration.setTenantId(parser.getTenantId());
        return msGraphConfiguration;
    }

}
