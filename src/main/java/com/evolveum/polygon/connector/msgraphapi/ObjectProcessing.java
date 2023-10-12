package com.evolveum.polygon.connector.msgraphapi;

import org.apache.commons.lang3.StringEscapeUtils;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

abstract class ObjectProcessing {

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
    protected static final String O_DELTA = "@delta";
    private ICFPostMapper postMapper;
    private GraphEndpoint graphEndpoint;

    protected ObjectProcessing(GraphEndpoint graphEndpoint, ICFPostMapper postMapper) {
        this.graphEndpoint = graphEndpoint;
        this.postMapper = postMapper;
    }

    protected abstract String type();

    protected abstract ObjectClassInfo objectClassInfo();

    protected static final Log LOG = Log.getLog(MSGraphConnector.class);

    public GraphEndpoint getGraphEndpoint() {
        return graphEndpoint;
    }

    public SchemaTranslator getSchemaTranslator() {
        return graphEndpoint.getSchemaTranslator();
    }

    public MSGraphConfiguration getConfiguration() {
        return graphEndpoint.getConfiguration();
    }

    protected void getIfExists(JSONObject object, String attrName, Class<?> type, ConnectorObjectBuilder builder) {
        if (object.has(attrName) && object.get(attrName) != null && !JSONObject.NULL.equals(object.get(attrName)) && !String.valueOf(object.get(attrName)).isEmpty()) {
            if (type.equals(String.class)) {
                addAttr(builder, attrName, String.valueOf(object.get(attrName)));
            } else if (type.equals(byte[].class)) {
                addAttr(builder, attrName, java.util.Base64.getDecoder().decode(String.valueOf(object.get(attrName))));
            } else {
                addAttr(builder, attrName, object.get(attrName));
            }
        }
    }

    protected void getRoleInheritPermissionsIfExists(JSONObject object, String attrName, Class<?> type, ConnectorObjectBuilder builder) {
        if (object.has(attrName) && object.get(attrName) != null && !JSONObject.NULL.equals(object.get(attrName)) && !String.valueOf(object.get(attrName)).isEmpty()) {
            if (type.equals(String.class)) {
                String attrValue = String.valueOf(object.get(attrName));
                attrValue = attrValue.replaceFirst("https://graph.microsoft.com/v1.0/\\$metadata#roleManagement/directory/roleDefinitions\\('", "");
                attrValue = attrValue.replaceFirst("'\\)/inheritsPermissionsFrom", "");
                addAttr(builder, attrName, attrValue);
            } else {
                addAttr(builder, attrName, object.get(attrName));
            }
        }
    }

    protected void getAndRenameIfExists(JSONObject object, String jsonAttrName, Class<?> type, String icfAttrName, ConnectorObjectBuilder builder) {
        if (object.has(jsonAttrName) && object.get(jsonAttrName) != null && !JSONObject.NULL.equals(object.get(jsonAttrName)) && !String.valueOf(object.get(jsonAttrName)).isEmpty()) {
            if (type.equals(String.class)) {
                addAttr(builder, icfAttrName, String.valueOf(object.get(jsonAttrName)));
            } else {
                addAttr(builder, icfAttrName, object.get(jsonAttrName));
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

    private Object getValueFromItem(JSONObject object, String attrName, Class<?> type) {
        if (object.has(attrName) && object.get(attrName) != null && !JSONObject.NULL.equals(object.get(attrName)) && !String.valueOf(object.get(attrName)).isEmpty()) {
            if (type.equals(String.class))
                return String.valueOf(object.get(attrName));
            else
                return object.get(attrName);
        } else {
            return null;
        }
    }

    protected Object getIdFromAssignmentObject(JSONArray value, String attrName, Class<?> type) {
        int length = value.length();
        LOG.info("JSON Object length: {0}", length);

        if (length == 1) {
            JSONObject assignmentObject = value.getJSONObject(0);
            if (assignmentObject.has(attrName) && assignmentObject.get(attrName) != null && !JSONObject.NULL.equals(assignmentObject.get(attrName)) && !String.valueOf(assignmentObject.get(attrName)).isEmpty()) {
                if (type.equals(String.class))
                    return String.valueOf(assignmentObject.get(attrName));
                else
                    return assignmentObject.get(attrName);
            } else {
                return null;
            }
        } else {
            LOG.info("JSON Object should have size exactly 1");
            return null;
        }
    }

    protected void getFromItemIfExists(JSONObject object, String attrName, String subAttrName, Class<?> type, ConnectorObjectBuilder builder) {
        if (object.has(attrName)) {
            Object valueObject = object.get(attrName);
            if (valueObject != null) {
                Object subValue = getValueFromItem((JSONObject) valueObject, subAttrName, type);
                builder.addAttribute(attrName + "." + subAttrName, subValue);
            }
        }
    }

    protected void getJSONObjectItemIfExists(JSONObject object, String attrName, String subAttrName, Class<?> type, ConnectorObjectBuilder builder) {
        if (object.has(attrName)) {
            Object valueObject = object.get(attrName);
            if (valueObject != null && !JSONObject.NULL.equals(valueObject)) {
                if (valueObject instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) valueObject;

                    if (subAttrName != null) {
                        Object value = getValueFromItem(jsonObject, subAttrName, type);
                        if (value != null)
                            builder.addAttribute(attrName + "." + subAttrName, value);
                    }
                }
            }
        }
    }

    protected void getFromArrayIfExists(JSONObject object, String attrName, String subAttrName, Class<?> type, ConnectorObjectBuilder builder) {

        getFromArrayIfExists(object, attrName, subAttrName, null, type, builder, false);
    }

    protected void getFromArrayIfExists(JSONObject object, String attrName, String subAttrName, String omitTag,
                                        Class<?> type, ConnectorObjectBuilder builder, Boolean isDelta) {

        String originalName = attrName;

        if(isDelta){

            attrName = attrName+ O_DELTA;
        }

        if (object.has(attrName)) {
            Object valueObject = object.get(attrName);
            if (valueObject != null && !JSONObject.NULL.equals(valueObject)) {
                if (valueObject instanceof JSONArray) {
                    JSONArray objectArray = (JSONArray) valueObject;
                    List<Object> values = new ArrayList<>();
                    objectArray.forEach(it -> {
                        if (it instanceof JSONObject) {

                            if (omitTag != null) {

                                if (!object.has(omitTag)) {

                                    Object subValue = getValueFromItem((JSONObject) it, subAttrName, type);
                                    if (subValue != null)
                                        values.add(subValue);
                                }
                            } else {

                                Object subValue = getValueFromItem((JSONObject) it, subAttrName, type);
                                if (subValue != null)
                                    values.add(subValue);
                            }
                        }
                    });
                    builder.addAttribute(originalName + "." + subAttrName, values.toArray());
                }
            }
        }
    }

    protected void getRolePermissionsIfExists(JSONObject object, String attrName, String subAttrName, Class<?> type, ConnectorObjectBuilder builder) {
        if (object.has(attrName)) {
            Object valueObject = object.get(attrName);
            if (valueObject != null && !JSONObject.NULL.equals(valueObject)) {
                if (valueObject instanceof JSONArray) {
                    JSONArray objectArray = (JSONArray) valueObject;
                    List<String> workingValues = new ArrayList<>();
                    List<String> returnValues = new ArrayList<>();
                    objectArray.forEach(it -> {
                        if (it instanceof JSONObject) {
                            String subValue = (String) getValueFromItem((JSONObject) it, subAttrName, type);
                            String conditionValue = (String) getValueFromItem((JSONObject) it, "condition", type);
                            if (subValue != null)
                                workingValues.addAll(
                                        Arrays.asList(
                                                subValue.replaceAll("\\[", "")
                                                        .replaceAll("\\]", "")
                                                        .replaceAll("\"", "")
                                                        .split(",")
                                        )
                                );

                            returnValues.addAll(workingValues.stream().map(value -> {
                                if (conditionValue != null)
                                    return conditionValue + "|" + value;
                                else
                                    return value;
                            }).collect(Collectors.toList()));
                        }
                    });
                    builder.addAttribute(attrName + "." + subAttrName, returnValues.toArray());
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
        LOG.ok("getUIDIfExists nameAttr: {0} builder {1}", nameAttr, builder);
        if (object.has(nameAttr)) {
            String uid = object.getString(nameAttr);
            builder.setUid(new Uid(String.valueOf(uid)));
            return uid;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing required attribute: ").append(nameAttr)
                    .append(" for converting JSONObject to ConnectorObject.");
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
                    .append(" for converting JSONObject to ConnectorObject.");
            throw new InvalidAttributeValueException(sb.toString());
        }
    }

    protected void getNAMEIfExists(JSONObject object, String nameAttr, ConnectorObjectBuilder builder) {
        if (object.has(nameAttr)) {
            builder.setName(object.getString(nameAttr));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Missing required attribute: ").append(nameAttr)
                    .append(" for converting JSONObject to ConnectorObject.");
            throw new InvalidAttributeValueException(sb.toString());
        }
    }

    protected String getAttributeFirstValue(AttributeFilter filter) {
        final String attributeName = filter.getAttribute().getName();
        final List<Object> allValues = filter.getAttribute().getValue();
        if (allValues == null || allValues.get(0) == null) {
            invalidAttributeValue(attributeName, filter);
        }
        return allValues.get(0).toString();
    }

    protected final JSONArray getJSONArray(JSONArray arr, String attribute) {
        return new JSONArray(arr.toList().stream().map(i -> ((Map) i).get(attribute)).collect(Collectors.toList()));
    }

    protected void invalidAttributeValue(String attrName, Filter query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Value of").append(attrName).append("attribute not provided for query: ").append(query);
        throw new InvalidAttributeValueException(sb.toString());
    }

    protected JSONObject buildLayeredAttributeJSON(Set<Attribute> multiLayerAttribute, Set<String> excludeAttrs) {
        JSONObject json = new JSONObject();
        for (Attribute attribute : multiLayerAttribute) {
            if (excludeAttrs.contains(attribute.getName())) {
                continue;
            }

            final String[] attributePath = resolveAttributePath(attribute);
            if (attributePath == null) {
                continue;
            }

            JSONObject current = json;
            for (int i = 0; i < attributePath.length - 1; i++) {
                if (!current.has(attributePath[i])) {
                    JSONObject child = new JSONObject();
                    current.put(attributePath[i], child);
                    current = child;
                } else {
                    current = current.getJSONObject(attributePath[i]);
                }
            }

            boolean isMultiValue = isAttributeMultiValues(attribute.getName());
            LOG.info("attribute {0} isMultiValue {1}", attribute, isMultiValue);

            String key = attributePath[attributePath.length - 1];

            if (isMultiValue) {
                List<Object> attributes = postMapper.getMultiValue(attribute);
                for (Object value : attributes) {
                    current.append(key, value);
                }
            } else {
                Object value = postMapper.getSingleValue(attribute);
                if (value == null) {
                    current.put(key, JSONObject.NULL);
                } else {
                    current.put(key, value);
                }
            }
        }

        return json;
    }

    protected List<JSONObject> buildLayeredAttribute(JSONObject oldJson, Set<AttributeDelta> modifications, Set<String> excludeAttrs, Set<String> separatedAttrs) {
        final JSONObject json = new JSONObject();
        final JSONObject separatedJson = new JSONObject();

        if (oldJson != null) {
            for(String key : oldJson.keySet()) {
                if (separatedAttrs.contains(key)) {
                    separatedJson.put(key, oldJson.get(key));
                } else {
                    json.put(key, oldJson.get(key));
                }
            }
        }

        for (AttributeDelta attributeDelta : modifications) {
            if (excludeAttrs.contains(attributeDelta.getName())) {
                continue;
            }

            final String[] attributePath = resolveAttributePath(attributeDelta);
            if (attributePath == null) {
                continue;
            }

            JSONObject current;
            if (separatedAttrs.contains(attributeDelta.getName())) {
                current = separatedJson;
            } else {
                current = json;
            }

            for (int i = 0; i < attributePath.length - 1; i++) {
                if (!current.has(attributePath[i])) {
                    JSONObject child = new JSONObject();
                    current.put(attributePath[i], child);
                    current = child;
                } else {
                    current = current.getJSONObject(attributePath[i]);
                }
            }

            boolean isMultiValue = isAttributeMultiValues(attributeDelta.getName());
            LOG.info("attributeDelta {0} isMultiValue {1}", attributeDelta, isMultiValue);

            String key = attributePath[attributePath.length - 1];

            if (isMultiValue) {
                final Set<String> currentValues;
                if (!current.has(key)) {
                    currentValues = Collections.emptySet();
                } else {
                    currentValues = new LinkedHashSet(current.getJSONArray(key).toList());
                }

                final Set<Object> removeValues;
                if (attributeDelta.getValuesToRemove() != null) {
                    removeValues = new LinkedHashSet(postMapper.getMultiValueToRemove(attributeDelta));
                } else {
                    removeValues = Collections.emptySet();
                }

                JSONArray mergedValues = new JSONArray();
                for (Object value : currentValues) {
                    if (!removeValues.contains(value)) {
                        mergedValues.put(value);
                    }
                }

                if (attributeDelta.getValuesToAdd() != null) {
                    List<Object> addValues = postMapper.getMultiValueToAdd(attributeDelta);
                    for (Object value : addValues) {
                        // Avoid duplicate values
                        if (!currentValues.contains(value)) {
                            mergedValues.put(value);
                        }
                    }
                }

                // When removing all values, Microsoft Graph API expects empty JSON array (Don't set null for empty)
                current.put(key, mergedValues);
            } else {
                Object value = postMapper.getSingleValue(attributeDelta);
                if (value == null) {
                    current.put(key, JSONObject.NULL);
                } else {
                    current.put(key, value);
                }
            }
        }

        List<JSONObject> list = new ArrayList<>();
        if (!json.isEmpty()) {
            list.add(json);
        }
        if (!separatedJson.isEmpty()) {
            list.add(separatedJson);
        }

        return list;
    }

    private String[] resolveAttributePath(Attribute attribute) {
        return resolveAttributePath(attribute.getName());
    }

    private String[] resolveAttributePath(AttributeDelta attributeDelta) {
        return resolveAttributePath(attributeDelta.getName());
    }

    private String[] resolveAttributePath(String attribute) {
        final String path = postMapper.getTarget(attribute);
        if (path == null) return null;
        else return path.split(DELIMITER);
    }

    boolean isAttributeMultiValues(String attrName) {
        Map<String, Map<String, AttributeInfo>> connIdSchemaMap = getSchemaTranslator().getConnIdSchemaMap();
        Map<String, AttributeInfo> attributeInfoMap = connIdSchemaMap.get(type());
        AttributeInfo attributeInfo = attributeInfoMap.get(attrName);
        if (attributeInfo != null && attributeInfo.isMultiValued()) {
            return true;
        }
        return false;
    }

    protected abstract boolean handleJSONObject(OperationOptions options, JSONObject object, ResultsHandler handler);

    @FunctionalInterface
    protected interface JSONObjectHandler {
        boolean handle(OperationOptions options, JSONObject object);
    }

    protected JSONObjectHandler createJSONObjectHandler(ResultsHandler handler) {
        return (options, jsonObject) -> handleJSONObject(options, jsonObject, handler);
    }

    protected List<JSONObject> handleJSONArray(JSONArray value) {
        List<JSONObject> objectList = CollectionUtil.newList();

        int length = value.length();
        LOG.ok("jsonObj length: {0}", length);

        for (int i = 0; i < length; i++) {
            JSONObject obj = value.getJSONObject(i);
            objectList.add(obj);
        }

        return objectList;
    }

    /**
     * Create a selector clause for GraphAPI attributes to list (from field names)
     *
     * @param fields Names of fields to query
     * @return Selector clause
     */
    protected static String selector(String... fields) {
        if (fields == null || fields.length == 0)
            throw new ConfigurationException("Connector selector query is badly configured. This is likely a programming error.");
        if (Arrays.stream(fields).anyMatch(f ->
                f == null || "".equals(f) || f.contains("&") || f.contains("?") || f.contains("$") || f.contains("=")
        ))
            throw new ConfigurationException("Connector selector fields contain invalid characters. This is likely a programming error.");
        return "$select=" + String.join(",", fields);
    }

    protected boolean shouldSaturate(OperationOptions options, String type, String attr) {
        return !Boolean.TRUE.equals(options.getAllowPartialAttributeValues()) && getSchemaTranslator().containsToGet(type, options, attr);
    }

    protected void incompleteIfNecessary(OperationOptions options, String type, String attr, ConnectorObjectBuilder builder) {
        if (Boolean.TRUE.equals(options.getAllowPartialAttributeValues()) && getSchemaTranslator().containsToGet(type, options, attr)) {
            AttributeBuilder attrBuilder = new AttributeBuilder();
            attrBuilder.setName(attr).setAttributeValueCompleteness(AttributeValueCompleteness.INCOMPLETE);
            attrBuilder.addValue(Collections.EMPTY_LIST);
            builder.addAttribute(attrBuilder.build());
        }
    }
}


