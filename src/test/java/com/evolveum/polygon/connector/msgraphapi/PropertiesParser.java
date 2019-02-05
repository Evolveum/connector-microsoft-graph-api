package com.evolveum.polygon;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


public class PropertiesParser {

    private static final Log LOGGER = Log.getLog(PropertiesParser.class);
    private Properties properties;
    private String FilePath = "../connector-msgraph/testProperties/propertiesforTest.properties";
    private final String CLIENT_SECRET = "clientSecret";
    private final String CLIENT_ID = "clientID";
    private final String TENANT_ID = "tenantID";


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


}