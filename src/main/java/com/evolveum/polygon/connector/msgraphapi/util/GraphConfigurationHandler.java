package com.evolveum.polygon.connector.msgraphapi.util;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;

public class GraphConfigurationHandler {

    private String pathToTrustStore;
    private String[] disabledPlans = {};
    private static final Log LOG = Log.getLog(GraphConfigurationHandler.class);

    public GraphConfigurationHandler() {

        pathToTrustStore = generateNativeJVMTrustPath();
    }

    private String generateNativeJVMTrustPath() {


        String separator = System.getProperty("file.separator");
        String javaHome = System.getProperty("java.home");

        LOG.ok("The java home system property is set to: {0}, using as file separator: {1}", javaHome, separator);

        StringBuilder pathToJavaTrust = new StringBuilder(javaHome);

        pathToJavaTrust.append(separator);
        pathToJavaTrust.append("lib");
        pathToJavaTrust.append(separator);
        pathToJavaTrust.append("security");
        pathToJavaTrust.append(separator);
        pathToJavaTrust.append("cacerts");

        LOG.ok("Path to original Java Trust Store: {0}", pathToJavaTrust);

        return pathToJavaTrust.toString();
    }

    public void validateDisabledPlans() {

        if (disabledPlans != null) {

            if (disabledPlans.length == 0) {
                LOG.ok("Disabled plans property is empty");
                return;
            }
            for (String licensePlan : disabledPlans) {

                if (licensePlan.contains("[") && licensePlan.contains("]")) {

                    String[] divPlan = StringUtils.substringsBetween(licensePlan, "[", "]");

                    String potentialPlanId = divPlan[divPlan.length - 1];

                    if (potentialPlanId.contains(":")) {
                        LOG.ok("Following plan validation passed: {0}, will be formatted to {1}", licensePlan
                                , potentialPlanId);

                    } else {


                        throw new ConfigurationException("Potentially malformed plan ID detected on input: " + potentialPlanId);
                    }

                } else {

                    if (licensePlan.contains(":")) {

                        LOG.ok("Following plan validation passed: {0}, no further formatting will be done by the connector."
                                , licensePlan);

                    } else {

                        throw new ConfigurationException("Potentially malformed plan ID detected on input: " + licensePlan);
                    }

                }
            }
        } else {
            throw new ConfigurationException("Disabled plans array can't be null");

        }
    }

    public String getPathToFailoverTrustStore() {
        return pathToTrustStore;
    }

    public void setPathToFailoverTrustStore(String pathToTrustStore) {
        this.pathToTrustStore = pathToTrustStore;
    }

    public String[] getDisabledPlans() {
        return disabledPlans;
    }

    public void setDisabledPlans(String[] disabledPlans) {
        this.disabledPlans = disabledPlans;
    }
}
