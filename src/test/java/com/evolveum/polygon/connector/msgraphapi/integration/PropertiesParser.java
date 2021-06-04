package com.evolveum.polygon.connector.msgraphapi.integration;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


public class PropertiesParser {

    private static final Log LOGGER = Log.getLog(PropertiesParser.class);
    private static final Properties PROPERTIES = new Properties();

    private static final String PROPERTIES_PATH = "/testProperties/propertiesForTest.properties";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String CLIENT_ID = "clientID";
    private static final String TENANT_ID = "tenantID";
    private static final String LICENSES = "licenses";
    private static final String LICENSES2 = "licenses2";
    private static final String DISABLED_PLANS = "disabledPlans";

    public PropertiesParser() {

        try {
            PROPERTIES.load(getClass().getResourceAsStream(PROPERTIES_PATH));
        } catch (FileNotFoundException e) {
            LOGGER.error(e, "File not found: {0}", e.getLocalizedMessage());
        } catch (IOException e) {
            LOGGER.error(e, "IO exception occurred {0}", e.getLocalizedMessage());
        } catch (NullPointerException e) {
            LOGGER.error(e, "Properties file not found", e.getLocalizedMessage());
        }
    }


    public String getClientId() {
        return (String) PROPERTIES.get(CLIENT_ID);
    }

    public String getTenantId() {
        return (String) PROPERTIES.get(TENANT_ID);
    }

    public GuardedString getClientSecret() {
        return new GuardedString(((String) PROPERTIES.get(CLIENT_SECRET)).toCharArray());
    }

    private Set<String> getValues(String name) {
        Set<String> values = new HashSet<>();
        if (PROPERTIES.containsKey(name)) {
            String value = (String) PROPERTIES.get(name);
            values.addAll(Arrays.asList(value.split(",")));
        }
        return values;
    }

    public Set<String> getLicenses() {
        return getValues(LICENSES);
    }

    public Set<String> getLicenses2() {
        return getValues(LICENSES2);
    }

    public String[] getDisabledPlans() {
        if (PROPERTIES.containsKey(DISABLED_PLANS))
            return new String[]{(String) PROPERTIES.get(DISABLED_PLANS)};
        else
            return new String[0];
    }
}