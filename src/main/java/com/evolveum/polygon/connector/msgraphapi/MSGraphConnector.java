

package com.evolveum.polygon.connector.msgraphapi;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpRequestBase;
//import org.apache.http.*;
//import org.apache.http.protocol.*;
//import org.apache.http.client.protocol.HttpClientContext;
//import org.apache.http.client.methods.*;
//import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
//import org.apache.http.client.*;
//import org.apache.http.impl.client.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@ConnectorClass(displayNameKey = "msgraphconnector.connector.display", configurationClass = MSGraphConfiguration.class)
public class MSGraphConnector implements Connector,
        CreateOp,
        DeleteOp,
        SearchOp<Filter>,
        TestOp,
        UpdateDeltaOp,
        SchemaOp,
        UpdateOp,
        SyncOp,
        UpdateAttributeValuesOp {

    private static final Log LOG = Log.getLog(MSGraphConnector.class);

    private MSGraphConfiguration configuration;

    private static final String USERS = "/users";
    private static final String GROUPS = "/groups";

    private Schema schema = null;
    private CloseableHttpClient httpclient;


    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        LOG.info("Initialize");

        this.configuration = (MSGraphConfiguration) configuration;
        this.configuration.validate();
    }

    @Override
    public void dispose() {
        LOG.info("Dispose");

        configuration = null;
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {
        validateObjectClass(objectClass);
        if (attributes == null) {
            LOG.error("Attribute of type Set<Attribute> not provided.");
            throw new InvalidAttributeValueException("Attribute of type Set<Attribute> not provided.");
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
            UserProcessing userProcessing = new UserProcessing(configuration, this);
            return userProcessing.createUser(null, attributes);


        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            return groupProcessing.createOrUpdateGroup(null, attributes);

        } else {
            throw new UnsupportedOperationException("Unsupported object class " + objectClass);
        }

    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {

        if (uid.getUidValue() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Uid not provided or empty:").append(uid.getUidValue()).append(";");
            throw new InvalidAttributeValueException(sb.toString());
        }
        LOG.info("DELETE METHOD UID VALUE: {0}", uid.getUidValue());

        if (objectClass == null) {
            throw new InvalidAttributeValueException("ObjectClass value not provided");
        }
        LOG.info("DELETE METHOD OBJECTCLASS VALUE: {0}", objectClass);

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            UserProcessing user = new UserProcessing(configuration, this);
            user.delete(uid);

        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing group = new GroupProcessing(configuration);
            group.delete(uid);

        }
    }

    @Override
    public Schema schema() {
        if (this.schema == null) {
            SchemaBuilder schemaBuilder = new SchemaBuilder(MSGraphConnector.class);
            UserProcessing userProcessing = new UserProcessing(configuration, this);
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            LicenseProcessing licenseProcessing = new LicenseProcessing(configuration);

            userProcessing.buildUserObjectClass(schemaBuilder);
            groupProcessing.buildGroupObjectClass(schemaBuilder);
            licenseProcessing.buildLicenseObjectClass(schemaBuilder);

            return schemaBuilder.build();
        }
        return this.schema;
    }

    @Override
    public FilterTranslator<Filter> createFilterTranslator(ObjectClass arg0, OperationOptions arg1) {
        return new FilterTranslator<Filter>() {
            @Override
            public List<Filter> translate(Filter filter) {
                return CollectionUtil.newList(filter);
            }
        };
    }

    @Override
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
       if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
          String getPath = USERS + "/microsoft.graph.delta";
          String customQuery = "$deltaToken=latest";
          GraphEndpoint endpoint = new GraphEndpoint(configuration);
          URIBuilder uriBuilder = endpoint.createURIBuilder().clearParameters();
          uriBuilder.setCustomQuery(customQuery);
          uriBuilder.setPath(getPath);
          LOG.info("Get latest sync token uri is {0} ", uriBuilder.toString());
          try {
             URI uri = uriBuilder.build();
             HttpGet syncTokenRequest = new HttpGet(uri);
             JSONObject  syncTokenJson = endpoint.callRequest(syncTokenRequest, true);
             LOG.info("SyncToken JSON content {0}", syncTokenJson);
             String deltaLink = syncTokenJson.getString("@odata.deltaLink");
             return new SyncToken(deltaLink);
          } catch (URISyntaxException e) {
            StringBuilder sb = new StringBuilder();
            sb.append("It was not possible create URI from UriBuider:").append(uriBuilder).append(";")
                    .append(e.getLocalizedMessage());
            throw new ConnectorException(sb.toString(), e);
         }

       } else {
            LOG.error("Attribute of type ObjectClass is not supported. Only Account objectclass is supported for getLatestSyncToken currently.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported. Only Account objectclass is supported for getLatestSyncToken currently.");
       }
    }

    @Override
    public void sync(ObjectClass objectClass, SyncToken fromToken, SyncResultsHandler handler, OperationOptions oo) {
       if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
          if (fromToken == null) {
               fromToken = getLatestSyncToken(objectClass);
          }
          LOG.info("starting sync");
          LOG.info("ObjectClass.ACCOUNT_NAME is " + ObjectClass.ACCOUNT_NAME);
          LOG.info("sync ObjectClass is " + objectClass.getObjectClassValue() + "--");
          LOG.info("fromToken value is " + fromToken);
          GraphEndpoint endpoint = new GraphEndpoint(configuration);
          UserProcessing userProcessor = new UserProcessing(configuration, this);
          String nextDeltaLink = new String();
          HttpRequestBase request = new HttpGet((String)fromToken.getValue());
            //request.setRetryHandler(new StandardHttpRequestRetryHandler(5, true));
          JSONObject firstCall = endpoint.callRequest(request, true);
          JSONArray value = new JSONArray();
          String nextLink = new String();
          //JSONObject values = new JSONObject();
          boolean morePages = false;
          if (firstCall.has("@odata.nextLink") && firstCall.getString("@odata.nextLink") != null && !firstCall.getString("@odata.nextLink").isEmpty() ) {
             morePages = true;
             nextLink = firstCall.getString("@odata.nextLink");
             LOG.info("nextLink: {0} ; firstCall: {1} ", nextLink, firstCall);
          }
          else {
             morePages = false;
             LOG.info("No nextLink defined, final page was firstCall");
             nextDeltaLink = firstCall.getString("@odata.deltaLink");
          }
          if ( firstCall.has("value") && firstCall.get("value") != null  ) {
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
             nextLinkJson = endpoint.callRequest(nextLinkUriRequest, true);
             if (nextLinkJson.has("@odata.nextLink") && nextLinkJson.getString("@odata.nextLink") != null && !nextLinkJson.getString("@odata.nextLink").isEmpty() ) {
                morePages = true;
                nextLink = nextLinkJson.getString("@odata.nextLink");
                LOG.info("nextLink: {0} ; nextLinkJson: {1} ", nextLink, nextLinkJson);
             }
             else {
                morePages = false;
                LOG.info("No nextLink defined, final page");
                nextDeltaLink = nextLinkJson.getString("@odata.deltaLink");
             }
             if ( nextLinkJson.has("value") && nextLinkJson.get("value") != null  ) {
                //value.addAll(nextLinkJson.getJSONArray("value"));
                for (int i = 0; i < nextLinkJson.getJSONArray("value").length(); i++) {
                   value.put(nextLinkJson.getJSONArray("value").get(i));
                }
                LOG.info("nextLinkJson: {0} ", nextLinkJson);
             } else {
                LOG.info("nextLinkJson contained no value object or the object was null");
             }
          }
          //values.put("value",value);
          SyncToken nextLinkSyncToken = new SyncToken(nextDeltaLink);
          int length = value.length();
          LOG.info("User JSONArray length for SyncOp: {0}", length);
          for (int i = 0; i < length; i++) {
             JSONObject user = value.getJSONObject(i);
             LOG.info("Processing user json object, {0}", user);
             ConnectorObjectBuilder userConnectorObjectBuilder = userProcessor.convertUserJSONObjectToConnectorObject(user);
             SyncDeltaBuilder builder = new SyncDeltaBuilder();
             builder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
             builder.setObjectClass(ObjectClass.ACCOUNT);
             builder.setToken(nextLinkSyncToken);
             ConnectorObject connectorObject = userConnectorObjectBuilder.build();
             builder.setObject(connectorObject);
             handler.handle(builder.build());
          }


       } else {
            LOG.error("Attribute of type ObjectClass is not supported. Only Account objectclass is supported for SyncOp currently.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported. Only Account objectclass is supported for SyncOp currently.");
       }

    }

    @Override
    public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        if (objectClass == null) {
            LOG.error("Attribute of type ObjectClass not provided.");
            throw new InvalidAttributeValueException("Attribute of type ObjectClass is not provided.");
        }

        if (handler == null) {
            LOG.error("Attribute of type ResultsHandler not provided.");
            throw new InvalidAttributeValueException("Attribute of type ResultsHandler is not provided.");
        }

        if (options == null) {
            LOG.error("Attribute of type OperationOptions not provided.");
            throw new InvalidAttributeValueException("Attribute of type OperationOptions is not provided.");
        }

        LOG.info("executeQuery on {0}, filter: {1}, options: {2}", objectClass, query, options);

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            UserProcessing userProcessing = new UserProcessing(configuration, this);

            userProcessing.executeQueryForUser(query, handler, options);

        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            groupProcessing.executeQueryForGroup(query, handler, options);
        } else if (objectClass.is(LicenseProcessing.OBJECT_CLASS_NAME)) {
            LicenseProcessing licenseProcessing = new LicenseProcessing(configuration);
            licenseProcessing.executeQueryForLicense(query, handler, options);
        } else {
            LOG.error("Attribute of type ObjectClass is not supported.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported.");
        }

    }

    @Override
    public void test() {
        final GraphEndpoint endpoint = new GraphEndpoint(configuration);
        LOG.info("Start test.");
        final URIBuilder uriBuilder = endpoint.createURIBuilder();
        uriBuilder.setPath(USERS);
        LOG.info("path: {0}", uriBuilder);
        final URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new ConnectorException("It is not possible to create URI" + e.getLocalizedMessage(), e);
        }
        HttpGet request = new HttpGet(uri);
        new GraphEndpoint(configuration).callRequest(request, false);
    }


    @Override
    public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> attrsDelta, OperationOptions options) {
        validateObjectClassAndUID(objectClass, uid);

        if (attrsDelta == null) {
            LOG.error("Parameter of type Set<AttributeDelta> not provided.");
            throw new InvalidAttributeValueException("Parameter of type Set<AttributeDelta> not provided.");
        }

        if (options == null) {
            LOG.error("Parameter of type OperationOptions not provided.");
            throw new InvalidAttributeValueException("Parameter of type OperationOptions not provided.");
        }
        LOG.info("UpdateDelta with ObjectClass: {0} , uid: {1} , attrsDelta: {2} , options: {3}  ", objectClass, uid, attrsDelta, options);
        Set<Attribute> attributeReplace = new HashSet<>();
        Set<AttributeDelta> attrsDeltaMultivalue = new HashSet<>();
        for (AttributeDelta attrDelta : attrsDelta) {
            List<Object> replaceValue = attrDelta.getValuesToReplace();
            if (replaceValue != null) {
                LOG.info("attributeReplace.add {0} {1}", AttributeBuilder.build(attrDelta.getName(), replaceValue));
                attributeReplace.add(AttributeBuilder.build(attrDelta.getName(), replaceValue));
            } else {
                LOG.info("attrsDeltaMultivalue.add {0}", attrDelta);
                attrsDeltaMultivalue.add(attrDelta);
            }
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
            if (!attributeReplace.isEmpty()) {
                UserProcessing userProcessing = new UserProcessing(configuration, this);
                userProcessing.updateUser(uid, attributeReplace);
            }
            if (!attrsDeltaMultivalue.isEmpty()) {
                UserProcessing userProcessing = new UserProcessing(configuration, this);
                userProcessing.updateDeltaMultiValues(uid, attrsDeltaMultivalue, options);

            }
        } else if (objectClass.is(ObjectClass.GROUP_NAME)) { // __GROUP__
            if (!attributeReplace.isEmpty()) {
                new GroupProcessing(configuration).createOrUpdateGroup(uid, attributeReplace);
            }
            if (!attrsDeltaMultivalue.isEmpty()) {
                new GroupProcessing(configuration).updateDeltaMultiValuesForGroup(uid, attrsDeltaMultivalue, options);
            }
        } else {
            LOG.error("The value of the ObjectClass parameter is unsupported.");
            throw new UnsupportedOperationException("The value of the ObjectClass parameter is unsupported.");
        }
        return null;

    }

    @Override
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions operationOptions) {
        validateObjectClassAndUID(objectClass, uid);
        if (operationOptions == null) {
            LOG.error("Attribute of type OperationOptions not provided.");
        }

        if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            groupProcessing.addToGroup(uid, attributes);

        }

        return uid;
    }

    @Override
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions operationOptions) {
        if (objectClass == null) {
            LOG.error("Parameter of type ObjectClass not provided.");
            throw new InvalidAttributeValueException("Parameter of type ObjectClass not provided.");
        }

        if (uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
            LOG.error("Parameter of type Uid not provided or is empty.");
            throw new InvalidAttributeValueException("Parameter of type Uid not provided or is empty.");
        }
        if (operationOptions == null) {
            LOG.error("Attribute of type OperationOptions not provided.");
        }

        if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            groupProcessing.removeFromGroup(uid, attributes);
        }

        return uid;
    }

    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions operationOptions) {
        validateObjectClassAndUID(objectClass, uid);
        if (operationOptions == null) {
            LOG.error("Attribute of type OperationOptions not provided.");
            throw new InvalidAttributeValueException("Attribute of type OperationOptions is not provided.");
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            UserProcessing userProcessing = new UserProcessing(configuration, this);
            userProcessing.updateUser(uid, attributes);


        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            groupProcessing.createOrUpdateGroup(uid, attributes);
        }
        return uid;
    }

    private void validateObjectClassAndUID(ObjectClass objectClass, Uid uid) throws InvalidAttributeValueException {
        validateObjectClass(objectClass);
        if (uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
            LOG.error("Parameter of type Uid not provided or is empty.");
            throw new InvalidAttributeValueException("Parameter of type Uid not provided or is empty.");
        }
    }

    private void validateObjectClass(ObjectClass objectClass) throws InvalidAttributeValueException {
        if (objectClass == null) {
            LOG.error("Parameter of type ObjectClass not provided.");
            throw new InvalidAttributeValueException("Parameter of type ObjectClass not provided.");
        }
    }

}
