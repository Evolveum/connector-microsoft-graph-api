
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
    private String[] disabledPlans = {};
    private String pageSize = "100";

    // invites
    private boolean inviteGuests;
    private boolean sendInviteMail;
    private String inviteRedirectUrl;
    private String inviteMessage;

    //throttling
    private String throttlingRetryWait = "10";
    private Integer throttlingRetryCount = 3;

    @ConfigurationProperty(order = 10, displayMessageKey = "ClientId", helpMessageKey = "The Application ID that the 'Application Registration Portal' (apps.dev.microsoft.com) assigned to your app.", required = true)

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
            required = true)

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


    @ConfigurationProperty(order = 100, displayMessageKey = "PageSize", helpMessageKey = "The number of entries to bring back per page in the call to the Graph API")

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    @ConfigurationProperty(order = 50, displayMessageKey = "ProxyPort", helpMessageKey = "Port number of the HTTPS proxy to use to connect to cloud services. For this setting to take any effect, ProxyHost needs to be configured as well.")

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    @ConfigurationProperty(order = 55, displayMessageKey = "DisabledPlans", helpMessageKey = "List of the SkuId:ServicePlanId,[ServicePlanId2...]. These service plan will be disabled during assignment of the each license. Friendly names are not supported. Default: (empty)")

    public String[] getDisabledPlans() {
        return disabledPlans;
    }

    public void setDisabledPlans(String[] disabledPlans) {
        this.disabledPlans = disabledPlans;
    }

    @ConfigurationProperty(order = 60, displayMessageKey = "InviteGuests", helpMessageKey = "Whether to allow creation of guest accounts by inviting users from outside the tenant (based on e-mail address only)")

    public boolean isInviteGuests() {
        return inviteGuests;
    }

    public void setInviteGuests(boolean inviteGuests) {
        this.inviteGuests = inviteGuests;
    }


    @ConfigurationProperty(order = 70, displayMessageKey = "SendInviteMail", helpMessageKey = "Whether to send an email invitation to guest users.")

    public boolean isSendInviteMail() {
        return sendInviteMail;
    }

    public void setSendInviteMail(boolean sendInviteMail) {
        this.sendInviteMail = sendInviteMail;
    }


    @ConfigurationProperty(order = 80, displayMessageKey = "InviteRedirectURL", helpMessageKey = "Specify a URL that an invited user should be redirected to once he claims his invitation. Mandatory if 'InviteGuests' is true")

    public String getInviteRedirectUrl() {
        return inviteRedirectUrl;
    }

    public void setInviteRedirectUrl(String inviteRedirectUrl) {
        this.inviteRedirectUrl = inviteRedirectUrl;
    }


    @ConfigurationProperty(order = 90, displayMessageKey = "InviteMessage", helpMessageKey = "Custom message to send in an invite. Requires 'InviteRedirectURL'")

    public String getInviteMessage() {
        return inviteMessage;
    }

    public void setInviteMessage(String inviteMessage) {
        this.inviteMessage = inviteMessage;
    }

    @ConfigurationProperty(order = 100, displayMessageKey = "ThrottlingMaxReplyCount", helpMessageKey = "Max retry count in case of an request impacted by throttling. Default 3.")

    public Integer getThrottlingRetryCount() {
        return throttlingRetryCount;
    }

    public void setThrottlingRetryCount(Integer throttlingRetryCount) {
        this.throttlingRetryCount = throttlingRetryCount;
    }

    @ConfigurationProperty(order = 110, displayMessageKey = "ThrottlingMaxWait", helpMessageKey = "Max time period in between requests impacted by throttling. Define as number of seconds. Default 10")

    public String getThrottlingRetryWait() {

        return throttlingRetryWait;
    }

    public void setThrottlingRetryWait(String throttlingRetryWait) {
        this.throttlingRetryWait = throttlingRetryWait;
    }

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

        if (disabledPlans == null)
            throw new ConfigurationException("Disabled plans array can't be null");

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

                throw new ConfigurationException("The specified number for the maximum throttling request retry wait time has to be a non negative number!");
            }
        } catch (NumberFormatException e) {

            throw new ConfigurationException("The specified number for the max throttling wait time is not a valid float!");
        }

        if (throttlingRetryCount < 0) {

            throw new ConfigurationException("The specified number for the maximum throttling request retries has to be a non negative number!");
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
