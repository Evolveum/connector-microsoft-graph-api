
package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.connector.msgraphapi.util.GraphConfigurationHandler;
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
    private String pageSize = "100";

    // invites
    private boolean inviteGuests;
    private boolean sendInviteMail;
    private String inviteRedirectUrl;
    private String inviteMessage;

    //throttling
    private String throttlingRetryWait = "10";
    private Integer throttlingRetryCount = 3;

    //cert auth
    private boolean certificateBasedAuthentication;
    private String certificatePath;
    private String privateKeyPath;
    private boolean validateWithFailoverTrust = true;
    private GraphConfigurationHandler configHandler = new GraphConfigurationHandler();

    @ConfigurationProperty(order = 10, displayMessageKey = "ClientId.display", helpMessageKey = "ClientId.help", required = true)

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }


    @ConfigurationProperty(order = 20, displayMessageKey = "ClientSecret.display", helpMessageKey = "ClientSecret.help",
            required = true, confidential = true)

    public GuardedString getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(GuardedString clientSecret) {
        this.clientSecret = clientSecret;
    }


    @ConfigurationProperty(order = 30, displayMessageKey = "TenantId.display",
            helpMessageKey = "TenantId.help",
            required = true)

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ConfigurationProperty(order = 35, displayMessageKey = "ValidateWithFailoverTrustStore.display", helpMessageKey = "ValidateWithFailoverTrustStore.help")

    public boolean isValidateWithFailoverTrust() { return validateWithFailoverTrust; }

    public void setValidateWithFailoverTrust(boolean validateWithFailoverTrust) { this.validateWithFailoverTrust =
            validateWithFailoverTrust; }

    @ConfigurationProperty(order = 36, displayMessageKey = "PathToFailoverTrustStore.display", helpMessageKey = "PathToFailoverTrustStore.help")

    public String getPathToFailoverTrustStore() { return configHandler.getPathToFailoverTrustStore(); }

    public void setPathToFailoverTrustStore(String pathToFailoverTrustStore) { configHandler.setPathToFailoverTrustStore(pathToFailoverTrustStore); }

    @ConfigurationProperty(order = 40, displayMessageKey = "ProxyHost.display", helpMessageKey = "ProxyHost.help")

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) throws ConfigurationException {
        this.proxyHost = proxyHost;
    }


    @ConfigurationProperty(order = 100, displayMessageKey = "PageSize.display", helpMessageKey = "PageSize.help")

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    @ConfigurationProperty(order = 50, displayMessageKey = "ProxyPort.display", helpMessageKey = "ProxyPort.help")

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    @ConfigurationProperty(order = 55, displayMessageKey = "DisabledPlans.display", helpMessageKey = "DisabledPlans.help")

    public String[] getDisabledPlans() {
        return configHandler.getDisabledPlans();
    }

    public void setDisabledPlans(String[] disabledPlans) {
        configHandler.setDisabledPlans(disabledPlans);
    }

    @ConfigurationProperty(order = 60, displayMessageKey = "InviteGuests.display", helpMessageKey = "InviteGuests.help")

    public boolean isInviteGuests() {
        return inviteGuests;
    }

    public void setInviteGuests(boolean inviteGuests) {
        this.inviteGuests = inviteGuests;
    }


    @ConfigurationProperty(order = 70, displayMessageKey = "SendInviteMail.display", helpMessageKey = "SendInviteMail.help")

    public boolean isSendInviteMail() {
        return sendInviteMail;
    }

    public void setSendInviteMail(boolean sendInviteMail) {
        this.sendInviteMail = sendInviteMail;
    }


    @ConfigurationProperty(order = 80, displayMessageKey = "InviteRedirectURL.display", helpMessageKey = "InviteRedirectURL.help")

    public String getInviteRedirectUrl() {
        return inviteRedirectUrl;
    }

    public void setInviteRedirectUrl(String inviteRedirectUrl) {
        this.inviteRedirectUrl = inviteRedirectUrl;
    }


    @ConfigurationProperty(order = 90, displayMessageKey = "InviteMessage.display", helpMessageKey = "InviteMessage.help")

    public String getInviteMessage() {
        return inviteMessage;
    }

    public void setInviteMessage(String inviteMessage) {
        this.inviteMessage = inviteMessage;
    }

    @ConfigurationProperty(order = 100, displayMessageKey = "ThrottlingMaxRetryCount.display", helpMessageKey = "ThrottlingMaxRetryCount.help")

    public Integer getThrottlingRetryCount() {
        return throttlingRetryCount;
    }

    public void setThrottlingRetryCount(Integer throttlingRetryCount) {
        this.throttlingRetryCount = throttlingRetryCount;
    }

    @ConfigurationProperty(order = 110, displayMessageKey = "ThrottlingMaxWait.display", helpMessageKey = "ThrottlingMaxWait.help")

    public String getThrottlingRetryWait() {

        return throttlingRetryWait;
    }

    public void setThrottlingRetryWait(String throttlingRetryWait) {
        this.throttlingRetryWait = throttlingRetryWait;
    }

    @ConfigurationProperty(order = 120, displayMessageKey = "CertificateBasedAuthentication.display", helpMessageKey = "CertificateBasedAuthentication.help")

    public boolean isCertificateBasedAuthentication() { return certificateBasedAuthentication; }

    public void setCertificateBasedAuthentication(boolean certificateBasedAuthentication) { this.certificateBasedAuthentication = certificateBasedAuthentication; }

    @ConfigurationProperty(order = 130, displayMessageKey = "CertificatePath.display", helpMessageKey = "CertificatePath.help")

    public String getCertificatePath() { return certificatePath; }

    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }

    @ConfigurationProperty(order = 140, displayMessageKey = "PrivateKeyPath.display", helpMessageKey = "PrivateKeyPath.help")

    public String getPrivateKeyPath() { return privateKeyPath; }

    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }


    @Override
    public void validate() {
        LOG.info("Processing trough configuration validation procedure.");
        if (StringUtil.isBlank(clientId)) {
            throw new ConfigurationException("Client Id cannot be empty.");
        }

        if ("".equals(clientSecret)) {
            throw new ConfigurationException("Client Secret cannot be empty.");
        }

        if (StringUtil.isBlank(tenantId)) {
            throw new ConfigurationException("Tenant Id cannot be empty.");
        }

        if (!StringUtil.isBlank(proxyHost)) {
            if (StringUtil.isBlank(proxyPort))
                throw new ConfigurationException("Proxy host is configured, but proxy port is not");
            final Integer proxyPortNo;
            try {
                proxyPortNo = Integer.parseInt(proxyPort);
            } catch (NumberFormatException nfe) {
                throw new ConfigurationException("Proxy port is not a valid number", nfe);
            }
            if (proxyPortNo <= 0) throw new ConfigurationException("Proxy port value must be positive");
        }

        configHandler.validateDisabledPlans();

        if (inviteGuests) {
            if (StringUtil.isBlank(inviteRedirectUrl))
                throw new ConfigurationException("InviteRedirectUrl is mandatory when InviteGuests is enabled");
        } else {
            if (StringUtil.isNotBlank(inviteRedirectUrl))
                throw new ConfigurationException("InviteRedirectUrl is configured, but InviteGuests is disabled");
            if (StringUtil.isNotBlank(inviteMessage))
                throw new ConfigurationException("InviteMessage is configured, but InviteGuests is disabled");
        }

        try {
            Float f = Float.parseFloat(throttlingRetryWait);

            if (f < 0f) {

                throw new ConfigurationException("The specified number for the maximum throttling request retry wait " +
                        "time has to be a non negative number!");
            }
        } catch (NumberFormatException e) {

            throw new ConfigurationException("The specified number for the max throttling wait time is not a valid float!");
        }

        if (throttlingRetryCount < 0) {

            throw new ConfigurationException("The specified number for the maximum throttling request retries has to be " +
                    "a non negative number!");
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
