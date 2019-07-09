package com.evolveum.polygon.connector.msgraphapi;

import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GroupProcessing extends ObjectProcessing {

    private final static String GROUPS = "/groups";

    private static final String ATTR_ALLOWEXTERNALSENDERS = "allowExternalSenders";
    private static final String ATTR_AUTOSUBSCRIBENEWMEMBERS = "autoSubscribeNewMembers";
    private static final String ATTR_CLASSIFICATION = "classification";
    private static final String ATTR_CREATEDDATETIME = "createdDateTime";
    private static final String ATTR_DESCRIPTION = "description";
    private static final String ATTR_DISPLAYNAME = "displayName";
    private static final String ATTR_GROUPTYPES = "groupTypes";
    private static final String ATTR_ID = "id";
    private static final String ATTR_ISSUBSCRIBEDBYMAIL = "isSubscribedByMail";
    private static final String ATTR_MAIL = "mail";
    private static final String ATTR_MAILENABLED = "mailEnabled";
    private static final String ATTR_MAILNICKNAME = "mailNickname";
    private static final String ATTR_ONPREMISESLASTSYNCDATETIME = "onPremisesLastSyncDateTime";
    private static final String ATTR_ONPREMISESSECURITYIDENTIFIER = "onPremisesSecurityIdentifier";
    private static final String ATTR_ONPREMISESSYNCENABLED = "onPremisesSyncEnabled";
    private static final String ATTR_PROXYADDRESSES = "proxyAddresses";
    private static final String ATTR_SECURITYENABLED = "securityEnabled";
    private static final String ATTR_UNSEENCOUNT = "unseenCount";
    private static final String ATTR_VISIBILITY = "visibility";
    private static final String ATTR_MEMBERS = "members";
    private static final String ATTR_OWNERS = "owners";

    public GroupProcessing(MSGraphConfiguration configuration, MSGraphConnector connector) {
        super(configuration, ICFPostMapper.builder().build());
    }


    public void buildGroupObjectClass(SchemaBuilder schemaBuilder) {
        schemaBuilder.defineObjectClass(objectClassInfo());
    }

    @Override
    protected ObjectClassInfo objectClassInfo() {
        ObjectClassInfoBuilder groupObjClassBuilder = new ObjectClassInfoBuilder();

        groupObjClassBuilder.setType(ObjectClass.GROUP_NAME);

        //required

        //Supports $filter and $orderby
        AttributeInfoBuilder attrDisplayName = new AttributeInfoBuilder(ATTR_DISPLAYNAME);
        attrDisplayName.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrDisplayName.build());

        AttributeInfoBuilder attrMailEnabled = new AttributeInfoBuilder(ATTR_MAILENABLED);
        attrMailEnabled.setRequired(true).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrMailEnabled.build());

        //Supports $filter
        AttributeInfoBuilder attrMailNickname = new AttributeInfoBuilder(ATTR_MAILNICKNAME);
        attrMailNickname.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrMailNickname.build());

        //$filter
        AttributeInfoBuilder attrSecurityEnabled = new AttributeInfoBuilder(ATTR_SECURITYENABLED);
        attrSecurityEnabled.setRequired(true).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrSecurityEnabled.build());


        //optional

        AttributeInfoBuilder attrAllowExternalSenders = new AttributeInfoBuilder(ATTR_ALLOWEXTERNALSENDERS);
        attrAllowExternalSenders.setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrAllowExternalSenders.build());

        AttributeInfoBuilder attrAutoSubscribeNewMembers = new AttributeInfoBuilder(ATTR_AUTOSUBSCRIBENEWMEMBERS);
        attrAutoSubscribeNewMembers.setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrAutoSubscribeNewMembers.build());

        AttributeInfoBuilder attrClassification = new AttributeInfoBuilder(ATTR_CLASSIFICATION);
        attrClassification.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrClassification.build());

        AttributeInfoBuilder attrCreatedDateTime = new AttributeInfoBuilder(ATTR_CREATEDDATETIME);
        attrCreatedDateTime.setRequired(false)
                //.setType(Date.class)
                .setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrCreatedDateTime.build());

        AttributeInfoBuilder attrDescription = new AttributeInfoBuilder(ATTR_DESCRIPTION);
        attrDescription.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrDescription.build());

        //Supports $filter
        AttributeInfoBuilder attrGroupTypes = new AttributeInfoBuilder(ATTR_GROUPTYPES);
        attrGroupTypes.setRequired(false).setType(String.class).setMultiValued(true).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrGroupTypes.build());

        // Not nullable, Read-only.
        AttributeInfoBuilder attrId = new AttributeInfoBuilder(ATTR_ID);
        attrId.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrId.build());

        AttributeInfoBuilder attrIsSubscribedByMail = new AttributeInfoBuilder(ATTR_ISSUBSCRIBEDBYMAIL);
        attrIsSubscribedByMail.setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrIsSubscribedByMail.build());

        //Read-only, supports $filter.
        AttributeInfoBuilder attrMail = new AttributeInfoBuilder(ATTR_MAIL);
        attrMail.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrMail.build());

        //Read-only, supports $filter.
        AttributeInfoBuilder attrOnPremisesLastSyncDateTime = new AttributeInfoBuilder(ATTR_ONPREMISESLASTSYNCDATETIME);
        attrOnPremisesLastSyncDateTime.setRequired(false)
                //.setType(Date.class)
                .setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOnPremisesLastSyncDateTime.build());

        //read-only
        AttributeInfoBuilder attrOnPremisesSecurityIdentifier = new AttributeInfoBuilder(ATTR_ONPREMISESSECURITYIDENTIFIER);
        attrOnPremisesSecurityIdentifier.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOnPremisesSecurityIdentifier.build());

        //Read-only, Supports $filter.
        AttributeInfoBuilder attrOnPremisesSyncEnabled = new AttributeInfoBuilder(ATTR_ONPREMISESSYNCENABLED);
        attrOnPremisesSyncEnabled.setRequired(false).setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOnPremisesSyncEnabled.build());

        //Read-only, Not nullable, Supports $filter, multivalued
        AttributeInfoBuilder attrProxyAddresses = new AttributeInfoBuilder(ATTR_PROXYADDRESSES);
        attrProxyAddresses.setRequired(false).setType(String.class).setMultiValued(true).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrProxyAddresses.build());

        AttributeInfoBuilder attrUnseenCount = new AttributeInfoBuilder(ATTR_UNSEENCOUNT);
        attrUnseenCount.setRequired(false).setType(Integer.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrUnseenCount.build());

        AttributeInfoBuilder attrVisibility = new AttributeInfoBuilder(ATTR_VISIBILITY);
        attrVisibility.setRequired(false).setType(Integer.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrVisibility.build());

        AttributeInfoBuilder attrMembers = new AttributeInfoBuilder(ATTR_MEMBERS);
        attrVisibility.setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(true);
        groupObjClassBuilder.addAttributeInfo(attrMembers.build());

        AttributeInfoBuilder attrOwners = new AttributeInfoBuilder(ATTR_OWNERS);
        attrVisibility.setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(true);
        groupObjClassBuilder.addAttributeInfo(attrOwners.build());


        return groupObjClassBuilder.build();
    }

    protected Uid createOrUpdateGroup(Uid uid, Set<Attribute> attributes) {
        LOG.info("Start createOrUpdateGroup, Uid: {0}, attributes: {1}", uid, attributes);


        if (attributes == null || attributes.isEmpty()) {
            throw new InvalidAttributeValueException("attributes not provided or empty");
        }

        Boolean create = uid == null;
        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final URIBuilder uriBuilder = endpoint.createURIBuilder();

        HttpEntityEnclosingRequestBase request = null;
        URI uri = null;
        JSONObject jsonAnswer = new JSONObject();

        if (create) {
            int mandatoryAttributes = 0;
            for (Attribute attribute : attributes) {
                if ((attribute.getName().equals("displayName") ||
                        attribute.getName().equals("mailEnabled") ||
                        attribute.getName().equals("mailNickname") ||
                        attribute.getName().equals("securityEnabled")) &&
                        !attribute.getValue().isEmpty()) {
                    mandatoryAttributes++;
                }
            }
            if (mandatoryAttributes < 4) {
                throw new InvalidAttributeValueException();
            }
            uriBuilder.setPath(GROUPS);
            uri = endpoint.getUri(uriBuilder);
            LOG.info("Uid == null -> create group");
            LOG.info("Path: {0}", uri);
            request = new HttpPost(uri);
            JSONObject jsonObject1 = buildLayeredAttributeJSON(attributes);
            jsonAnswer = endpoint.callRequest(request, jsonObject1, true);


        } else {
            // update
            uriBuilder.setPath(GROUPS + "/" + uid.getUidValue());
            uri = endpoint.getUri(uriBuilder);
            LOG.info("Uid != null -> update group");
            LOG.info("Path: {0}", uri);
            request = new HttpPatch(uri);
            List<Object> attributeList = buildLayeredAtrribute(attributes);
            endpoint.callRequestNoContentNoJson(request, attributeList);
        }


        // JSONObject jsonRequest = callRequest(request, attributes);

        if (!create) {
            LOG.info("Returning original Uid: {0} ", uid);
            return uid;
        }
        String newUid = jsonAnswer.getString("id");
        LOG.info("The new Uid is {0} ", newUid);


        return new Uid(newUid);
    }

    protected void delete(Uid uid) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }
        HttpDelete request;
        URI uri = null;

        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final URIBuilder uriBuilder = endpoint.createURIBuilder();
        uriBuilder.setPath(GROUPS + "/" + uid.getUidValue());
        uri = endpoint.getUri(uriBuilder);
        LOG.info("Path: {0}", uri);
        request = new HttpDelete(uri);
        if (endpoint.callRequest(request, false) == null) {
            LOG.info("Deleted group with Uid {0}", uid.getUidValue());
        }

    }


    protected void updateDeltaMultiValuesForGroup(Uid uid, Set<AttributeDelta> attributesDelta, OperationOptions options) {
        LOG.info("updateDeltaMultiValuesForGroup on uid: {0}, attrDelta: {1}, options: {2}", uid.getValue(), attributesDelta, options);
        for (AttributeDelta attrDelta : attributesDelta) {
            LOG.info("attrDelta: {0}", attrDelta);
            //add or remove owners to/from group
            if (attrDelta.getName().equalsIgnoreCase("owners")) {
                LOG.info("addOwnersToGroup");
                addOrRemoveOwner(uid, attrDelta, GROUPS);
            }//add or remove members to/from group
            else if (attrDelta.getName().equalsIgnoreCase("members")) {
                LOG.info("addMembersToGroup");
                addOrRemoveMember(uid, attrDelta, GROUPS);
            }
        }

    }


    protected void addOrRemoveMember(Uid uid, AttributeDelta attrDelta, String path) {
        LOG.info("addOrRemoveMember {0} , {1} , {2}", uid, attrDelta, path);
        StringBuilder sbPath = new StringBuilder();
        sbPath.append(path).append("/").append(uid.getUidValue()).append("/" + ATTR_MEMBERS);

        LOG.info("path: {0}", sbPath);

        List<Object> addValues = attrDelta.getValuesToAdd();
        List<Object> removeValues = attrDelta.getValuesToRemove();

        if (addValues != null && !addValues.isEmpty()) {
            sbPath.append("/").append("$ref");
            for (Object addValue : addValues) {
                if (addValue != null) {

                    JSONObject json = new JSONObject();
                    String userID = (String) addValue;

                    String addToJson = "https://graph.microsoft.com/v1.0/directoryObjects/" + userID;
                    //POST https://graph.microsoft.com/v1.0/groups/{id}/members/$ref
                    //json.put(ATTR_ID, userID);
                    json.put("@odata.id", addToJson);
                    LOG.ok("json: {0}", json.toString());
                    postRequestNoContent(sbPath.toString(), json);
                }
            }
        }

        groupProcessRemove(sbPath, removeValues);
    }

    protected void addToGroup(Uid uid, Set<Attribute> attributes) {
        LOG.info("addToGroup {0} , {1}", uid, attributes);

        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase("member") ||
                    attribute.getName().equalsIgnoreCase("members")) {
                addMembersToGroup(uid, attribute);
            } else if (attribute.getName().equalsIgnoreCase("owner") ||
                    attribute.getName().equalsIgnoreCase("owners")) {
                addOwnerToGroup(uid, attribute);
            }
        }
    }


    private void addMembersToGroup(Uid uid, Attribute attribute) {
        LOG.info("addMembersToGroup {0} , {1}", uid, attribute);

        StringBuilder sbPath = new StringBuilder();
        sbPath.append(GROUPS).append("/").append(uid.getUidValue()).append("/" + ATTR_MEMBERS);
        sbPath.append("/").append("$ref");
        LOG.info("path: {0}", sbPath);

        JSONObject json = new JSONObject();
        String addToJson = "https://graph.microsoft.com/v1.0/directoryObjects/" + AttributeUtil.getAsStringValue(attribute);
        json.put("@odata.id", addToJson);
        LOG.ok("json: {0}", json.toString());
        postRequestNoContent(sbPath.toString(), json);


    }

    private void addOwnerToGroup(Uid uid, Attribute attribute) {
        LOG.info("addOwnerToGroup {0} , {1}", uid, attribute);

        StringBuilder sbPath = new StringBuilder();
        sbPath.append(GROUPS).append("/").append(uid.getUidValue()).append("/" + ATTR_OWNERS);
        sbPath.append("/").append("$ref");
        LOG.info("path: {0}", sbPath);
        JSONObject json = new JSONObject();
        String addToJson = "https://graph.microsoft.com/v1.0/users/" + AttributeUtil.getAsStringValue(attribute);
        json.put("@odata.id", addToJson);
        LOG.ok("json: {0}", json.toString());
        postRequestNoContent(sbPath.toString(), json);


    }

    protected void removeFromGroup(Uid uid, Set<Attribute> attributes) {
        LOG.info("removeFromGroup {0}, {1}", uid, attributes);
        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase("member") ||
                    attribute.getName().equalsIgnoreCase("members")) {
                removeMemberFromGroup(uid, attribute);
            } else if (attribute.getName().equalsIgnoreCase("owner") ||
                    attribute.getName().equalsIgnoreCase("owners")) {
                removeOwnerFromGroup(uid, attribute);
            }
        }
    }

    private void removeOwnerFromGroup(Uid uid, Attribute attribute) {
        LOG.info("addOrRemoveMember {0} , {1} ", uid, attribute);
        StringBuilder sbPath = new StringBuilder();
        sbPath.append(GROUPS).append("/").append(uid.getUidValue()).append("/" + ATTR_OWNERS);
        LOG.info("executeDeleteOperation userId: {0} , sbPath.toString: {1} ", AttributeUtil.getAsStringValue(attribute), sbPath.toString());
        executeDeleteOperation(new Uid(AttributeUtil.getAsStringValue(attribute)), sbPath.toString());
        LOG.info("path: {0}", sbPath);
    }

    private void removeMemberFromGroup(Uid uid, Attribute attribute) {
        LOG.info("addOrRemoveMember {0} , {1} ", uid, attribute);
        StringBuilder sbPath = new StringBuilder();
        sbPath.append(GROUPS).append("/").append(uid.getUidValue()).append("/" + ATTR_MEMBERS);
        LOG.info("executeDeleteOperation userId: {0} , sbPath.toString: {1} ", AttributeUtil.getAsStringValue(attribute), sbPath.toString());
        executeDeleteOperation(new Uid(AttributeUtil.getAsStringValue(attribute)), sbPath.toString());
        LOG.info("path: {0}", sbPath);
    }

    public void addOrRemoveOwner(Uid uid, AttributeDelta attrDelta, String path) {
        LOG.info("add owner to group or remove ");
        StringBuilder sbPath = new StringBuilder();
        sbPath.append(path).append("/").append(uid.getUidValue()).append("/" + ATTR_OWNERS);


        List<Object> addValues = attrDelta.getValuesToAdd();
        List<Object> removeValues = attrDelta.getValuesToRemove();

        if (addValues != null && !addValues.isEmpty()) {
            sbPath.append("/").append("$ref");
            for (Object addValue : addValues) {
                if (addValue != null) {

                    JSONObject json = new JSONObject();
                    String userID = (String) addValue;
                    //"@odata.id": "https://graph.microsoft.com/v1.0/users/{id}"
                    String addToJson = "https://graph.microsoft.com/v1.0/users/" + userID;
                    //POST https://graph.microsoft.com/v1.0/groups/{id}/owners/$ref
                    //json.put(ATTR_ID, userID);
                    json.put("@odata.id", addToJson);
                    LOG.ok("json: {0}", json.toString());
                    postRequestNoContent(sbPath.toString(), json);
                }
            }
        }
        LOG.info("sbPath : {0} ; removeValues {1} ", sbPath, removeValues);
        groupProcessRemove(sbPath, removeValues);
    }

    private void postRequestNoContent(String path, JSONObject json) {
        LOG.info("path: {0} , json {1}, ", path, json);
        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(path);
        final URI uri = endpoint.getUri(uriBuilder);
        LOG.info("uri {0}", uri);


        HttpEntityEnclosingRequestBase request;
        LOG.info("HttpEntityEnclosingRequestBase request");

        request = new HttpPost(uri);
        LOG.info("create true - HTTP POST {0}", uri);
        endpoint.callRequestNoContent(request, null, json);
    }


    private void groupProcessRemove(StringBuilder sbPath, List<Object> removeValues) {
        if (removeValues != null && !removeValues.isEmpty()) {
            for (Object removeValue : removeValues) {
                if (removeValue != null) {

                    String userID = (String) removeValue;
                    LOG.info("executeDeleteOperation userId: {0} , sbPath.toString: {1} ", userID, sbPath.toString());
                    executeDeleteOperation(new Uid(userID), sbPath.toString());
                }
            }
        }
    }

    private void executeDeleteOperation(Uid uid, String path) {
        LOG.info("Delete object, Uid: {0}, Path: {1}", uid, path);

        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        final URIBuilder uriBuilder = endpoint.createURIBuilder();
        uriBuilder.setPath(path + "/" + uid.getUidValue() + "/$ref");

        LOG.info("Uri for delete: {0}", uriBuilder);

        final URI uri = endpoint.getUri(uriBuilder);
        final HttpRequestBase request = new HttpDelete(uri);
        endpoint.callRequest(request, false);
    }


    public void executeQueryForGroup(Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQueryForGroup()");
        final GraphEndpoint endpoint = new GraphEndpoint(getConfiguration());
        if (query instanceof EqualsFilter) {
            LOG.info("query instanceof EqualsFilter");
            if (((EqualsFilter) query).getAttribute() instanceof Uid) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof Uid");

                Uid uid = (Uid) ((EqualsFilter) query).getAttribute();
                if (uid.getUidValue() == null) {
                    invalidAttributeValue("Uid", query);
                }
                StringBuilder sbPath = new StringBuilder();

                //get information about group
                sbPath.append(GROUPS).append("/").append(uid.getUidValue());
                LOG.info("sbPath: {0}", sbPath);
                JSONObject group = endpoint.executeGetRequest(sbPath.toString(), null, options, false);
                LOG.info("JSONObject group {0}", group.toString());
                processingGroupObjectFromGET(group, handler);

                //get list of group members
                StringBuilder sbPathForMembers = new StringBuilder();
                sbPathForMembers.append(GROUPS).append("/").append(uid.getUidValue()).append("/").append("members");
                JSONObject groupMembers = endpoint.executeGetRequest(sbPathForMembers.toString(), null, options, true);
                LOG.info("JSONObject group {0}", groupMembers.toString());
                processingMultipleObjectFromGET(groupMembers, handler);

                //get list of group owners
                StringBuilder sbPathForOwners = new StringBuilder();
                sbPathForOwners.append(GROUPS).append("/").append(uid.getUidValue()).append("/").append("members");
                JSONObject groupOwners = endpoint.executeGetRequest(sbPathForOwners.toString(), null, options, true);
                LOG.info("JSONObject group {0}", groupOwners.toString());
                processingMultipleObjectFromGET(groupOwners, handler);


            } else if (((EqualsFilter) query).getAttribute().getName().equals(ATTR_DISPLAYNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_DISPLAYNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + ATTR_DISPLAYNAME + " eq '" + attributeValue + "'";
                JSONObject groups = endpoint.executeGetRequest(GROUPS, customQuery, options, true);
                LOG.info("JSONObject users {0}", groups.toString());
                processingMultipleObjectFromGET(groups, handler);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(ATTR_MAILNICKNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof mailNickname");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_MAILNICKNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + ATTR_MAILNICKNAME + " eq '" + attributeValue + "'";
                JSONObject groups = endpoint.executeGetRequest(GROUPS, customQuery, options, true);
                LOG.info("JSONObject users {0}", groups.toString());
                processingMultipleObjectFromGET(groups, handler);
            }
        } else if (query instanceof ContainsFilter) {
            if (((ContainsFilter) query).getAttribute().getName().equals(ATTR_DISPLAYNAME)) {
                LOG.info("((ContainsFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((ContainsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_DISPLAYNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + STARTSWITH + "(" + ATTR_DISPLAYNAME + ",'" + attributeValue + "')";
                JSONObject groups = endpoint.executeGetRequest(GROUPS, customQuery, options, true);
                LOG.info("JSONObject users {0}", groups.toString());
                processingMultipleObjectFromGET(groups, handler);
            } else if (((ContainsFilter) query).getAttribute().getName().equals(ATTR_MAIL)) {
                LOG.info("((ContainsFilter) query).getAttribute() instanceof mail");

                List<Object> allValues = ((ContainsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_MAIL, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + STARTSWITH + "(" + ATTR_MAIL + ",'" + attributeValue + "')";
                JSONObject groups = endpoint.executeGetRequest(GROUPS, customQuery, options, true);
                LOG.info("JSONObject users {0}", groups.toString());
                processingMultipleObjectFromGET(groups, handler);
            } else if (((ContainsFilter) query).getAttribute().getName().equals(ATTR_MAILNICKNAME)) {
                LOG.info("((ContainsFilter) query).getAttribute() instanceof mailNickName");

                List<Object> allValues = ((ContainsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue(ATTR_MAILNICKNAME, query);
                }

                String attributeValue = allValues.get(0).toString();
                LOG.info("value {0}", attributeValue);
                String customQuery = "$filter=" + STARTSWITH + "(" + ATTR_MAILNICKNAME + ",'" + attributeValue + "')";
                JSONObject groups = endpoint.executeGetRequest(GROUPS, customQuery, options, true);
                LOG.info("JSONObject users {0}", groups.toString());
                processingMultipleObjectFromGET(groups, handler);
            }

        } else if (query == null) {
            LOG.info("query==null");
            JSONObject groups = endpoint.executeGetRequest(GROUPS, null, options, true);
            //  LOG.info("JSONObject group {0}", groups.toString());
            processingMultipleObjectFromGET(groups, handler);

        }
    }

    private void processingGroupObjectFromGET(JSONObject group, ResultsHandler handler) {
        LOG.info("processingObjectFromGET (Object)");
        ConnectorObjectBuilder builder = convertGroupJSONObjectToConnectorObject(group);
        ConnectorObject connectorObject = builder.build();
        LOG.info("processingGroupObjectFromGET, group: {0}, \n\tconnectorObject: {1}", group.get("id"), connectorObject.toString());
        handler.handle(connectorObject);
    }


    private void processingMultipleObjectFromGET(JSONObject group, ResultsHandler handler) {
        LOG.info("processingMultipleObjectFromGET ");
        if (group == null) return;
        String jsonStr = group.toString();
        JSONObject jsonObj = new JSONObject(jsonStr);

        JSONArray value;
        try {
            value = jsonObj.getJSONArray("value");
        } catch (JSONException e) {
            LOG.info("not find anything");
            return;
        }
        int length = value.length();
        LOG.info("jsonObj length: {0}", length);

        for (int i = 0; i < length; i++) {
            JSONObject user = value.getJSONObject(i);
            processingGroupObjectFromGET(user, handler);

        }
    }

    private ConnectorObjectBuilder convertGroupJSONObjectToConnectorObject(JSONObject group) {
        LOG.info("convertGroupJSONObjectToConnectorObject");
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.GROUP);

        getUIDIfExists(group, ATTR_ID, builder);

        getNAMEIfExists(group, ATTR_DISPLAYNAME, builder);

        getIfExists(group, ATTR_DESCRIPTION, String.class, builder);
        getMultiIfExists(group, ATTR_GROUPTYPES, builder); //?
        getIfExists(group, ATTR_MAIL, String.class, builder);
        getIfExists(group, ATTR_MAILENABLED, Boolean.class, builder);
        getIfExists(group, ATTR_MAILNICKNAME, String.class, builder);
        getIfExists(group, ATTR_ONPREMISESLASTSYNCDATETIME, String.class, builder);
        getIfExists(group, ATTR_ONPREMISESSECURITYIDENTIFIER, String.class, builder);
        getIfExists(group, ATTR_ONPREMISESSYNCENABLED, Boolean.class, builder);
        getMultiIfExists(group, ATTR_PROXYADDRESSES, builder);
        getIfExists(group, ATTR_SECURITYENABLED, Boolean.class, builder);
        getIfExists(group, ATTR_VISIBILITY, String.class, builder);
        getIfExists(group, ATTR_CREATEDDATETIME, String.class, builder);
        getIfExists(group, ATTR_CLASSIFICATION, String.class, builder);


        getIfExists(group, ATTR_ALLOWEXTERNALSENDERS, Boolean.class, builder);
        getIfExists(group, ATTR_AUTOSUBSCRIBENEWMEMBERS, Boolean.class, builder);
        getIfExists(group, ATTR_ISSUBSCRIBEDBYMAIL, Boolean.class, builder);
        getIfExists(group, ATTR_UNSEENCOUNT, Integer.class, builder);


        return builder;
    }


}
