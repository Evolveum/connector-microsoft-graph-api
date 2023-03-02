package com.evolveum.polygon.connector.msgraphapi.integration;

import java.util.*;

import com.evolveum.polygon.connector.msgraphapi.MSGraphConfiguration;
import com.evolveum.polygon.connector.msgraphapi.MSGraphConnector;
import com.evolveum.polygon.connector.msgraphapi.common.ObjectConstants;
import com.evolveum.polygon.connector.msgraphapi.common.TestSearchResultsHandler;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class BasicConfigurationForTests implements ObjectConstants {

    private PropertiesParser parser = new PropertiesParser();
    protected String tenantId;
    protected Set<String> licenses, licenses2;

    protected static int _WAIT_INTERVAL = 2;
    protected static int _REPEAT_COUNT = 5;
    protected static long _REPEAT_INTERVAL = 6500;

    protected MSGraphConnector msGraphConnector;
    protected MSGraphConfiguration msGraphConfiguration;

    public BasicConfigurationForTests() {

        msGraphConnector = new MSGraphConnector();
        msGraphConfiguration = new MSGraphConfiguration();
    }

    protected MSGraphConfiguration getConfiguration() {
        MSGraphConfiguration msGraphConfiguration = new MSGraphConfiguration();
        msGraphConfiguration.setClientSecret(parser.getClientSecret());
        msGraphConfiguration.setClientId(parser.getClientId());
        msGraphConfiguration.setTenantId(parser.getTenantId());
        msGraphConfiguration.setDisabledPlans(parser.getDisabledPlans());
        this.tenantId = parser.getTenantId();
        licenses = parser.getLicenses();
        licenses2 = parser.getLicenses2();
        return msGraphConfiguration;
    }

    protected OperationOptions getDefaultAccountOperationOptions() {
        Map<String, Object> operationOptions = new HashMap<>();
        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);
        operationOptions.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[]{ATTR_ACCOUNTENABLED, ATTR_DISPLAYNAME,
                ATTR_ONPREMISESIMMUTABLEID, ATTR_MAILNICKNAME, ATTR_USERPRINCIPALNAME, ATTR_ABOUTME,
                ATTR_BIRTHDAY, ATTR_CITY, ATTR_COMPANYNAME, ATTR_COUNTRY, ATTR_DEPARTMENT,
                ATTR_GIVENNAME, ATTR_HIREDATE, ATTR_IMADDRESSES, ATTR_ID, ATTR_INTERESTS,
                ATTR_JOBTITLE, ATTR_MAIL, ATTR_MOBILEPHONE, ATTR_MYSITE, ATTR_OFFICELOCATION,
                ATTR_ONPREMISESLASTSYNCDATETIME, ATTR_ONPREMISESSECURITYIDENTIFIER,
                ATTR_ONPREMISESSYNCENABLED, ATTR_PASSWORDPOLICIES, ATTR_PASTPROJECTS,
                ATTR_POSTALCODE, ATTR_PREFERREDLANGUAGE, ATTR_PREFERREDNAME,
                ATTR_PROXYADDRESSES, ATTR_RESPONSIBILITIES, ATTR_SCHOOLS,
                ATTR_SKILLS, ATTR_STATE, ATTR_STREETADDRESS, ATTR_SURNAME,
                ATTR_USAGELOCATION, ATTR_USERTYPE, ATTR_ASSIGNEDLICENSES, ATTR_SIGN_IN, ATTR_SKUID
        });
        OperationOptions options = new OperationOptions(operationOptions);
        return options;
    }

    protected OperationOptions getDefaultGroupOperationOptions() {
        Map<String, Object> operationOptions = new HashMap<>();
        operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

        operationOptions.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[]{ATTR_ALLOWEXTERNALSENDERS, ATTR_AUTOSUBSCRIBENEWMEMBERS,
                ATTR_CLASSIFICATION, ATTR_CREATEDDATETIME, ATTR_DESCRIPTION, ATTR_DISPLAYNAME, ATTR_GROUPTYPES, ATTR_ID,
                ATTR_ISSUBSCRIBEDBYMAIL, ATTR_MAIL, ATTR_MAILENABLED, ATTR_MAILNICKNAME, ATTR_ONPREMISESLASTSYNCDATETIME,
                ATTR_ONPREMISESSECURITYIDENTIFIER, ATTR_ONPREMISESSYNCENABLED, ATTR_PROXYADDRESSES, ATTR_SECURITYENABLED,
                ATTR_UNSEENCOUNT, ATTR_VISIBILITY, ATTR_MEMBERS, ATTR_OWNERS
        });
        OperationOptions options = new OperationOptions(operationOptions);
        return options;
    }

    protected TestSearchResultsHandler getResultHandler() {

        return new TestSearchResultsHandler();
    }

    protected void deleteWaitAndRetry(ObjectClass objectClass, Uid uid, OperationOptions oOptions) throws Exception {
        int iterator = 0;
        boolean notFound = true;
        Exception exp = null;

        while (_REPEAT_COUNT > iterator && notFound) {
            notFound = false;
            try {
                msGraphConnector.delete(objectClass, uid, oOptions);

            } catch (UnknownUidException e) {

                exp = e;
                notFound = true;
                Thread.sleep(_REPEAT_INTERVAL);
            }

            if (!notFound) {

                return;
            }
            iterator++;
        }

        if (exp != null) {

            throw exp;
        }
    }

    protected void queryWaitAndRetry(ObjectClass objectClass, AttributeFilter filter, TestSearchResultsHandler handler,
                                     OperationOptions oOptions, Uid uid) throws InterruptedException {

        queryWaitAndRetry(objectClass, filter, handler, oOptions, Collections.singletonList(uid));
    }

    protected void queryWaitAndRetry(ObjectClass objectClass, AttributeFilter filter, TestSearchResultsHandler handler,
                                     OperationOptions oOptions, List<Uid> uid) throws InterruptedException {
        int iterator = 0;
        for (Uid i : uid) {
        }
        while (_REPEAT_COUNT > iterator) {

            try {
                msGraphConnector.executeQuery(objectClass, filter, handler, oOptions);
            } catch (UnknownUidException e) {

            }
            ArrayList resultsAccount = handler.getResult();

            if (resultsAccount.isEmpty()) {

                Thread.sleep(_REPEAT_INTERVAL);
            } else {

                if (areObjectsPresent(resultsAccount, uid)) {

                    break;
                }
            }
            iterator++;
        }
    }


    private boolean areObjectsPresent(ArrayList<ConnectorObject> resultsAccount, List<Uid> uidToCompare) {
        boolean match = false;

        for (Uid uid : uidToCompare) {

            match = false;
            for (ConnectorObject o : resultsAccount) {

                match = o.getUid().equals(uid);
                if (match) {

                    break;
                }
            }
            if (!match) {

                return match;
            }
        }

        return match;
    }

    @BeforeMethod
    private void init() {
        msGraphConfiguration = new MSGraphConfiguration();
        msGraphConnector = new MSGraphConnector();
    }

    @AfterMethod
    private void cleanup() {
        msGraphConnector.dispose();
        msGraphConfiguration.release();
    }

}
