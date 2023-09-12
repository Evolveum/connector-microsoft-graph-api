package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.common.GuardedStringAccessor;
import com.evolveum.polygon.connector.msgraphapi.util.PolyTrustManager;
import com.microsoft.aad.adal4j.AsymmetricKeyCredential;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;

import javax.net.ssl.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

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

    private Boolean validateWithCustomAndDefaultTrust = false;

    GraphEndpoint(MSGraphConfiguration configuration) {

        this(configuration,false);
    }

    GraphEndpoint(MSGraphConfiguration configuration, boolean validateWithCustomAndDefaultTrust) {
        this.configuration = configuration;
        this.uriBuilder = createURIBuilder();
        this.validateWithCustomAndDefaultTrust = validateWithCustomAndDefaultTrust;

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
        LOG.ok("Processing through authenticate method");

        try {
            service = Executors.newFixedThreadPool(1);

            LOG.ok("Loading authentication context");

            AuthenticationContext context = new AuthenticationContext(AUTHORITY + configuration.getTenantId()
                    + "/oauth2/authorize", false, service);

            if(getConfiguration().isValidateWithFailoverTrust() || validateWithCustomAndDefaultTrust){

                context.setSslSocketFactory(createCustomSSLSocketFactory());
            }


            if (configuration.hasProxy()) {

                LOG.info("Authenticating through proxy[{0}]", configuration.getProxyAddress());
                context.setProxy(createProxy());
            }
            Future<AuthenticationResult> future;
            if (configuration.isCertificateBasedAuthentication()) {

                LOG.ok("Processing through certificate based authentication flow");
                X509Certificate certificate = getCertificate(configuration.getCertificatePath());

                PrivateKey privateKey;
                if (configuration.getPrivateKeyPath().toLowerCase().endsWith(".der")) {
                    privateKey = getPrivateKey(configuration.getPrivateKeyPath());
                } else {
                    privateKey = getPrivateKeyFromPem(configuration.getPrivateKeyPath());
                }

                AsymmetricKeyCredential asymmetricKeyCredential = AsymmetricKeyCredential.create(configuration.getClientId(), privateKey, certificate);
                future = context.acquireToken(RESOURCE, asymmetricKeyCredential, null);
            } else {
                GuardedString clientSecret = configuration.getClientSecret();
                GuardedStringAccessor accessorSecret = new GuardedStringAccessor();
                clientSecret.access(accessorSecret);
                ClientCredential credential = new ClientCredential(configuration.getClientId(), accessorSecret.getClearString());

                LOG.ok("About to acquire security token from the authority");
                future = context.acquireToken(RESOURCE, credential, null);
            }
            LOG.ok("Fetching authentication result");

            result = future.get();
        } catch (InterruptedException | ExecutionException | GeneralSecurityException e) {
//
//            if (e.getCause() instanceof SSLException){
//
//                if(useCustomTrustStore && !initialTrustAttemptFailed){
//
//                    LOG.ok("Auth round trip");
//
//                    initialTrustAttemptFailed = true;
//                    authenticate();
//                }
//            }

            throw new ConnectionFailedException("Exception while authenticating to the service provider: "+ e.getLocalizedMessage());
        } catch (IOException e) {
            LOG.error(e, e.toString());
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new ConnectionFailedException("Failed to authenticate");
        }

        this.authenticateResult = result;
    }



    private static X509Certificate getCertificate(String certificationPath) throws IOException, CertificateException {
        InputStream input = new FileInputStream(certificationPath);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(input);

        return certificate;
    }

    private static RSAPrivateKey getPrivateKey(String privateKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File file = new File(privateKeyPath);
        FileInputStream fis = new FileInputStream(file);
        DataInputStream dis = new DataInputStream(fis);

        byte[] keyBytes = new byte[(int) file.length()];
        dis.readFully(keyBytes);
        dis.close();

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(spec);

        return privateKey;
    }

    public RSAPrivateKey getPrivateKeyFromPem(String privateKeyPath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File file = new File(privateKeyPath);
        String key = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());
        String privateKeyPEM = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY----- ", "");

        byte[] encoded = Base64.decodeBase64(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
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
            LOG.info("Executing request through proxy[{0}]", configuration.getProxyAddress());
            clientBuilder.setProxy(
                    new HttpHost(configuration.getProxyAddress().getAddress(), configuration.getProxyAddress().getPort())
            );
        }

        if(configuration.isValidateWithFailoverTrust()){

        clientBuilder.setSSLSocketFactory(new SSLConnectionSocketFactory(createCustomSSLSocketFactory(),
                new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return hostname!=null ? hostname.equals(session.getPeerHost()) : false;
                    }
                }));
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
        if (request.getURI().toString().contains("photo")) {
            request.setHeader("Content-Type", "image/jpg");
        }
        else {
            request.setHeader("Content-Type", "application/json");
        }

        request.setHeader("ConsistencyLevel", "eventual");
        LOG.ok("Request execution -> HttpUriRequest: {0}", request);
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
            throw new InvalidPasswordException(message);
        }
        if (statusCode == 400 && message.contains("Invalid object identifier")) {
            throw new UnknownUidException(message);
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
            if (message.contains("ImageNotFound"))
                return;
            else {
                LOG.info("Status code 404 or 410 caught in processResponseErrors {0}", message);
                throw new UnknownUidException(message);
            }
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
        LOG.ok("callRequest execution");
        String result = null;
        request.setHeader("ConsistencyLevel", "eventual");

        if (LOG.isOk()) {
            LOG.ok("URL in request: {0}", request.getRequestLine().getUri());
            LOG.ok("Enumerating headers");
            for (Header header : request.getAllHeaders()) {
                LOG.info("Headers.. name,value:{0},{1}", header.getName(), header.getValue());
            }
        }
        if (request.getURI().toString().contains("photo")){
            // this uri check is necessary otherwise inspecting of shadow w/o photo returns 404
            return callRequestPhoto(request);
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

             LOG.ok("Response before evaluation: {0}", response.getEntity());

            result = EntityUtils.toString(response.getEntity());
            if (!parseResult) {
                return null;
            }
            return new JSONObject(result);
        } catch (IOException e) {
            throw new ConnectorIOException();
        }

    }

    private JSONObject callRequestPhoto(HttpRequestBase request) {
        String result;

        try (CloseableHttpResponse response = executeRequest(request)) {
            if (response.getStatusLine().getStatusCode() == 404){
                return new JSONObject(Collections.singletonMap("data", null));
            }
            else {
                processResponseErrors(response);
                result = java.util.Base64.getEncoder().encodeToString(EntityUtils.toByteArray(response.getEntity()));
                return new JSONObject(Collections.singletonMap("data", result));
            }
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

    protected JSONObject executeGetRequest(String path, String customQuery, OperationOptions options) {
        LOG.info("executeGetRequest path {0}, customQuery {1}, options: {2}", path, customQuery, options);
        final URIBuilder uribuilder = createURIBuilder().setPath(path);

        if (customQuery != null) {
            uribuilder.setCustomQuery(customQuery);
            LOG.ok("setCustomQuery {0}", uribuilder);
        }

        try {
            URI uri = uribuilder.build();
            LOG.info("uri {0}", uri);
            HttpRequestBase request = new HttpGet(uri);
            return callRequest(request, true);

        } catch (URISyntaxException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("It was not possible create URI from UriBuilder:").append(uriBuilder).append(";")
                    .append(e.getLocalizedMessage());
            throw new ConnectorException(sb.toString(), e);
        }
    }

    // If the resource indicated by the "path" argument does not support paging, the "paging" argument must be false
    protected JSONArray executeListRequest(String path, String customQuery, OperationOptions options, boolean paging) {
        JSONArray value = new JSONArray();
        executeListRequest(path, customQuery, options, paging, (op, object) -> {
            value.put(object);
            return true;
        });
        return value;
    }

    // If the resource indicated by the "path" argument does not support paging, the "paging" argument must be false
    protected void executeListRequest(String path, String customQuery, OperationOptions options,
                                      boolean paging, ObjectProcessing.JSONObjectHandler handler) {
        LOG.info("executeGetRequest path {0}, customQuery {1}, options: {2}", path, customQuery, options);
        final URIBuilder uribuilder = createURIBuilder().setPath(path);

        StringBuilder query = new StringBuilder();
        if (customQuery != null) {
            query.append(customQuery);
        }

        if (paging) {
            String pageSize = configuration.getPageSize();
            if (StringUtil.isNotBlank(pageSize)) {
                if (customQuery != null) {
                    query.append("&");
                }
                query.append(TOP);
                query.append("=");
                query.append(pageSize);
            }
        }

        if (query.length() > 0) {
            uribuilder.setCustomQuery(query.toString());
            LOG.ok("setCustomQuery {0}", uribuilder);
        }

        URI uri;
        try {
            uri = uribuilder.build();
        } catch (URISyntaxException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("It was not possible create URI from UriBuilder:").append(uriBuilder).append(";")
                    .append(e.getLocalizedMessage());
            throw new ConnectorException(sb.toString(), e);
        }

        // Handle paging if the response contains @odata.nextLink
        do {
            HttpRequestBase request = new HttpGet(uri);
            LOG.info("request {0}", request);

            final JSONObject response = callRequest(request, true);

            if (hasNextLink(response)) {
                String nextLink = getNextLink(response);
                LOG.info("nextLink: {0}", nextLink);
                uri = URI.create(nextLink);
            } else {
                LOG.info("No nextLink defined, final page");
                uri = null;
            }
            if (hasJSONArray(response)) {
                LOG.info("response: {0} ", response);
                JSONArray jsonArray = getJSONArray(response);
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (!handler.handle(options, jsonArray.getJSONObject(i))) {
                        return;
                    }
                }
            } else {
                LOG.info("nextLinkJson contained no value object or the object was null");
            }
        } while (uri != null);
    }

    private boolean hasJSONArray(JSONObject object) {
        return object.has("value") && object.get("value") != null;
    }

    private JSONArray getJSONArray(JSONObject object) {
        return object.getJSONArray("value");
    }

    private boolean hasNextLink(JSONObject object) {
        return object.has("@odata.nextLink") && object.getString("@odata.nextLink") != null && !object.getString("@odata.nextLink").isEmpty();
    }

    private String getNextLink(JSONObject object) {
        return object.getString("@odata.nextLink");
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
                LOG.ok("204 - No content, Update was successful");
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
                    LOG.ok("204 - No content, Update was successful");
                } else {
                    LOG.error("Not updated, statusCode: {0}", statusCode);
                }
            } catch (IOException e) {
                throw new ConnectorIOException();
            }

        }
    }

    private SSLSocketFactory createCustomSSLSocketFactory() {

        LOG.ok("Initializing custom SSLSocketFactory method");


        SSLContext sslContext;
        try {

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);

            X509TrustManager defaultTm = null;

            for (TrustManager tm : trustManagerFactory.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    defaultTm = (X509TrustManager) tm;
                    break;
                }
            }

            X509TrustManager customManager = buildCustomManager(configuration.getPathToFailoverTrustStore());

            PolyTrustManager polyTrustManager;
            if(validateWithCustomAndDefaultTrust){

                 polyTrustManager = new PolyTrustManager(defaultTm, customManager) ;
            } else {

                 polyTrustManager = new PolyTrustManager(customManager) ;
            }


            LOG.info("Attempt to initialize custom SSL context, using custom trust manager");
            sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null, new TrustManager[]{polyTrustManager}, null);

        } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException e) {

            LOG.error("Exception while loading custom trustStore" + e);

            return null;
        }

        LOG.info("SSL context initialize, about to return custom SSL socket factory");
        return sslContext.getSocketFactory();
    }

    private X509TrustManager buildCustomManager(String path) throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException {

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());;

        FileInputStream trustStoreIs = new FileInputStream(path);

        /// Providing empty password ro read CA certificates
        keyStore.load(trustStoreIs, null);
        trustStoreIs.close();

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);


        X509TrustManager customManager = null;
        for (TrustManager tm : trustManagerFactory.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                customManager = (X509TrustManager) tm;
                break;
            }
        }

        return customManager;
    }

    public void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            throw new ConnectorIOException(e);
        }
    }
}
