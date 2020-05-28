package com.evolveum.polygon.connector.msgraphapi;

import org.apache.commons.lang3.StringEscapeUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
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

    private ICFPostMapper postMapper;
    private MSGraphConfiguration configuration;

    protected ObjectProcessing(MSGraphConfiguration configuration, ICFPostMapper postMapper) {
        this.configuration = configuration;
        this.postMapper = postMapper;
    }

    protected abstract ObjectClassInfo objectClassInfo();

    protected static final Log LOG = Log.getLog(MSGraphConnector.class);

    public MSGraphConfiguration getConfiguration() {
        return configuration;
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

    protected String getAttributeFirstValue(AttributeFilter filter) {
        final String attributeName = filter.getAttribute().getName();
        final List<Object> allValues = filter.getAttribute().getValue();
        if (allValues == null || allValues.get(0) == null) {
            invalidAttributeValue(attributeName, filter);
        }
        return allValues.get(0).toString();
    }

    protected final JSONArray getJSONArray(JSONObject objectCollection, String attribute) {
        final JSONArray arr = objectCollection.getJSONArray("value");
        return new JSONArray(arr.toList().stream().map(i -> ((Map) i).get(attribute)).collect(Collectors.toList()));
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
            else return value == null ? null : "\"" + value + "\"";
        }
    }

    protected JSONObject buildLayeredAttributeJSON(Set<Attribute> multiLayerAttribute) {
        JSONObject json = new JSONObject();
        for (Attribute attribute : multiLayerAttribute) {
            final String[] attributePath = resolveAttributePath(attribute);
            if (attributePath == null) continue;
            IntermediateNode root = new IntermediateNode();
            IntermediateNode currentNode = root;
            for (int i = 0; i < attributePath.length - 1; i++) {
                Node node = currentNode.keyValueMap.get(attributePath[i]);
                if (node == null) {
                    IntermediateNode child = new IntermediateNode();
                    currentNode.keyValueMap.put(attributePath[i].trim(), child);
                    currentNode = child;
                } else {
                    currentNode = (IntermediateNode) node;
                }
            }
            LeafNode leaf = new LeafNode();
            boolean isMultiValue = isAttributeMultiValues(attribute.getName());
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
                Object value = postMapper.getSingleValue(attribute);
                leaf.value = value == null ? null : value.toString();
            }


            currentNode.keyValueMap.put(attributePath[attributePath.length - 1], leaf);
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
            final String[] attributePath = resolveAttributePath(attribute);
            if (attributePath == null) continue;
            IntermediateNode root = new IntermediateNode();
            IntermediateNode currentNode = root;
            for (int i = 0; i < attributePath.length - 1; i++) {
                Node node = currentNode.keyValueMap.get(attributePath[i]);
                if (node == null) {
                    IntermediateNode child = new IntermediateNode();
                    currentNode.keyValueMap.put(attributePath[i].trim(), child);
                    currentNode = child;
                } else {
                    currentNode = (IntermediateNode) node;
                }
            }
            LeafNode leaf = new LeafNode();
            boolean isMultiValue = isAttributeMultiValues(attribute.getName());
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
                Object value = postMapper.getSingleValue(attribute);
                leaf.value = value == null ? null : value.toString();
            }

            currentNode.keyValueMap.put(attributePath[attributePath.length - 1], leaf);
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

    private String[] resolveAttributePath(Attribute attribute) {
        final String path = postMapper.getTarget(attribute);
        if (path == null) return null;
        else return path.split(DELIMITER);
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

    boolean isAttributeMultiValues(String attrName) {
        for (AttributeInfo ai : objectClassInfo().getAttributeInfo()) {
            if (ai.getName().equals(attrName)) {
                if (ai.isMultiValued()) {
                    return true;
                }
            }
        }

        return false;
    }

    protected abstract void handleJSONObject(JSONObject object, ResultsHandler handler);

    protected void handleJSONArray(JSONObject users, ResultsHandler handler) {
        String jsonStr = users.toString();
        JSONObject jsonObj = new JSONObject(jsonStr);

        JSONArray value;
        try {
            value = jsonObj.getJSONArray("value");
        } catch (JSONException e) {
            LOG.info("No objects in JSON Array");
            return;
        }
        int length = value.length();
        LOG.info("jsonObj length: {0}", length);

        for (int i = 0; i < length; i++) {
            JSONObject user = value.getJSONObject(i);
            handleJSONObject(user, handler);
        }
    }

    /**
     * Create a selector clause for GraphAPI attributes to list (from field names)
     * @param fields Names of fields to query
     * @return Selector clause
     */
    protected static String selector(String... fields) {
        if ( fields == null || fields.length <= 0)
            throw new ConfigurationException("Connector selector query is badly configured. This is likely a programming error.");
        if (Arrays.stream(fields).anyMatch(f ->
                f == null || "".equals(f) || f.contains("&") || f.contains("?") || f.contains("$") || f.contains("=")
        )) throw new ConfigurationException("Connector selector fields contain invalid characters. This is likely a programming error.");
        return "$select=" + Arrays.stream(fields).collect(Collectors.joining(","));
    }

}


