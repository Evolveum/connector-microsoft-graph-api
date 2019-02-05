
package com.evolveum.polygon;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;


public class MSGraphConfiguration extends AbstractConfiguration
        implements StatefulConfiguration {

    private static final Log LOG = Log.getLog(MSGraphConfiguration.class);
    private String clientId;
    private GuardedString clientSecret = null;
    private String tenantId = null;


    @ConfigurationProperty(order = 1, displayMessageKey = "ClientId", helpMessageKey = "The Application ID that the 'Application Registration Portal' (apps.dev.microsoft.com) assigned to your app.", required = true, confidential = false)

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "ClientSecret", helpMessageKey = "The Application Secret that you generated for your app in the app registration portal.", required = true, confidential = true)

    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString clientSecret) {
        this.clientSecret = clientSecret;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "TenantId",
            helpMessageKey = "Allows only users with work/school accounts from a particular Azure Active Directory tenant" +
                    " to sign into the application. Either the friendly domain name of the Azure AD tenant or the " +
                    "tenant's guid identifier can be used. Example: '8eaef023-2b34-4da1-9baa-8bc8c9d6a490' or 'contoso.onmicrosoft.com'",
            required = true, confidential = false)

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }


    @Override
    public void validate() {
        LOG.info("Processing trough configuration validation procedure.");
        if (StringUtil.isBlank(clientId)) {
            throw new ConfigurationException("Client Id  cannot be empty.");
        }
        if ("".equals(clientSecret)) {
            throw new ConfigurationException("Client Secret cannot be empty.");
        }
        if (StringUtil.isBlank(tenantId)) {
            throw new ConfigurationException("Tenant Id cannot be empty.");
        }
        LOG.info("Configuration valid");


    }

    @Override
    public void release() {
        LOG.info("The release of configuration resources is being performed");
        this.clientId = null;
        this.clientSecret.dispose();
        this.tenantId = null;

    }
}