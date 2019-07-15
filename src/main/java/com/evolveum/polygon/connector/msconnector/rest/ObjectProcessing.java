package com.evolveum.polygon.connector.msconnector.rest;


import com.evolveum.polygon.common.GuardedStringAccessor;
import com.microsoft.graph.models.extensions.DirectoryObject;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequest;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequestBuilder;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ObjectProcessing {

    public MSGraphConfiguration getConfiguration() {
        return configuration;
    }

    private MSGraphConfiguration configuration;

    public IGraphServiceClient getGraphServiceClient() {
        return graphServiceClient;
    }

    private static IGraphServiceClient graphServiceClient;
    private static String GROUPODATATYPE = "#microsoft.graph.group";
    private static String USERODATATYPE = "#microsoft.graph.user";
    protected static String GROUP = "group";
    protected static String USER = "user";

    public ObjectProcessing(MSGraphConfiguration configuration) {
        this.configuration = configuration;
        graphServiceClient = createMsGraphClient();

    }

    static final Log LOG = Log.getLog(MSGraphConnector.class);




    protected IGraphServiceClient createMsGraphClient() {
        GuardedString clientSecret = configuration.getClientSecret();
        GuardedStringAccessor accessorSecret = new GuardedStringAccessor();
        clientSecret.access(accessorSecret);

        LOG.info("createMsGraphClient: {0}", configuration.getTenantId());

        MsGraphAccessTokenProvider accessTokenProvider = MsGraphAccessTokenProvider //
                .tenantName(configuration.getTenantId()) //
                .clientId(configuration.getClientId()) //
                .clientSecret(accessorSecret.getClearString()) //
                .refreshBeforeExpiry(5, TimeUnit.MINUTES) //
                .build();


        List<String> scopes = new ArrayList<>();
        scopes.add(".default");

       // ClientCredentialProvider authProvider = new ClientCredentialProvider(configuration.getClientId(), scopes, accessorSecret.getClearString(), configuration.getTenantId(), null);
        MsGraphAuthenticationProvider authProvider = MsGraphAuthenticationProvider.from(accessTokenProvider);

        return GraphServiceClient.builder() //
                .authenticationProvider(authProvider) //
                .buildClient();
    }

    public void test() {
        createMsGraphClient().users().buildRequest().get();
    }

    protected String getEncodedAttributeValue(String attributeValue) {
        String encodedAttributeValue;
        try {
            encodedAttributeValue = URLEncoder.encode(attributeValue, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getCause());
        }
        LOG.info("encoded value {0}", encodedAttributeValue);
        return encodedAttributeValue;
    }

    protected List<String> getListDirectoryObject(IDirectoryObjectCollectionWithReferencesRequest request, String objectType) {

        List<String> listDirectoryObject = new ArrayList<>();
        IDirectoryObjectCollectionWithReferencesPage page;
        IDirectoryObjectCollectionWithReferencesRequestBuilder nextPageBuilder;
        List<DirectoryObject> directoryObjects;


        do {
            page = request.get();
            directoryObjects = page.getCurrentPage();
            for (DirectoryObject object : directoryObjects) {
                //add to list only group directory object (not roles etc.)
                if (objectType.equals(GROUP)) {
                    if (object.oDataType.equals(GROUPODATATYPE)) {
                        listDirectoryObject.add(object.id);
                    }
                    //add to list only user directory object (not roles etc.)
                } else if (objectType.equals(USER)) {
                    if (object.oDataType.equals(USERODATATYPE)) {
                        listDirectoryObject.add(object.id);
                    }
                }
            }

            nextPageBuilder = page.getNextPage();
            if (nextPageBuilder == null) {
                request = null;
            } else {
                request = nextPageBuilder.buildRequest();
            }
        } while (request != null);

        return listDirectoryObject;
    }

}


