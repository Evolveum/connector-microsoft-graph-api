package com.evolveum.polygon.connector.msgraphapi.integration;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


public class PropertiesParser {

    private static final Log LOGGER = Log.getLog(PropertiesParser.class);
    private Properties properties;
    private String FilePath = "./testProperties/propertiesforTest.properties";
    private final String CLIENT_SECRET = "clientSecret";
    private final String CLIENT_ID = "clientID";
    private final String TENANT_ID = "tenantID";
    private final String LICENSES = "licenses";
    private final String LICENSES2 = "licenses2";
    private final String DISABLED_PLANS = "disabledPlans";


    public PropertiesParser() {

        try {
            InputStreamReader fileInputStream = new InputStreamReader(new FileInputStream(FilePath),
                    StandardCharsets.UTF_8);
            properties = new Properties();
            properties.load(fileInputStream);
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: {0}", e.getLocalizedMessage());
            e.printStackTrace();
        } catch (IOException e) {
            LOGGER.error("IO exception occurred {0}", e.getLocalizedMessage());
            e.printStackTrace();
        }
    }


    public String getClientId() {
        return (String) properties.get(CLIENT_ID);
    }

    public String getTenantId() {
        return (String) properties.get(TENANT_ID);
    }

    public GuardedString getClientSecret() {
        return new GuardedString(((String) properties.get(CLIENT_SECRET)).toCharArray());
    }

    private Set<String> getValues(String name) {
        Set<String> values = new HashSet<>();
        if (properties.containsKey(name)) {
            String value = (String)properties.get(name);
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
        if (properties.containsKey(DISABLED_PLANS))
            return new String[] {(String)properties.get(DISABLED_PLANS)};
        else
            return new String[0];
    }
}