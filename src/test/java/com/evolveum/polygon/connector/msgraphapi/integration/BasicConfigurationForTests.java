package com.evolveum.polygon.connector.msgraphapi.integration;

import java.util.Set;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;

public class BasicConfigurationForTests {

    private PropertiesParser parser = new PropertiesParser();
    protected String tenantId;
    protected Set<String> licenses, licenses2;

    protected MSGraphConfiguration getConfiguration() {
        MSGraphConfiguration msGraphConfiguration = new MSGraphConfiguration();
        msGraphConfiguration.setClientSecret(parser.getClientSecret());
        msGraphConfiguration.setClientId(parser.getClientId());
        msGraphConfiguration.setTenantId(parser.getTenantId());
        msGraphConfiguration.setDisabledPlans(parser.getDisabledPlans());
        this.tenantId = parser.getTenantId();
        this.licenses = parser.getLicenses();
        this.licenses2 = parser.getLicenses2();
        return msGraphConfiguration;
    }

}
