package com.evolveum.polygon.connector.msgraphapi;

import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.*;

public class RoleProcessing extends ObjectProcessing {
    public static final String ROLE_NAME = "Role";
    public static final ObjectClass ROLE = new ObjectClass(ROLE_NAME);

    private final static String ROLES = "/roleManagement/directory/roleDefinitions";
    private final static String ROLE_ASSIGNMENT = "/roleManagement/directory/roleAssignments";
    private final static String USERS = "/users";

    private static final String ATTR_ID = "id";
    private static final String ATTR_DESCRIPTION = "description";
    private static final String ATTR_DISPLAY_NAME = "displayName";
    private static final String ATTR_IS_BUILT_IN = "isBuiltIn";
    private static final String ATTR_IS_ENABLED = "isEnabled";
    private static final String ATTR_RESOURCE_SCOPES = "resourceScopes";
    private static final String ATTR_TEMPLATE_ID = "templateId";
    private static final String ATTR_VERSION = "version";
    private static final String ATTR_ROLE_PERMISSIONS = "rolePermissions";
    private static final String ATTR_ALLOWED_RESOURCE_ACTIONS = "allowedResourceActions";
    private static final String ATTR_ROLE_PERMISSIONS_ALL = ATTR_ROLE_PERMISSIONS + "." + ATTR_ALLOWED_RESOURCE_ACTIONS;
    private static final String ATTR_INHERIT_PERMISSIONS_FROM_ODATA_CONTEXT = "inheritsPermissionsFrom@odata.context";
    private static final String ATTR_INHERIT_PERMISSIONS_FROM = "inheritsPermissionsFrom";
    private static final String ATTR_INHERIT_PERMISSIONS_FROM_ALL = ATTR_INHERIT_PERMISSIONS_FROM + "." + ATTR_ID;


    private static final String ATTR_MEMBERS = "members";

    public RoleProcessing(GraphEndpoint graphEndpoint) {
        super(graphEndpoint, ICFPostMapper.builder().build());
    }


    public void buildRoleObjectClass(SchemaBuilder schemaBuilder) {
        schemaBuilder.defineObjectClass(objectClassInfo());
    }

    @Override
    protected ObjectClassInfo objectClassInfo() {
        ObjectClassInfoBuilder roleObjClassBuilder = new ObjectClassInfoBuilder();

        roleObjClassBuilder.setType(RoleProcessing.ROLE_NAME);

        // required attribute is icfs:name and icfs:uid

        // optional

        // read-only
        AttributeInfoBuilder attrIsEnabled = new AttributeInfoBuilder(ATTR_IS_ENABLED);
        attrIsEnabled.setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrIsEnabled.build());

        // read-only
        AttributeInfoBuilder attrIsBuiltIn = new AttributeInfoBuilder(ATTR_IS_BUILT_IN);
        attrIsBuiltIn.setRequired(false).setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrIsBuiltIn.build());

        // read-only
        AttributeInfoBuilder attrTemplateId = new AttributeInfoBuilder(ATTR_TEMPLATE_ID);
        attrTemplateId.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrTemplateId.build());

        // read-only
        AttributeInfoBuilder attrVersion = new AttributeInfoBuilder(ATTR_VERSION);
        attrVersion.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrVersion.build());

        // read-only
        AttributeInfoBuilder attrDescription = new AttributeInfoBuilder(ATTR_DESCRIPTION);
        attrDescription.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrDescription.build());

        // multivalued, read-only
        AttributeInfoBuilder attrResourceScopes = new AttributeInfoBuilder(ATTR_RESOURCE_SCOPES);
        attrResourceScopes.setRequired(false).setType(String.class).setMultiValued(true).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrResourceScopes.build());

        // multivalued, read-only
        AttributeInfoBuilder attrRolePermissionsAll = new AttributeInfoBuilder(ATTR_ROLE_PERMISSIONS_ALL);
        attrRolePermissionsAll.setRequired(false).setType(String.class).setMultiValued(true).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrRolePermissionsAll.build());

        // read-only
        AttributeInfoBuilder attrInheritPermissionsFromDataContext = new AttributeInfoBuilder(ATTR_INHERIT_PERMISSIONS_FROM_ODATA_CONTEXT);
        attrInheritPermissionsFromDataContext.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrInheritPermissionsFromDataContext.build());

        // multivalued, read-only
        AttributeInfoBuilder attrInheritPermissionsFromAll = new AttributeInfoBuilder(ATTR_INHERIT_PERMISSIONS_FROM_ALL);
        attrInheritPermissionsFromAll.setRequired(false).setType(String.class).setMultiValued(true).setCreateable(false).setUpdateable(false).setReadable(true);
        roleObjClassBuilder.addAttributeInfo(attrInheritPermissionsFromAll.build());

        // multivalued, read-only
        AttributeInfoBuilder attrMembers = new AttributeInfoBuilder(ATTR_MEMBERS);
        attrMembers.setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true).setMultiValued(true).setReturnedByDefault(false);
        roleObjClassBuilder.addAttributeInfo(attrMembers.build());


        return roleObjClassBuilder.build();
    }

    protected Uid createOrUpdateRole(Uid uid, Set<Attribute> attributes) {
        throw new UnsupportedOperationException("Create/Update operation is not supported for Roles!");
    }

    protected void delete(Uid uid) {
        throw new UnsupportedOperationException("Delete operation is not supported for Roles!");
    }


    protected void updateDeltaMultiValuesForRole(Uid uid, Set<AttributeDelta> attributesDelta, OperationOptions options) {
        LOG.info("updateDeltaMultiValuesForRole on uid: {0}, attrDelta: {1}, options: {2}", uid.getValue(), attributesDelta, options);
        for (AttributeDelta attrDelta : attributesDelta) {
            LOG.info("attrDelta: {0}", attrDelta);
            // add or remove members to/from role
            if (attrDelta.getName().equalsIgnoreCase(ATTR_MEMBERS)) {
                LOG.info("addMembersToRole");
                addOrRemoveMember(uid, attrDelta, ROLE_ASSIGNMENT, options);
            }
        }

    }


    protected void addOrRemoveMember(Uid uid, AttributeDelta attrDelta, String path, OperationOptions options) {
        LOG.info("addOrRemoveMember {0} , {1} , {2}", uid, attrDelta, path);
        StringBuilder sbPath = new StringBuilder();
        sbPath.append(path);
        String roleDefinitionId = uid.getUidValue();

        LOG.info("path: {0}", sbPath);

        List<Object> removeValues = attrDelta.getValuesToRemove();
        roleProcessRemove(sbPath, removeValues, roleDefinitionId, options);

        List<Object> addValues = attrDelta.getValuesToAdd();
        if (addValues != null && !addValues.isEmpty()) {
            for (Object addValue : addValues) {
                if (addValue != null) {

                    JSONObject json = new JSONObject();
                    String principalId = (String) addValue;
                    String directoryScopeId = "/";

                    json.put("principalId", principalId);
                    json.put("directoryScopeId", directoryScopeId);
                    json.put("roleDefinitionId", roleDefinitionId);

                    LOG.ok("json: {0}", json.toString());
                    postRequestNoContent(sbPath.toString(), json);
                }
            }
        }
    }

    protected void addToRole(Uid uid, Set<Attribute> attributes) {
        LOG.info("addToGroup {0} , {1}", uid, attributes);

        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(ATTR_MEMBERS)) {
                addMembersToRole(uid, attribute);
            }
        }
    }

    private void addMembersToRole(Uid uid, Attribute attribute) {
        LOG.info("addMembersToRole {0} , {1}", uid, attribute);

        StringBuilder sbPath = new StringBuilder();
        sbPath.append(ROLE_ASSIGNMENT);
        LOG.info("path: {0}", sbPath);

        String roleDefinitionId = uid.getUidValue();

        JSONObject json = new JSONObject();
        String principalId = AttributeUtil.getAsStringValue(attribute);
        String directoryScopeId = "/";

        json.put("principalId", principalId);
        json.put("directoryScopeId", directoryScopeId);
        json.put("roleDefinitionId", roleDefinitionId);

        LOG.ok("json: {0}", json.toString());
        postRequestNoContent(sbPath.toString(), json);
    }


    protected void removeFromRole(Uid uid, Set<Attribute> attributes, OperationOptions options) {
        LOG.info("removeFromRole {0}, {1}", uid, attributes);
        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(ATTR_MEMBERS)) {
                removeMemberFromRole(uid, attribute, options);
            }
        }
    }

    private void removeMemberFromRole(Uid uid, Attribute attribute, OperationOptions options) {
        LOG.info("addOrRemoveMember {0} , {1} ", uid, attribute);
        StringBuilder sbPath = new StringBuilder();
        sbPath.append(ROLE_ASSIGNMENT);

        String roleDefinitionId = uid.getUidValue();
        String principalId = AttributeUtil.getAsStringValue(attribute);
        String roleAssignmentId = getRoleAssignmentId(options, roleDefinitionId, principalId);
        LOG.info("executeDeleteOperation principalId: {0} , sbPath.toString: {1} ", principalId, sbPath.toString());
        executeDeleteOperation(roleAssignmentId, sbPath.toString());
    }

    private void postRequestNoContent(String path, JSONObject json) {
        LOG.info("path: {0} , json {1}, ", path, json);
        final GraphEndpoint endpoint = getGraphEndpoint();
        final URIBuilder uriBuilder = endpoint.createURIBuilder().setPath(path);
        final URI uri = endpoint.getUri(uriBuilder);
        LOG.info("uri {0}", uri);


        HttpEntityEnclosingRequestBase request;
        LOG.info("HttpEntityEnclosingRequestBase request");

        request = new HttpPost(uri);
        LOG.info("create true - HTTP POST {0}", uri);
        endpoint.callRequestNoContent(request, null, json);
    }


    private void roleProcessRemove(StringBuilder sbPath, List<Object> removeValues, String roleDefinitionId, OperationOptions options) {
        if (removeValues != null && !removeValues.isEmpty()) {
            for (Object removeValue : removeValues) {
                if (removeValue != null) {

                    String principalId = (String) removeValue;
                    String roleAssignmentId = getRoleAssignmentId(options, roleDefinitionId, principalId);
                    LOG.info("executeDeleteOperation principalId: {0} , sbPath.toString: {1} ", principalId, sbPath.toString());
                    executeDeleteOperation(roleAssignmentId, sbPath.toString());
                }
            }
        }
    }

    private void executeDeleteOperation(String roleAssignmentId, String path) {
        LOG.info("Delete object of roleAssignment, id: {0}, Path: {1}", roleAssignmentId, path);

        final GraphEndpoint endpoint = getGraphEndpoint();
        final URIBuilder uriBuilder = endpoint.createURIBuilder();
        uriBuilder.setPath(path + "/" + roleAssignmentId);

        LOG.info("Uri for delete: {0}", uriBuilder);

        final URI uri = endpoint.getUri(uriBuilder);
        final HttpRequestBase request = new HttpDelete(uri);
        endpoint.callRequest(request, false);
    }


    public void executeQueryForRole(Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQueryForRole() Query: {0}", query);
        final GraphEndpoint endpoint = getGraphEndpoint();

        if (query instanceof EqualsFilter) {
            final EqualsFilter equalsFilter = (EqualsFilter) query;
            final String attributeName = equalsFilter.getAttribute().getName();
            LOG.info("Query is instance of EqualsFilter: {0}", query);
            if (equalsFilter.getAttribute() instanceof Uid) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof Uid");

                Uid uid = (Uid) ((EqualsFilter) query).getAttribute();
                if (uid.getUidValue() == null) {
                    invalidAttributeValue("Uid", query);
                }
                StringBuilder sbPath = new StringBuilder();
                sbPath.append(ROLES).append("/").append(uid.getUidValue());

                JSONObject roles = endpoint.executeGetRequest(sbPath.toString(), null, options, false);
                handleJSONObject(options, roles, handler);
            } else if (equalsFilter.getAttribute() instanceof Name) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof Name");

                Name name = (Name) ((EqualsFilter) query).getAttribute();
                String nameValue = name.getNameValue();
                if (nameValue == null) {
                    invalidAttributeValue("Name", query);
                }
                final String customQuery = "$filter=" + ATTR_DISPLAY_NAME + " eq '" + nameValue + "'";
                final JSONObject roles = endpoint.executeGetRequest(ROLES, customQuery, options, true);
                handleJSONArray(options, roles, handler);
            } else if (ATTR_DISPLAY_NAME.equals(attributeName)) {
                final String attributeValue = getAttributeFirstValue(equalsFilter);
                final String customQuery = "$filter=" + attributeName + " eq '" + attributeValue + "'";
                final JSONObject roles = endpoint.executeGetRequest(ROLES, customQuery, options, true);
                handleJSONArray(options, roles, handler);
            }
        } else if (query instanceof ContainsFilter) {
            LOG.info("Query is instance of ContainsFilter: {0}", query);
            final ContainsFilter containsFilter = (ContainsFilter) query;
            final String attributeName = containsFilter.getAttribute().getName();
            final String attributeValue = getAttributeFirstValue(containsFilter);
            if (Arrays.asList(ATTR_DISPLAY_NAME).contains(attributeName)) {
                String customQuery = "$filter=" + STARTSWITH + "(" + attributeName + ",'" + attributeValue + "')";
                JSONObject roles = endpoint.executeGetRequest(ROLES, customQuery, options, true);
                handleJSONArray(options, roles, handler);
            }
        } else if (query instanceof ContainsAllValuesFilter) {
            LOG.info("[QUERY] - ContainsAllValuesFilter - query: {0}", query);
            final ContainsAllValuesFilter containsAllValuesFilter = (ContainsAllValuesFilter) query;
            final String attributeName = containsAllValuesFilter.getAttribute().getName();
            final String attributeValue = getAttributeFirstValue(containsAllValuesFilter);
            LOG.info("[QUERY] - ContainsAllValuesFilter - name is: {0} and value is: {1}", attributeName, attributeValue);

            ArrayList<String> roleUIDs = saturateUserRoleMembership(options, attributeValue);
            JSONObject groups = new JSONObject();
            groups.put("value", new JSONArray());

            LOG.info("[QUERY] - ContainsAllValuesFilter - roleUIDs: {0}", roleUIDs);

            for (String roleUID : roleUIDs) {
                String getPath = ROLES + "/" + roleUID;
                JSONObject group = endpoint.executeGetRequest(getPath, null, options, false);
                groups.append("value", group);
            }

            LOG.info("[QUERY] - ContainsAllValuesFilter - groups: {0}", groups);

            handleJSONArray(options, groups, handler);
        } else if (query == null) {
            LOG.info("Query is null");
            JSONObject roles = endpoint.executeGetRequest(ROLES, null, options, true);
            handleJSONArray(options, roles, handler);
        }
    }

    /**
     * Query a role's members, add them to the group's JSON attributes (multivalue)
     *
     * @param options Operation options
     * @param role Role to query for (JSON object resulting from previous API call)
     *
     * @return Original JSON, enriched with member information
     */
    private JSONObject saturateRoleMembership(OperationOptions options, JSONObject role) {
        final GraphEndpoint endpoint = getGraphEndpoint();
        final String uid = role.getString(ATTR_ID);

        if (getSchemaTranslator().containsToGet(RoleProcessing.ROLE_NAME, options, ATTR_MEMBERS)) {
            LOG.info("[GET] - saturateRoleMembership(), for role with UID: {0}", uid);

            //get list of role members
            final String customQuery = "$filter=roleDefinitionId eq '" + uid + "'";
            final JSONObject roleMembers = endpoint.executeGetRequest(ROLE_ASSIGNMENT, customQuery, options, false);
            role.put(ATTR_MEMBERS, getJSONArray(roleMembers, "principalId"));
        }

        return role;
    }

    private ArrayList<String> saturateUserRoleMembership(OperationOptions options, String principalId) {
        final GraphEndpoint endpoint = getGraphEndpoint();

        LOG.info("[GET] - saturateUsersRoleMembership(), for user with UID: {0}", principalId);

        //get list of roles where the user is memberOf
        final String customQuery = "$filter=principalId eq '" + principalId + "'";
        final JSONObject roleMembers = endpoint.executeGetRequest(ROLE_ASSIGNMENT, customQuery, options, false);

        LOG.info("[GET] - roleMembers: {0}", roleMembers);

        return getArrayList(roleMembers, "roleDefinitionId");
    }

    private String getRoleAssignmentId(OperationOptions options, String roleDefinitionId, String principalId) {
        final GraphEndpoint endpoint = getGraphEndpoint();

        LOG.info("[GET] - getRoleAssignmentId(), for role with UID: {0}", roleDefinitionId);

        //get exactly one roleAssignment by roleDefinitionId and principalId
        final String customQuery = "$filter=roleDefinitionId eq '" + roleDefinitionId + "' and principalId eq '" + principalId + "'";
        final JSONObject roleMembers = endpoint.executeGetRequest(ROLE_ASSIGNMENT, customQuery, options, false);

        LOG.info("[GET] - roleMembers: {0}", roleMembers);

        return (String) getIdFromAssignmentObject(roleMembers, "id", String.class);
    }

    @Override
    protected boolean handleJSONObject(OperationOptions options, JSONObject role, ResultsHandler handler) {
        LOG.info("processingRoleObjectFromGET (Object)");

        if (!Boolean.TRUE.equals(options.getAllowPartialAttributeValues())) {
            role = saturateRoleMembership(options, role);
        }

        final ConnectorObject connectorObject = convertRoleJSONObjectToConnectorObject(role).build();
        LOG.info("processingRoleObjectFromGET, role: {0}, \n\tconnectorObject: {1}", role.get("id"), connectorObject.toString());
        return handler.handle(connectorObject);
    }

    private ConnectorObjectBuilder convertRoleJSONObjectToConnectorObject(JSONObject role) {
        LOG.info("convertRoleJSONObjectToConnectorObject");
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(RoleProcessing.ROLE);

        // UID + NAME
        getUIDIfExists(role, ATTR_ID, builder);
        getNAMEIfExists(role, ATTR_DISPLAY_NAME, builder);

        // single valued
        getIfExists(role, ATTR_DESCRIPTION, String.class, builder);
        getIfExists(role, ATTR_IS_BUILT_IN, Boolean.class, builder);
        getIfExists(role, ATTR_IS_ENABLED, Boolean.class, builder);
        getIfExists(role, ATTR_TEMPLATE_ID, String.class, builder);
        getIfExists(role, ATTR_VERSION, String.class, builder);
        getRoleInheritPermissionsIfExists(role, ATTR_INHERIT_PERMISSIONS_FROM_ODATA_CONTEXT, String.class, builder);

        // specific item in the array
        getRolePermissionsIfExists(role, ATTR_ROLE_PERMISSIONS, ATTR_ALLOWED_RESOURCE_ACTIONS, String.class, builder);
        getFromArrayIfExists(role, ATTR_INHERIT_PERMISSIONS_FROM, ATTR_ID, String.class, builder);

        // multivalued
        getMultiIfExists(role, ATTR_RESOURCE_SCOPES, builder);
        getMultiIfExists(role, ATTR_MEMBERS, builder);

        return builder;
    }

    public String getNameAttribute(){

        return ATTR_DISPLAY_NAME;
    }

    public String getUIDAttribute(){

        return ATTR_ID;
    }

}
