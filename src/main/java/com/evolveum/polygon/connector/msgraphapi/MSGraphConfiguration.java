
package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.framework.spi.StatefulConfiguration;

import java.net.InetSocketAddress;


public class MSGraphConfiguration extends AbstractConfiguration
        implements StatefulConfiguration {

    private static final Log LOG = Log.getLog(MSGraphConfiguration.class);
    private String clientId;
    private GuardedString clientSecret = null;
    private String tenantId = null;
    private String proxyHost;
    private String proxyPort;

    @ConfigurationProperty(order = 10, displayMessageKey = "ClientId", helpMessageKey = "The Application ID that the 'Application Registration Portal' (apps.dev.microsoft.com) assigned to your app.", required = true, confidential = false)

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @ConfigurationProperty(order = 20, displayMessageKey = "ClientSecret", helpMessageKey = "The Application Secret that you generated for your app in the app registration portal.", required = true, confidential = true)

    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString clientSecret) {
        this.clientSecret = clientSecret;
    }

    @ConfigurationProperty(order = 30, displayMessageKey = "TenantId",
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

    @ConfigurationProperty(order = 40, displayMessageKey = "ProxyHost", helpMessageKey = "Hostname of the HTTPS proxy to use to connect to cloud services. If used, ProxyPort needs to be configured as well.")
    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) throws ConfigurationException {
        this.proxyHost = proxyHost;
    }

    @ConfigurationProperty(order = 50, displayMessageKey = "ProxyPort", helpMessageKey = "Port number of the HTTPS proxy to use to connect to cloud services. For this setting to take any effect, ProxyHost needs to be configured as well.")
    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
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

        if (!StringUtil.isBlank(proxyHost)) {
            if (proxyPort == null || "".equals(proxyPort)) throw new ConfigurationException("Proxy host is configured, but proxy port is not");
            final Integer proxyPortNo;
            try {
                proxyPortNo = Integer.parseInt(proxyPort);
            } catch (NumberFormatException nfe) {
                throw new ConfigurationException("Proxy port is not a valid number", nfe);
            }
            if (proxyPortNo <= 0) throw new ConfigurationException("Proxy port value must be positive");
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

    public boolean hasProxy() {
        return !(StringUtil.isBlank(proxyHost) || StringUtil.isBlank(proxyPort));
    }

    public InetSocketAddress getProxyAddress() {
        return new InetSocketAddress(this.proxyHost, Integer.parseInt(this.proxyPort));
    }
}