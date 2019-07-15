package com.evolveum.polygon.connector.msconnector.rest;

public class BasicConfigurationForTests {

    private PropertiesParser parser = new PropertiesParser();

    protected MSGraphConfiguration getConfiguration() {
        MSGraphConfiguration msGraphConfiguration = new MSGraphConfiguration();
        msGraphConfiguration.setClientId(parser.getClientId());
        msGraphConfiguration.setTenantId(parser.getTenantId());
        msGraphConfiguration.setClientSecret(parser.getClientSecret());


        return msGraphConfiguration;
    }

}
