

package com.evolveum.polygon.connector.msgraphapi;

import com.evolveum.polygon.connector.msgraphapi.util.FilterHandler;
import com.evolveum.polygon.connector.msgraphapi.util.ResourceQuery;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


@ConnectorClass(displayNameKey = "msgraphconnector.connector.display", configurationClass = MSGraphConfiguration.class)
public class MSGraphConnector implements Connector,
        PoolableConnector,
        CreateOp,
        DeleteOp,
        SearchOp<Filter>,
        TestOp,
        UpdateDeltaOp,
        SchemaOp,
        SyncOp,
        DiscoverConfigurationOp {

    private static final Log LOG = Log.getLog(MSGraphConnector.class);

    private MSGraphConfiguration configuration;

    private static final String USERS = "/users";
    private static final String GROUPS = "/groups";

    private GraphEndpoint graphEndpoint = null;

    public GraphEndpoint getGraphEndpoint() {
        if (graphEndpoint == null) {
            // Cache
            graphEndpoint = new GraphEndpoint(configuration);
        }
        return graphEndpoint;
    }

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

        if (graphEndpoint != null) {
            graphEndpoint.close();
            graphEndpoint = null;
        }
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {
        validateObjectClass(objectClass);
        if (attributes == null || attributes.isEmpty()) {
            LOG.error("Attribute of type Set<Attribute> not provided or empty.");
            throw new InvalidAttributeValueException("Attribute of type Set<Attribute> not provided or empty.");
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
            UserProcessing userProcessing = new UserProcessing(getGraphEndpoint(), getSchemaTranslator());
            return userProcessing.createUser( attributes);

        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(getGraphEndpoint());
            return groupProcessing.createGroup(attributes);

        } else if (objectClass.is(RoleProcessing.ROLE_NAME)) {
            RoleProcessing roleProcessing = new RoleProcessing(getGraphEndpoint());
            return roleProcessing.createRole(attributes);
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
            UserProcessing user = new UserProcessing(getGraphEndpoint(), getSchemaTranslator());
            user.delete(uid);

        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing group = new GroupProcessing(getGraphEndpoint());
            group.delete(uid);

        } else if (objectClass.is(RoleProcessing.ROLE_NAME)) {
            RoleProcessing role = new RoleProcessing(getGraphEndpoint());
            role.delete(uid);
        } else {
            throw new UnsupportedOperationException("Unsupported object class " + objectClass);
        }
    }

    protected SchemaTranslator getSchemaTranslator() {
        return getGraphEndpoint().getSchemaTranslator();
    }

    @Override
    public Schema schema() {
        // always fresh schema when this method is called
        return getSchemaTranslator().getConnIdSchema();
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
    public SyncToken getLatestSyncToken(ObjectClass objectClass, OperationOptions oo) {

        LOG.ok("Evaluation of getLatestSyncToken method with operation options set to: {0}", oo);
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {

            String getPath = USERS + "/microsoft.graph.delta";
            String customQuery = "$deltaToken=latest";
            GraphEndpoint endpoint = getGraphEndpoint();
            UserProcessing up = new UserProcessing(endpoint, getSchemaTranslator());

            String selector = up.getSelectorSingle(oo);

           // LOG.ok("Fetching latest token with following selector: {0}", selector);

            URIBuilder uriBuilder = endpoint.createURIBuilder().clearParameters();
            uriBuilder.setCustomQuery(customQuery + "&" + selector);
            uriBuilder.setCustomQuery(customQuery);
            uriBuilder.setPath(getPath);
            LOG.info("Get latest sync token uri is {0} ", uriBuilder);
            try {
                URI uri = uriBuilder.build();
                HttpGet syncTokenRequest = new HttpGet(uri);
                JSONObject syncTokenJson = endpoint.callRequest(syncTokenRequest, true);
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
        LOG.ok("Evaluation of SYNC op method regarding the object class {0} with the following options: {1}",objectClass
                , oo);
        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            if (fromToken == null) {

                LOG.ok("Empty token, fetching latest sync token");
                fromToken = getLatestSyncToken(objectClass, oo);
            }
            LOG.info("Starting sync operation");
            LOG.info("ObjectClass.ACCOUNT_NAME is " + ObjectClass.ACCOUNT_NAME);
            LOG.info("sync ObjectClass is " + objectClass.getObjectClassValue() + "--");
            LOG.ok("fromToken value is " + fromToken);

            GraphEndpoint endpoint = getGraphEndpoint();
            UserProcessing userProcessor = new UserProcessing(getGraphEndpoint(), getSchemaTranslator());
            String selector = userProcessor.getSelectorSingle(oo);
            String nextDeltaLink = new String();
            String tokenValue = (String) fromToken.getValue();
            LOG.ok("Selector value: " +selector);

            LOG.ok("Token and selector pair:" + tokenValue+"&"+selector);

            HttpRequestBase request = new HttpGet(tokenValue+"&"+selector);
            //request.setRetryHandler(new StandardHttpRequestRetryHandler(5, true));
            JSONObject firstCall = endpoint.callRequest(request, true);
            JSONArray value = new JSONArray();
            String nextLink = new String();
            //JSONObject values = new JSONObject();
            boolean morePages = false;
            if (firstCall.has("@odata.nextLink") && firstCall.getString("@odata.nextLink") != null && !firstCall.getString("@odata.nextLink").isEmpty()) {
                morePages = true;
                nextLink = firstCall.getString("@odata.nextLink");
                LOG.ok("nextLink: {0} ; firstCall: {1} ", nextLink, firstCall);
            } else {
                morePages = false;
                LOG.info("No nextLink defined, final page was firstCall");
                nextDeltaLink = firstCall.getString("@odata.deltaLink");
            }
            if (firstCall.has("value") && firstCall.get("value") != null) {
                //value.addAll(firstCall.getJSONArray("value"));
                for (int i = 0; i < firstCall.getJSONArray("value").length(); i++) {
                    value.put(firstCall.getJSONArray("value").get(i));
                }
                LOG.ok("firstCall: {0} ", firstCall);
            } else {
                LOG.info("firstCall contained no value object or the object was null");
            }
            while (morePages == true) {
                JSONObject nextLinkJson = new JSONObject();
                HttpRequestBase nextLinkUriRequest = new HttpGet(nextLink+"&"+selector);
                LOG.ok("nextLinkUriRequest {0}", nextLinkUriRequest);
                nextLinkJson = endpoint.callRequest(nextLinkUriRequest, true);
                if (nextLinkJson.has("@odata.nextLink") && nextLinkJson.getString("@odata.nextLink") != null &&
                        !nextLinkJson.getString("@odata.nextLink").isEmpty()) {

                    morePages = true;
                    nextLink = nextLinkJson.getString("@odata.nextLink");
                    LOG.ok("nextLink: {0} ; nextLinkJson: {1} ", nextLink, nextLinkJson);
                } else {
                    morePages = false;
                    LOG.info("No nextLink defined, final page");
                    nextDeltaLink = nextLinkJson.getString("@odata.deltaLink");
                }
                if (nextLinkJson.has("value") && nextLinkJson.get("value") != null) {
                    //value.addAll(nextLinkJson.getJSONArray("value"));
                    for (int i = 0; i < nextLinkJson.getJSONArray("value").length(); i++) {
                        value.put(nextLinkJson.getJSONArray("value").get(i));
                    }
                    LOG.ok("nextLinkJson: {0} ", nextLinkJson);
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

                String userUID = userProcessor.getUIDIfExists(user);

                LOG.info("Processing user json object, {0}", user);

                ConnectorObjectBuilder userConnectorObjectBuilder;
                SyncDeltaBuilder builder = new SyncDeltaBuilder();
                builder.setObjectClass(ObjectClass.ACCOUNT);

                if (userProcessor.isDeleteDelta(user)){

                    LOG.info("Sync operation: Processing Delete delta for the User: {0} ", userUID);

                    builder.setDeltaType(SyncDeltaType.DELETE);
                    builder.setUid(new Uid(userUID));

                } else {

                    LOG.info("Sync operation: Processing Create or Update delta for the User: {0} ", userUID);
                    if (!userProcessor.isNamePresent(user)){

                        continue;
                    }


                    Set<String> deltableItems = userProcessor.getObjectDeltaItems();
                    AtomicReference<Boolean> fetchedConainsDeltables = new AtomicReference<>(false);
                    deltableItems.forEach(item -> fetchedConainsDeltables.set(user.has(item)));

                    Boolean hasToGetManager = userProcessor.getAttributesToGet(oo).contains("manager.id");
                    if (hasToGetManager && !fetchedConainsDeltables.get()) {

                        userConnectorObjectBuilder = userProcessor.
                                evaluateAndFetchAttributesToGet(new Uid(userUID), oo);
                    } else {

                        userConnectorObjectBuilder = userProcessor.convertUserJSONObjectToConnectorObject(user);
                        if (hasToGetManager) {

                            userProcessor.enhanceConnectorObjectWithDeltaItems(user, userConnectorObjectBuilder);
                        }
                    }

                    builder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
                    builder.setObject(userConnectorObjectBuilder.build());

                }

                builder.setToken(nextLinkSyncToken);

                LOG.ok("Sync operation -> Object handler execution for the User object {0} ", userUID);

                handler.handle(builder.build());

            }


        } else {
            LOG.error("Attribute of type ObjectClass is not supported. Only Account objectclass is supported for SyncOp currently.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported. Only Account objectclass is supported for SyncOp currently.");
        }

    }

    //TODO remove
//    private void evaluateAndFetchAttributesToGet(ConnectorObjectBuilder userConnectorObjectBuilder,
//                                                 ObjectClass oc ,OperationOptions oo) {
//
//        ConnectorObject intermediaryObject = userConnectorObjectBuilder.build();
//
//      Set<String> attrsToGet =   getSchemaTranslator().getAttributesToGet(oc.getDisplayNameKey(), oo);
//
//      if (attrsToGet.contains()) {
//
//      }
//
//    }

    @Override
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        return getLatestSyncToken(objectClass, null);
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

        ResourceQuery translatedQuery= new ResourceQuery();
        Boolean fetchSpecificObject =false;

        if (query == null) {

            LOG.ok("Empty query parameter, returning full list of objects of the object class: {0}"
                    , objectClass.getDisplayNameKey());
        } else {

            if (query instanceof EqualsFilter){

                final EqualsFilter equalsFilter = (EqualsFilter) query;
                        Attribute fAttr = equalsFilter.getAttribute();

                if(Uid.NAME.equals(fAttr.getName()))
                    {
                        LOG.ok("Processing Equals Query based on UID.");

                        fetchSpecificObject = true;
                        translatedQuery.setIdOrMembershipExpression(((Uid) fAttr).getUidValue());

                        LOG.info("Query will fetch specific object with the uid: {0}",
                                translatedQuery.getIdOrMembershipExpression());
                    }
            }

        }

        LOG.info("executeQuery on {0}, filter: {1}, options: {2}", objectClass, query, options);

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            UserProcessing userProcessing = new UserProcessing(getGraphEndpoint(), getSchemaTranslator());

            if(!fetchSpecificObject){

                if(query!=null){

                 translatedQuery = query.accept(new FilterHandler(), new ResourceQuery(objectClass,
                        userProcessing.getUIDAttribute(), userProcessing.getNameAttribute()));

                }
            }

            if (LOG.isOk()) {
                LOG.ok("Query will be executed with the following filter: {0}", translatedQuery.toString() != null ?
                        translatedQuery : translatedQuery.getIdOrMembershipExpression());
                LOG.ok("The object class for which the filter will be executed: {0}", objectClass.getDisplayNameKey());
            }

            userProcessing.executeQueryForUser(translatedQuery, fetchSpecificObject ,handler, options);

        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {

            GroupProcessing groupProcessing = new GroupProcessing(getGraphEndpoint());

            if(!fetchSpecificObject){

                if(query!=null){

                    translatedQuery = query.accept(new FilterHandler(), new ResourceQuery(objectClass,
                            groupProcessing.getUIDAttribute(), groupProcessing.getNameAttribute()));
                }
            }

            if (LOG.isOk()) {
                LOG.ok("Query will be executed with the following filter: {0}", translatedQuery);
                LOG.ok("The object class for which the filter will be executed: {0}", objectClass.getDisplayNameKey());
            }

            groupProcessing.executeQueryForGroup(translatedQuery, fetchSpecificObject, handler, options);

        } else if (objectClass.is(LicenseProcessing.OBJECT_CLASS_NAME)) {
            LicenseProcessing licenseProcessing = new LicenseProcessing(getGraphEndpoint(), getSchemaTranslator());

            // TODO TEST only currently
//            if(!fetchSpecificObject){
//
//                if(query!=null){
//
//                    translatedQuery = query.accept(new FilterHandler(), new ResourceQuery(objectClass,
//                            licenseProcessing.getUIDAttribute(), licenseProcessing.getNameAttribute()));
//                }
//            }

            licenseProcessing.executeQueryForLicense(query, handler, options);

        } else if (objectClass.is(RoleProcessing.ROLE_NAME)) {
            RoleProcessing roleProcessing = new RoleProcessing(getGraphEndpoint());

            // TODO TEST only currently
//            if(!fetchSpecificObject){
//
//                if(query!=null){
//
//                    translatedQuery  = query.accept(new FilterHandler(), new ResourceQuery(objectClass,
//                            roleProcessing.getUIDAttribute(), roleProcessing.getNameAttribute()));
//                }
//            }

            roleProcessing.executeQueryForRole(query, handler, options);

        } else {
            LOG.warn("Provided ObjectClass is not supported, trying generic support");
            GenericListItemProcessing genericProcessing = new GenericListItemProcessing(getGraphEndpoint());

            if (query != null) {
                LOG.ok("Query will be executed with the following filter: {0}", query.toString());
                LOG.ok("The object class for which the filter will be executed: {0}", objectClass.getDisplayNameKey());
            }
            genericProcessing.executeQueryForListRecords(objectClass, query, handler, options);
        }
    }

    @Override
    public void test() {
        final GraphEndpoint endpoint = getGraphEndpoint();
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
        endpoint.callRequest(request, false);
    }

    @Override
    public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> attrsDelta, OperationOptions options) {
        validateObjectClassAndUID(objectClass, uid);

        if (attrsDelta == null || attrsDelta.isEmpty()) {
            LOG.error("Parameter of type Set<AttributeDelta> not provided or empty.");
            throw new InvalidAttributeValueException("Parameter of type Set<AttributeDelta> not provided or empty.");
        }

        if (options == null) {
            LOG.error("Parameter of type OperationOptions not provided.");
            throw new InvalidAttributeValueException("Parameter of type OperationOptions not provided.");
        }
        LOG.info("UpdateDelta with ObjectClass: {0} , uid: {1} , attrsDelta: {2} , options: {3}  ", objectClass, uid, attrsDelta, options);

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
            UserProcessing userProcessing = new UserProcessing(getGraphEndpoint(), getSchemaTranslator());
            return userProcessing.updateUser(uid, attrsDelta, options);

        } else if (objectClass.is(ObjectClass.GROUP_NAME)) { // __GROUP__
            GroupProcessing groupProcessing = new GroupProcessing(getGraphEndpoint());
            return groupProcessing.updateGroup(uid, attrsDelta, options);

        } else if (objectClass.is(RoleProcessing.ROLE_NAME)) { // __ROLE__
            RoleProcessing roleProcessing = new RoleProcessing(getGraphEndpoint());
            return roleProcessing.updateRole(uid, attrsDelta, options);

        } else {
            LOG.error("The value of the ObjectClass parameter is unsupported.");
            throw new UnsupportedOperationException("The value of the ObjectClass parameter is unsupported.");
        }
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

    @Override
    public void checkAlive() {
        // do nothing here
    }

    @Override
    public void testPartialConfiguration() {

        LOG.info("Starting graph endpoint instance as part of Partial configuration test while leveraging default" +
                " Java truststore in case of environment trustStore returns with exception");
        graphEndpoint = new GraphEndpoint(configuration, true);
        LOG.info("Execution of default test method");
        test();
    }

    @Override
    public Map<String, SuggestedValues> discoverConfiguration() {

            Map<String, SuggestedValues> suggestions = new HashMap<>();

            Boolean connectionFailed = false;
            try {

            configuration.setValidateWithFailoverTrust(false);
            graphEndpoint = new GraphEndpoint(configuration);
             } catch (ConnectionFailedException e){

                configuration.setValidateWithFailoverTrust(true);
                connectionFailed = true;
            }

            if (connectionFailed){

                LOG.ok("Setting up discovery suggestion to use validation with native trust store");
                suggestions.put("validateWithFailoverTrust", SuggestedValuesBuilder.build(true));
            } else {

                LOG.ok("Setting up discovery suggestion to not use validation with native trust store");
                suggestions.put("validateWithFailoverTrust", SuggestedValuesBuilder.build(false));
            }

            String pageSize = configuration.getPageSize();
            String throttlingRetryWait = configuration.getThrottlingRetryWait();
            String pathToFailoverTrustStore  = configuration.getPathToFailoverTrustStore();
            Integer throttlingRetryCount = configuration.getThrottlingRetryCount();



            if(pageSize !=null && !pageSize.isEmpty()){

                suggestions.put("pageSize", SuggestedValuesBuilder.buildOpen(pageSize));
            } else {

                // Default for page size
                suggestions.put("pageSize", SuggestedValuesBuilder.buildOpen("100"));
            }

        if(throttlingRetryWait!=null && !throttlingRetryWait.isEmpty()){

            suggestions.put("throttlingRetryWait", SuggestedValuesBuilder.buildOpen(throttlingRetryWait));
        } else {

            suggestions.put("throttlingRetryWait", SuggestedValuesBuilder.buildOpen("10"));
        }

        if(throttlingRetryCount != null){

            suggestions.put("throttlingRetryCount", SuggestedValuesBuilder.buildOpen(throttlingRetryCount));
        } else {

            suggestions.put("throttlingRetryCount", SuggestedValuesBuilder.buildOpen(3));
        }

        if(pathToFailoverTrustStore !=null && !pathToFailoverTrustStore.isEmpty()){

            suggestions.put("pathToFailoverTrustStore", SuggestedValuesBuilder.buildOpen(pathToFailoverTrustStore));
        }

        Set<String> availablePlans = fetchAvailablePlans();

        if (availablePlans !=null && !availablePlans.isEmpty()){

            suggestions.put("disabledPlans", SuggestedValuesBuilder.build(availablePlans));
        }

        return suggestions;
    }

    public Set<String> fetchAvailablePlans() {

        LOG.ok("Fetching available license plans");
        LicenseProcessing licenseProcessing = new LicenseProcessing(getGraphEndpoint(), getSchemaTranslator());
        List<JSONObject> licenses = licenseProcessing.list();
        Set<String> parsedPlans = CollectionUtil.newSet();

        for (JSONObject licence : licenses) {

            if (licence != null) {

                try {

                    String skuId = licence.getString("skuId");
                    String skuPartNumber = licence.getString("skuPartNumber");

                    JSONArray planList = licence.getJSONArray("servicePlans");
                    int length = planList.length();
                    LOG.ok("Length of service plans list: {0}", length);

                    for (int i = 0; i < length; i++) {
                        JSONObject obj = planList.getJSONObject(i);
                       String serviceName = (String) obj.get("servicePlanName");
                        String servicePlanId = (String) obj.get("servicePlanId");

                        parsedPlans.add(skuPartNumber+":"+serviceName+" "+"["+skuId+":"+servicePlanId+"]");
                    }

                } catch (JSONException e) {

                    LOG.warn("Exception while fetching available service plans while analyzing licences: {0} ", e.getLocalizedMessage());
                }
            }

        }
        return parsedPlans;
    }
}
