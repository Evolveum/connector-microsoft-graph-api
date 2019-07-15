package com.evolveum.polygon.connector.msconnector.rest;

import com.evolveum.polygon.common.GuardedStringAccessor;
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


    @ConfigurationProperty(order = 1, displayMessageKey = "Client (Application) Id ", helpMessageKey = "", required = true, confidential = false)

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @ConfigurationProperty(order = 2, displayMessageKey = "Client (Application) Secret", helpMessageKey = "", required = true, confidential = true)

    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString clientSecret) {
        this.clientSecret = clientSecret;
    }

    @ConfigurationProperty(order = 3, displayMessageKey = "Tenant Id",
            helpMessageKey = "Either the friendly domain name of the Azure AD tenant or the " +
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

        GuardedStringAccessor accessor = new GuardedStringAccessor();
        clientSecret.access(accessor);
        if ("".equals(accessor.getClearString())) {
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