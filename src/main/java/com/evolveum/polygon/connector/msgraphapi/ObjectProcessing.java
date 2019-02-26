package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.common.GuardedStringAccessor;
import com.google.gson.Gson;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.Configuration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ObjectProcessing {

    private static final String ME = "/me";
    private String DELETE = "delete";
    private String DELIMITER = "\\.";
    private String DEFAULT = "default";
    private String TYPE = "type";
    private String OPERATION = "operation";
    private String DOT = ".";
    private String BLANK = "blank";
    private String SCHEMA = "schema";
    protected static final String SKIP = "$skip";
    protected static final String TOP = "$top";
    protected static final String STARTSWITH = "startswith";


    private String token;

    private final static String API_ENDPOINT = "graph.microsoft.com/v1.0";

    private final static String AUTHORITY = "https://login.microsoftonline.com/";
    private final static String RESOURCE = "https://graph.microsoft.com";


    protected static final String USERS = "/users";
    protected static final String GROUPS = "/groups";


    private MSGraphConfiguration configuration;
    protected CloseableHttpClient httpclient;
    private URIBuilder uriBuilder;
    private MSGraphConnector connector;

    public ObjectProcessing(MSGraphConfiguration configuration, MSGraphConnector connector) {
        this.configuration = configuration;
        this.uriBuilder = new URIBuilder().setScheme("https").setHost(API_ENDPOINT);
        this.connector = connector;

    }

    public void test() {
        LOG.info("Start test.");
        URIBuilder uriBuilder = getURIBuilder();
        uriBuilder.setPath(USERS);
        LOG.info("path: {0}", uriBuilder);
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new ConnectorException("It is not possible to create URI" + e.getLocalizedMessage(), e);
        }
        HttpGet request = new HttpGet(uri);

        callRequest(request, false);
    }

    protected static final Log LOG = Log.getLog(MSGraphConnector.class);

    public Configuration getConfiguration() {
        return configuration;
    }


    private static AuthenticationResult getAccessTokenFromUserCredentials(MSGraphConfiguration configuration) {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        GuardedString clientSecret = configuration.getClientSecret();
        GuardedStringAccessor accessorSecret = new GuardedStringAccessor();
        clientSecret.access(accessorSecret);
        LOG.info("getAccessTokenFromUserCredentials");

        try {
            service = Executors.newFixedThreadPool(1);
            ClientCredential credential = new ClientCredential(configuration.getClientId(), accessorSecret.getClearString());
            context = new AuthenticationContext(AUTHORITY + configuration.getTenantId()
                    + "/oauth2/authorize", false, service);
            Future<AuthenticationResult> future = context.acquireToken(
                    RESOURCE,
                    credential,
                    null);
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            LOG.error(e.toString());
        } finally {
            service.shutdown();
        }

        if (result == null) {
            throw new AuthenticationException("authentication result was null");
        }

        return result;
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


    private CloseableHttpResponse executeRequest(HttpUriRequest request) {
        if (request == null) {
            throw new InvalidAttributeValueException("Request not provided");
        }
        request.setHeader("Authorization", getAccessTokenFromUserCredentials(configuration).getAccessToken());
        request.setHeader("Content-Type", "application/json");
        LOG.info("HtttpUriRequest: {0}", request);
        LOG.info(request.toString());
        CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;

        String responseXml = Arrays.toString(request.getAllHeaders());


        try {
            response = client.execute(request);
            LOG.info("response {0}", response);
            processResponseErrors(response);
            return response;

        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("It is not possible to execute request:").append(request.toString()).append(";")
                    .append(e.getLocalizedMessage());
            throw new ConnectorIOException(sb.toString(), e);
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
            throw new ConnectorIOException(message);
        }
        if (statusCode == 401 || statusCode == 402 || statusCode == 403 || statusCode == 407) {
            throw new PermissionDeniedException(message);
        }
        if (statusCode == 404 || statusCode == 410) {
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

        throw new ConnectorException(message);
    }




    protected JSONObject callRequest(HttpRequestBase request, boolean parseResult) {
        if (request == null) {
            throw new InvalidAttributeValueException("Request not provided or empty");
        }
        LOG.info("callRequest");
        String result = null;
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
        URIBuilder uribuilder = getURIBuilder();
        uribuilder.clearParameters();


        if (customQuery != null && options != null && paging) {
            Integer perPage = options.getPageSize();
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
            JSONObject firstCall = callRequest(request, true);

            //call skipToken for paging
            if (options != null && paging) {
                Integer page = options.getPagedResultsOffset();
                if (page != null) {
                    if (page == 0 || page == 1) {
                        return firstCall; // no need for skipToken actually returned the first page
                    } else {
                        String nextLink;
                        try {
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
                return firstCall;
            } else return firstCall;


        } catch (URISyntaxException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("It was not possible create URI from UriBuider:").append(uriBuilder).append(";")
                    .append(e.getLocalizedMessage());
            throw new ConnectorException(sb.toString(), e);
        }
    }

    public URIBuilder getURIBuilder() {
        return this.uriBuilder;
    }

    protected void getIfExists(JSONObject object, String attrName, Class<?> type, ConnectorObjectBuilder builder) {
        if (object.has(attrName) && object.get(attrName) != null && !JSONObject.NULL.equals(object.get(attrName)) && !String.valueOf(object.get(attrName)).isEmpty()) {
            if (type.equals(String.class)) {
                addAttr(builder, attrName, String.valueOf(object.get(attrName)));
            } else {
                addAttr(builder, attrName, object.get(attrName));
            }
        }
    }



    protected void getMultiIfExists(JSONObject object, String attrName, ConnectorObjectBuilder builder) {

        if (object.has(attrName)) {
            Object valueObject = object.get(attrName);
            if (valueObject != null && !JSONObject.NULL.equals(valueObject)) {
                List<String> values = new ArrayList<>();
                if (valueObject instanceof JSONArray) {
                    JSONArray objectArray = object.getJSONArray(attrName);
                    for (int i = 0; i < objectArray.length(); i++) {
                        if (objectArray.get(i) instanceof JSONObject) {
                            JSONObject jsonObject = objectArray.getJSONObject(i);
                            values.add(jsonObject.toString());
                        } else {
                            values.add(String.valueOf(objectArray.get(i)));
                        }
                    }
                    builder.addAttribute(attrName, values.toArray());
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unsupported value: ").append(valueObject).append(" for attribute name:").append(attrName)
                            .append(" from: ").append(object);
                    throw new InvalidAttributeValueException(sb.toString());
                }
            }
        }
    }

    protected <T> T addAttr(ConnectorObjectBuilder builder, String attrName, T attrVal) {
        if (attrVal != null) {
            if (attrVal instanceof String) {
                String unescapeAttrVal = StringEscapeUtils.unescapeXml((String) attrVal);
                builder.addAttribute(attrName, unescapeAttrVal);
            } else {
                builder.addAttribute(attrName, attrVal);
            }
        }
        return attrVal;
    }


    protected String getUIDIfExists(JSONObject object, String nameAttr, ConnectorObjectBuilder builder) {
        LOG.info("getUIDIfExists nameAttr: {0} bulder {1}", nameAttr, builder.toString());
        if (object.has(nameAttr)) {
            String uid = object.getString(nameAttr);
            builder.setUid(new Uid(String.valueOf(uid)));
            return uid;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing required attribute: ").append(nameAttr)
                    .append("for converting JSONObject to ConnectorObject.");
            throw new InvalidAttributeValueException(sb.toString());
        }
    }

    protected int getUIDIfExists(JSONObject object, String nameAttr) {
        if (object.has(nameAttr)) {
            int uid = object.getInt(nameAttr);
            return uid;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing required attribute: ").append(nameAttr)
                    .append("for converting JSONObject to ConnectorObject.");
            throw new InvalidAttributeValueException(sb.toString());
        }
    }

    protected void getNAMEIfExists(JSONObject object, String nameAttr, ConnectorObjectBuilder builder) {
        if (object.has(nameAttr)) {
            builder.setName(object.getString(nameAttr));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing required attribute: ").append(nameAttr)
                    .append("for converting JSONObject to ConnectorObject.");
            throw new InvalidAttributeValueException(sb.toString());
        }
    }


    protected void invalidAttributeValue(String attrName, Filter query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Value of").append(attrName).append("attribute not provided for query: ").append(query);
        throw new InvalidAttributeValueException(sb.toString());
    }

    static abstract class Node {
    }

    static class IntermediateNode extends Node {
        public Map<String, Node> keyValueMap = new LinkedHashMap<>();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{ \"");
            sb.append(keyValueMap.entrySet().stream().map(entry -> entry.getKey() + "\" : " + entry.getValue())
                    .collect(Collectors.joining(", ")));

            sb.append("}");
            return sb.toString();
        }
    }


    static class LeafNode extends Node {
        public String value;
        public boolean isMultiValue;

        @Override
        public String toString() {
            if (isMultiValue) return value;
            else return "\"" + value + "\"";
        }
    }

    protected JSONObject buildLayeredAttributeJSON(Set<Attribute> multiLayerAttribute) {
        JSONObject json = new JSONObject();
        for (Attribute attribute : multiLayerAttribute) {
            String[] result;
            if (attribute.getName().equals("__PASSWORD__")) {
                result = "passwordProfile.password".split("\\.");
                LOG.info("BINGO");
            } else {
                result = attribute.getName().split("\\.");
            }
            IntermediateNode root = new IntermediateNode();
            IntermediateNode currentNode = root;
            for (int i = 0; i < result.length - 1; i++) {
                Node node = currentNode.keyValueMap.get(result[i]);
                if (node == null) {
                    IntermediateNode child = new IntermediateNode();
                    currentNode.keyValueMap.put(result[i].trim(), child);
                    currentNode = child;
                } else {
                    currentNode = (IntermediateNode) node;
                }
            }
            LeafNode leaf = new LeafNode();
            MSGraphConnector connector = new MSGraphConnector();
            boolean isMultiValue = connector.isAttributeMultiValues(ObjectClass.ACCOUNT_NAME, attribute.getName());
            LOG.info("attribute {0} isMultiValue {1}", attribute, isMultiValue);

            leaf.isMultiValue = isMultiValue;
            if (isMultiValue) {
                //multi value with []
                StringBuilder multiValue = new StringBuilder("[");
                List<Object> attributes = attribute.getValue();
                Iterator itr = attributes.iterator();
                LOG.info("multi");
                for (Object attribute1 : attributes) {
                    multiValue.append("\"").append(attribute1.toString()).append("\"");
                }
                multiValue.append("]");
                leaf.value = multiValue.toString().replace("\"\"", "\",\"");
            } else {
                LOG.info("single");
                //single value , without []
                if (attribute.getName().equals("__PASSWORD__")) {
                    GuardedString guardedString = (GuardedString) AttributeUtil.getSingleValue(attribute);
                    GuardedStringAccessor accessor = new GuardedStringAccessor();
                    guardedString.access(accessor);
                    leaf.value = accessor.getClearString();
                } else {
                    leaf.value = AttributeUtil.getAsStringValue(attribute);

                }
            }


            currentNode.keyValueMap.put(result[result.length - 1], leaf);
            JSONObject jsonObject = new JSONObject(root.toString());
            //empty json
            if (jsonObject.length() == 0) {
                json = jsonObject;
            } else {

                String key = jsonObject.keys().next();
                Object value = jsonObject.get(key);
                JSONObject newJSONObject = new JSONObject();
                newJSONObject.put(key, value);
                deepMerge(jsonObject, json);

            }
        }

        return json;
    }

    protected List<Object> buildLayeredAtrribute(Set<Attribute> multiLayerAttribute) {
        LinkedList<Object> list = new LinkedList<>();
        String json = "";
        for (Attribute attribute : multiLayerAttribute) {
            String[] result;
            if (attribute.getName().equals("__PASSWORD__")) {
                result = "passwordProfile.password".split("\\.");
                LOG.info("BINGO");
            } else {
                result = attribute.getName().split("\\.");
            }
            IntermediateNode root = new IntermediateNode();

            IntermediateNode currentNode = root;
            for (int i = 0; i < result.length - 1; i++) {
                Node node = currentNode.keyValueMap.get(result[i]);
                if (node == null) {
                    IntermediateNode child = new IntermediateNode();
                    currentNode.keyValueMap.put(result[i].trim(), child);
                    currentNode = child;
                } else {
                    currentNode = (IntermediateNode) node;
                }
            }
            LeafNode leaf = new LeafNode();
            MSGraphConnector connector = new MSGraphConnector();
            boolean isMultiValue = connector.isAttributeMultiValues(ObjectClass.ACCOUNT_NAME, attribute.getName());
            leaf.isMultiValue = isMultiValue;
            if (isMultiValue) {
                //multi value with []
                StringBuilder multiValue = new StringBuilder("[");
                List<Object> attributes = attribute.getValue();
                Iterator itr = attributes.iterator();

                for (Object attribute1 : attributes) {
                    multiValue.append("\"").append(attribute1.toString()).append("\"");
                }
                multiValue.append("]");
                leaf.value = multiValue.toString().replace("\"\"", "\",\"");
            } else {
                //single value , without []
                if (attribute.getName().equals("__PASSWORD__")) {
                    GuardedString guardedString = (GuardedString) AttributeUtil.getSingleValue(attribute);
                    GuardedStringAccessor accessor = new GuardedStringAccessor();
                    guardedString.access(accessor);
                    leaf.value = accessor.getClearString();
                } else {
                    leaf.value = AttributeUtil.getAsStringValue(attribute);

                }
            }

            currentNode.keyValueMap.put(result[result.length - 1], leaf);
            JSONObject jsonObject = new JSONObject(root.toString());
            //empty json
            if (json.isEmpty()) {
                json = jsonObject.toString();
                list.add(json);
            } else {
                String key = jsonObject.keys().next();
                Object value = jsonObject.get(key);

                //JSON Object no.1
                JSONObject newJSONObject = new JSONObject();
                newJSONObject.put(key, value);
                list.add(newJSONObject);

            }
        }

        return list;
    }

    public static JSONObject deepMerge(JSONObject source, JSONObject target) throws JSONException {
        for (String key : JSONObject.getNames(source)) {
            Object value = source.get(key);
            if (!target.has(key)) {
                // new value for "key":
                target.put(key, value);
            } else {
                // existing value for "key" - recursively deep merge:
                if (value instanceof JSONObject) {
                    JSONObject valueJson = (JSONObject) value;
                    deepMerge(valueJson, target.getJSONObject(key));
                } else {
                    target.put(key, value);
                }
            }
        }
        return target;
    }

}


