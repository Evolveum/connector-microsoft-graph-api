package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.common.GuardedStringAccessor;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

import static com.evolveum.polygon.connector.msgraphapi.ObjectProcessing.LOG;
import static com.evolveum.polygon.connector.msgraphapi.ObjectProcessing.TOP;

/**
 * Facade for Microsoft Graph API endpoint.
 * Facilitates HTTP access to Microsoft services.
 */
public class GraphEndpoint {
    private final static String API_ENDPOINT = "graph.microsoft.com/v1.0";
    private final static String AUTHORITY = "https://login.microsoftonline.com/";
    private final static String RESOURCE = "https://graph.microsoft.com";
    //private static final int MAX_THROTTLING_RETRY_COUNT = 3;

    private final MSGraphConfiguration configuration;
    private final URIBuilder uriBuilder;
    private AuthenticationResult authenticateResult;
    private SchemaTranslator schemaTranslator;
    private CloseableHttpClient httpClient;
    private Boolean throttling = false;
    //private final long MAX_THROTTLING_REPLY_TIME = TimeUnit.SECONDS.toMillis(10);

    private final long SKEW = TimeUnit.MINUTES.toMillis(5);

    GraphEndpoint(MSGraphConfiguration configuration) {
        this.configuration = configuration;
        this.uriBuilder = createURIBuilder();

        authenticate();
        initSchema();
        initHttpClient();
    }

    public MSGraphConfiguration getConfiguration() {
        return configuration;
    }

    public SchemaTranslator getSchemaTranslator() {
        return schemaTranslator;
    }

    private void authenticate() {
        AuthenticationResult result = null;
        ExecutorService service = null;
        GuardedString clientSecret = configuration.getClientSecret();
        GuardedStringAccessor accessorSecret = new GuardedStringAccessor();
        clientSecret.access(accessorSecret);
        LOG.info("authenticate");

        try {
            service = Executors.newFixedThreadPool(1);
            ClientCredential credential = new ClientCredential(configuration.getClientId(), accessorSecret.getClearString());
            AuthenticationContext context = new AuthenticationContext(AUTHORITY + configuration.getTenantId()
                    + "/oauth2/authorize", false, service);
            if (configuration.hasProxy()) {
                LOG.info("Authenticating through proxy[{0}]", configuration.getProxyAddress().toString());
                context.setProxy(createProxy());
            }
            Future<AuthenticationResult> future = context.acquireToken(
                    RESOURCE,
                    credential,
                    null);
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ConnectionFailedException(e);
        } catch (MalformedURLException e) {
            LOG.error(e.toString());
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ConnectionFailedException("Failed to authenticate GitHub API");
        }

        this.authenticateResult = result;
    }

    private Proxy createProxy() {
        return new Proxy(Proxy.Type.HTTP, configuration.getProxyAddress());
    }

    private void initSchema() {
        schemaTranslator = new SchemaTranslator(this);
    }

    private void initHttpClient() {
        final HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setRetryHandler(myRetryHandler);
        clientBuilder.setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {
            @Override
            public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
                return executionCount <= 7 && response.getStatusLine().getStatusCode() >= 500 && response.getStatusLine().getStatusCode() < 600;
            }

            @Override
            public long getRetryInterval() {
                return 3000;
            }
        });
        if (configuration.hasProxy()) {
            LOG.info("Executing request through proxy[{0}]", configuration.getProxyAddress().toString());
            clientBuilder.setProxy(
                    new HttpHost(configuration.getProxyAddress().getAddress(), configuration.getProxyAddress().getPort())
            );
        }
        httpClient = clientBuilder.build();
    }

    private AuthenticationResult getAccessToken() {
        if (authenticateResult.getExpiresOnDate().getTime() - SKEW < new Date().getTime()) {
            // Expired, re-authenticate
            authenticate();
        }
        return authenticateResult;
    }

    public URIBuilder createURIBuilder() {
        return new URIBuilder().setScheme("https").setHost(API_ENDPOINT);
    }

    public URI getUri(URIBuilder uriBuilder) {
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new ConnectorException("It is not possible to create URI" + e.getLocalizedMessage(), e);
        }
        return uri;
    }

    HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {

        public boolean retryRequest(
                IOException exception,
                int executionCount,
                HttpContext context) {
            if (executionCount >= 10) {
                // Do not retry if over max retry count
                return false;
            }
        /*if (exception instanceof InterruptedIOException) {
            // Timeout
            return false;
        }
        if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
        }
        if (exception instanceof ConnectTimeoutException) {
            // Connection refused
            return false;
        }
        if (exception instanceof SSLException) {
            // SSL handshake exception
            return false;
        } */

            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }

            if (exception instanceof org.apache.http.NoHttpResponseException) {
                LOG.warn("No response from server on " + executionCount + " call");
                return true;
            }

            return false;
        }

    };

    private CloseableHttpResponse executeRequest(HttpUriRequest request) {
        if (request == null) {
            throw new InvalidAttributeValueException("Request not provided");
        }
        request.setHeader("Authorization", getAccessToken().getAccessToken());
        request.setHeader("Content-Type", "application/json");
        request.setHeader("ConsistencyLevel", "eventual");
        LOG.info("HtttpUriRequest: {0}", request);
        LOG.info(request.toString());
        CloseableHttpResponse response;
        int retryCount = 0;
        try {
            response = httpClient.execute(request);
            LOG.info("response {0}", response);
            throttling = false;
            processResponseErrors(response);
            while (throttling) {
                throttling = false;
                LOG.ok("Current retry count: {0}", retryCount);
                if (retryCount >= configuration.getThrottlingRetryCount()) {

                    throw new ConnectorException("Max retry count for request throttling exceeded! Request was not successful");
                }
                retryCount++;
                Header[] callHeaders = response.getAllHeaders();
                if (callHeaders != null) {
                    for (Header header : callHeaders) {
                        if (header.getName().equals("Retry-After")) {
                            String tmpRaHeadValue = header.getValue();
                            if (tmpRaHeadValue != null || !tmpRaHeadValue.isEmpty()) {
                                long retryAfter = (long) (Float.parseFloat(tmpRaHeadValue) * 1000);
                                long maxWail = (long) (Float.parseFloat(configuration.getThrottlingRetryWait()) * 1000);
                                LOG.ok("Max retry time in ms: {0}", maxWail);
                                LOG.ok("Returned retry time in ms: {0}", retryAfter);
                                if (retryAfter > maxWail) {

                                    throw new ConnectorException("Max time for request throttling exceeded! Request was not successful");
                                }
                                Thread.sleep(retryAfter);
                                LOG.ok("Throttling retry");

                                response = httpClient.execute(request);
                                processResponseErrors(response);
                            }
                        }
                    }
                }
            }

            return response;

        } catch (IOException | InterruptedException e) {
            StringBuilder sb = new StringBuilder();
            LOG.ok("The exception type: {0}", e.getClass());
            sb.append("It is not possible to execute request:").append(request.toString()).append(";")
                    .append(e.getLocalizedMessage());
            if (e instanceof IOException) {
                throw new ConnectorIOException(sb.toString(), e);
            } else {

                throw new ConnectorException(sb.toString(), e);
            }
        }
    }


    public void processResponseErrors(CloseableHttpResponse response) {
        if (response == null) {
            throw new InvalidAttributeValueException("Response not provided ");
        }
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode <= 299) {
            return;
        }
        /*if (statusCode == 404) {
            //throw new UnknownUidException(message);
            LOG.info("Status code 404 caught in processResponseErrors {0}", response.getStatusLine().getReasonPhrase());
            return;
        }*/
        String responseBody = null;
        try {
            responseBody = EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            LOG.warn("cannot read response body: " + e, e);
        }
        String message = "HTTP error " + statusCode + " " + response.getStatusLine().getReasonPhrase() + " : "
                + responseBody;
        //LOG.error("{0}", message);
        if (statusCode == 400 && message.contains("The client credentials are invalid")) {
            throw new InvalidCredentialException(message);
        }
        if (statusCode == 400 && message.contains("Another object with the same value for property userPrincipalName already exists.")) {
            throw new AlreadyExistsException(message);
        }
        if (statusCode == 400 && message.contains("The specified password does not comply with password complexity requirements.")) {
            throw new InvalidPasswordException();
        }
        if (statusCode == 400 && message.contains("Invalid object identifier")) {
            throw new UnknownUidException();
        }
        if (statusCode == 400 || statusCode == 405 || statusCode == 406) {
            throw new InvalidAttributeValueException(message);
        }
        if (statusCode == 401 && message.contains("Access token has expired")) {
            throw new ConnectionFailedException(message);
        }
        if (statusCode == 401 || statusCode == 402 || statusCode == 403 || statusCode == 407) {
            throw new PermissionDeniedException(message);
        }
        if (statusCode == 404 || statusCode == 410) {
            LOG.info("Status code 404 or 410 caught in processResponseErrors {0}", message);
            throw new UnknownUidException(message);
        }
        if (statusCode == 408) {
            throw new OperationTimeoutException(message);
        }
        if (statusCode == 409) {
            throw new AlreadyExistsException(message);
        }
        if (statusCode == 412) {
            throw new PreconditionFailedException(message);
        }
        if (statusCode == 418) {
            throw new UnsupportedOperationException("Sorry, no cofee: " + message);
        }
        if (statusCode == 429) {
            LOG.warn("Request returned with status code 429 which means an api call limit was reached.");
            throttling = true;
            return;
        }

        throw new ConnectorException(message);
    }


    protected JSONObject callRequest(HttpRequestBase request, boolean parseResult) {
        if (request == null) {
            throw new InvalidAttributeValueException("Request not provided or empty");
        }
        LOG.info("callRequest");
        String result = null;
        request.setHeader("ConsistencyLevel", "eventual");
        LOG.info("URL in request: {0}", request.getRequestLine().getUri());
        LOG.info("Enumerating headers");
        List<Header> httpHeaders = Arrays.asList(request.getAllHeaders());
        for (Header header : httpHeaders) {
            LOG.info("Headers.. name,value:" + header.getName() + "," + header.getValue());
        }
        try (CloseableHttpResponse response = executeRequest(request)) {
            processResponseErrors(response);
            if (response.getStatusLine().getStatusCode() == 204) {
                LOG.ok("204 - No Content ");
                return null;
            } else if (response.getStatusLine().getStatusCode() == 200 && !parseResult) {
                LOG.ok("200 - OK");
                return null;
            }
            result = EntityUtils.toString(response.getEntity());
            if (!parseResult) {
                return null;
            }
            return new JSONObject(result);
        } catch (IOException e) {
            throw new ConnectorIOException();
        }

    }


    public JSONObject callRequest(HttpEntityEnclosingRequestBase request, JSONObject json, Boolean parseResult) {
        LOG.info("request URI: {0}", request.getURI());
        LOG.info("json {0}", json);

        HttpEntity entity;
        byte[] jsonByte;
        try {
            jsonByte = json.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed creating byte[] from JSONObject: ").append(json).append(", which was encoded by UTF-8;")
                    .append(e.getLocalizedMessage());
            throw new ConnectorIOException(sb.toString(), e);
        }
        entity = new ByteArrayEntity(jsonByte);

        request.setEntity(entity);
        LOG.info("request {0}", request);
        // execute request
        CloseableHttpResponse response = executeRequest(request);
        LOG.info("response: {0}", response);

        processResponseErrors(response);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 201) {
            LOG.ok("201 - Created");
        } else if (statusCode == 204) {
            LOG.ok("204 - no content");
        } else {
            LOG.info("statuscode - {0}", statusCode);
        }


        if (!parseResult) {
            return null;
        }

        // result as output
        HttpEntity responseEntity = response.getEntity();
        try {
            // String result = EntityUtils.toString(responseEntity);
            byte[] byteResult = EntityUtils.toByteArray(responseEntity);
            String result = new String(byteResult, "ISO-8859-2");
            responseClose(response);
            LOG.info("result: {0}", result);
            return new JSONObject(result);
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed creating result from HttpEntity: ").append(responseEntity).append(";")
                    .append(e.getLocalizedMessage());
            responseClose(response);
            throw new ConnectorIOException(sb.toString(), e);
        }
    }


    private void responseClose(CloseableHttpResponse response) {
        try {
            response.close();
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Failed close response: ").append(response);
            LOG.warn(e, sb.toString());
        }
    }

    //paging == false if only 1 result will be returned
    protected JSONObject executeGetRequest(String path, String customQuery, OperationOptions options, Boolean paging) {
        LOG.info("executeGetRequest path {0}, customQuery {1}, options: {2}, paging: {3}", path, customQuery, options, paging);
        final URIBuilder uribuilder = createURIBuilder().clearParameters();

        if (customQuery != null && options != null && paging) {
            /*Integer perPage = options.getPageSize();
            if (perPage == null) {
               LOG.info("perPage found null, but paging set to: {0}", paging);
               perPage = 100;
            }*/
            String perPage = configuration.getPageSize();
            if (perPage != null) {
                uribuilder.setCustomQuery(customQuery + "&" + TOP + "=" + perPage.toString());
                LOG.info("setCustomQuery {0} ", uribuilder.toString());
            } else {
                uribuilder.setCustomQuery(customQuery);
                LOG.info("setCustomQuery {0} ", uribuilder.toString());
            }

        } else if (customQuery == null && options != null && paging) {
            Integer perPage = options.getPageSize();
            if (perPage != null) {
                uribuilder.addParameter(TOP, perPage.toString());
                LOG.info("uribulder.addparamater perPage {0}: ", uribuilder.toString());
            }
        } else if (customQuery != null && options != null && !paging) {
            uribuilder.setCustomQuery(customQuery);
            LOG.info("setCustomQuery {0} ", uribuilder.toString());
        }

        uribuilder.setPath(path);

        try {
            URI uri = uribuilder.build();
            LOG.info("uri {0}", uri);
            HttpRequestBase request = new HttpGet(uri);
            //request.setRetryHandler(new StandardHttpRequestRetryHandler(5, true));
            JSONObject firstCall = callRequest(request, true);

            //call skipToken for paging
            if (options != null && paging) {
                /*Integer page = options.getPagedResultsOffset();
                LOG.info("Paging enabled, and this page is: {0} ", page);
                if (page != null) {
                    if (page == 0 || page == 1) {
                        LOG.info("Inside returning firstCall");
                        return firstCall; // no need for skipToken actually returned the first page
                    } else {
                        String nextLink;
                        try {
                            LOG.info("About to try getting the odata.nextLink");
                            nextLink = firstCall.getString("@odata.nextLink");
                        } catch (JSONException e) {
                            LOG.info("all data returned from the first call, no more data remain to return");
                            return new JSONObject();
                        }

                        JSONObject nextLinkJson = null;
                        for (int count = 1; count < page; count++) {
                            LOG.info(" count: {0} ; nextLink: {1} ; nextLinkJson: {2} ", count, nextLink, nextLinkJson);
                            HttpRequestBase nextLinkUriRequest = new HttpGet(nextLink);
                            LOG.info("nextLinkUriRequest {0}", nextLinkUriRequest);
                            nextLinkJson = callRequest(nextLinkUriRequest, true);
                            try {
                                nextLink = nextLinkJson.getString("@odata.nextLink");
                            } catch (JSONException e) {
                                LOG.info("all data returned from the {0} call, no more data remain to return", count);
                                return new JSONObject();
                            }
                        }
                        return nextLinkJson;

                    }
                }
                return firstCall;*/
                JSONArray value = new JSONArray();
                String nextLink = new String();
                JSONObject values = new JSONObject();
                boolean morePages = false;
                if (firstCall.has("@odata.nextLink") && firstCall.getString("@odata.nextLink") != null && !firstCall.getString("@odata.nextLink").isEmpty()) {
                    morePages = true;
                    nextLink = firstCall.getString("@odata.nextLink");
                    LOG.info("nextLink: {0} ; firstCall: {1} ", nextLink, firstCall);
                } else {
                    morePages = false;
                    LOG.info("No nextLink defined, final page was firstCall");
                }
                if (firstCall.has("value") && firstCall.get("value") != null) {
                    //value.addAll(firstCall.getJSONArray("value"));
                    for (int i = 0; i < firstCall.getJSONArray("value").length(); i++) {
                        value.put(firstCall.getJSONArray("value").get(i));
                    }
                    LOG.info("firstCall: {0} ", firstCall);
                } else {
                    LOG.info("firstCall contained no value object or the object was null");
                }
                while (morePages == true) {
                    JSONObject nextLinkJson = new JSONObject();
                    HttpRequestBase nextLinkUriRequest = new HttpGet(nextLink);
                    LOG.info("nextLinkUriRequest {0}", nextLinkUriRequest);
                    nextLinkJson = callRequest(nextLinkUriRequest, true);
                    if (nextLinkJson.has("@odata.nextLink") && nextLinkJson.getString("@odata.nextLink") != null && !nextLinkJson.getString("@odata.nextLink").isEmpty()) {
                        morePages = true;
                        nextLink = nextLinkJson.getString("@odata.nextLink");
                        LOG.info("nextLink: {0} ; nextLinkJson: {1} ", nextLink, nextLinkJson);
                    } else {
                        morePages = false;
                        LOG.info("No nextLink defined, final page");
                    }
                    if (nextLinkJson.has("value") && nextLinkJson.get("value") != null) {
                        //value.addAll(nextLinkJson.getJSONArray("value"));
                        for (int i = 0; i < nextLinkJson.getJSONArray("value").length(); i++) {
                            value.put(nextLinkJson.getJSONArray("value").get(i));
                        }
                        LOG.info("nextLinkJson: {0} ", nextLinkJson);
                    } else {
                        LOG.info("nextLinkJson contained no value object or the object was null");
                    }
                }
                values.put("value", value);
                return values;
            } else return firstCall;


        } catch (URISyntaxException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("It was not possible create URI from UriBuider:").append(uriBuilder).append(";")
                    .append(e.getLocalizedMessage());
            throw new ConnectorException(sb.toString(), e);
        }
    }

    protected void callRequestNoContent(HttpEntityEnclosingRequestBase request, Set<Attribute> attributes, JSONObject jsonObject) {
        LOG.info("Request {0} ", request);
        LOG.info("Attributes {0} ", attributes);

        if (request == null) {
            throw new InvalidAttributeValueException("Request not provided or empty");
        }


        HttpEntity entity = null;
        JSONObject json = new JSONObject();

        LOG.info("Translated to JSON");
        LOG.info("JSON {0}", jsonObject);
        try {
            entity = new ByteArrayEntity(jsonObject.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unsupported Encoding when creating object in Box").append(";").append(e.getLocalizedMessage());
            throw new ConnectorException(sb.toString(), e);
        }

        LOG.info("SetEntity {0} to request", entity);
        request.setEntity(entity);
        LOG.info("SetEntity  to request");

        try (CloseableHttpResponse response = executeRequest(request)) {
            processResponseErrors(response);
            LOG.info("response {0}", response);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 204) {
                LOG.ok("204 - No content, Update was succesfull");
            } else {
                LOG.error("Not updated, statusCode: {0}", statusCode);
            }
        } catch (IOException e) {
            throw new ConnectorIOException();
        }


    }

    protected void callRequestNoContentNoJson(HttpEntityEnclosingRequestBase request, List jsonObject) {
        LOG.info("Request {0} ", request);
        LOG.info("Attributes {0} ", jsonObject);

        if (request == null) {
            throw new InvalidAttributeValueException("Request not provided or empty");
        }


        //execute one by one because Microsoft Graph not support SharePoint and Azure AD attributes in one JSONObject
        for (Object list : jsonObject) {
            HttpEntity entity = null;

            try {
                entity = new ByteArrayEntity(list.toString().getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            request.setEntity(entity);

            try (CloseableHttpResponse response = executeRequest(request)) {
                processResponseErrors(response);
                LOG.info("response {0}", response);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 204) {
                    LOG.ok("204 - No content, Update was succesfull");
                } else {
                    LOG.error("Not updated, statusCode: {0}", statusCode);
                }
            } catch (IOException e) {
                throw new ConnectorIOException();
            }

        }
    }

    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new ConnectorIOException(e);
        }
    }
}
