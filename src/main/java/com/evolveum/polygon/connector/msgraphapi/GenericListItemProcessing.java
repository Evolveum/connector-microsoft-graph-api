package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Optional;

public class GenericListItemProcessing extends ObjectProcessing {
    private static final String SITES = "/sites";
    private static final String LISTS = "/lists";
    private static final String ITEMS = "/items";

    private static final String COLUMNS = "/columns";

    ///  Attributes
    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_IS_PERSONAL = "isPersonalSite";
    private static final String ATTR_READ_ONLY = "readOnly";
    private static final String ATTR_REQUIRED = "required";
    private static final String ATTR_TEXT = "text";
    private static final String ATTR_FIELDS = "fields";
    private static final String ATTR_ALLOW_MULTILINE = "allowMultipleLines";

    ///  Delimiters
    private static final String SITE_LIST_DELIMITER = "~";

    private static final String SELECTOR_PARTIAL = selector(
            ATTR_ID,
            ATTR_NAME,
            ATTR_IS_PERSONAL
    );

    protected GenericListItemProcessing(GraphEndpoint graphEndpoint) {
        super(graphEndpoint, ICFPostMapper.builder().build());
    }

    @Override
    protected String type() { return ""; }

    @Override
    protected ObjectClassInfo objectClassInfo() { return null; }

    @Override
    protected boolean handleJSONObject(OperationOptions options, JSONObject object, ResultsHandler handler) {
        // Does not handle objects as generic ones
        return false;
    }

    protected boolean handleJSONObject(ObjectClass objectClass, OperationOptions options, JSONObject json, ResultsHandler handler) {
        ObjectClassInfo classInfo = getSchemaTranslator().getConnIdSchema().findObjectClassInfo(objectClass.getObjectClassValue());

        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(objectClass);

        getUIDIfExists(json, ATTR_ID, builder);
        String itemName = null;
        JSONObject fields = json.getJSONObject(ATTR_FIELDS);
        classInfo.getAttributeInfo().forEach(attributeInfo -> {
            if (attributeInfo.isMultiValued()) {
                getIfExists(fields, attributeInfo.getName(), String.class, builder);
            } else {
                getIfExists(fields, attributeInfo.getName(), String.class, builder);
            }
        });

        String[] possibleAttrNames = getConfiguration().getExpectedPropertyNames().split(",");
        for(String attrName: possibleAttrNames) {
            LOG.info("Azure connector: Searching for attribute:" + attrName + " in keyset:" + fields.keySet());
            itemName = getStringIfExistsIgnoreCase(fields, attrName, String.class, builder);
            if (itemName != null) {
                builder.setName(itemName);
                break;
            }
        }

        if (itemName == null) {
            LOG.warn("Azure connector: Defaulting to __NAME__ as ID as it is the only chance to list items in the list without proper naming.");
            itemName = getStringIfExistsIgnoreCase(fields, ATTR_ID, String.class, builder); // Intentionally throw Invalid argument if there is no ID present
            if (itemName != null) {
                builder.setName(itemName);
                LOG.warn("The list item does not contain any property for name, Please specify main key for name in project. KeySet:" + json.keySet().toString());
            }
        }

        return handler.handle(builder.build());
    }

    public void executeQueryForListRecords(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        String[] path = objectClass.getObjectClassValue().split(SITE_LIST_DELIMITER);

        if(path.length != 2) {
            throw new InvalidAttributeValueException("ObjectClassName has to be splittable by ~ delimiter into two components: SiteName~ListName. This object class is not supported." + objectClass.getObjectClassValue());
        }
        String site = path[0];
        String list = path[1];

        Optional<String> siteId;;
        Optional<String> listId = Optional.empty();

        // Workaround due site name not being searchable on the Azure site API - unsupported
        siteId = listSites().stream()
                .filter(item -> item.getString(ATTR_NAME).equalsIgnoreCase(site) )
                .map(item -> item.getString(ATTR_ID))
                .findFirst();
        // Workaround, it often fails to search the list by name, therefore requires ID
        if (siteId.isPresent()) {
            listId = listsForSite(siteId.get()).stream()
                    .filter(item -> item.getString(ATTR_NAME).equalsIgnoreCase(list) )
                    .map(item -> item.getString(ATTR_ID))
                    .findFirst();
        }

        if (siteId.isPresent() && listId.isPresent()) {
            LOG.info("Azure connector: Searching for site with id: " + siteId.get());
            if (query != null) {
                if (query instanceof EqualsFilter) {
                    final EqualsFilter equalsFilter = (EqualsFilter) query;
                    final Attribute attr = equalsFilter.getAttribute();
                    final String attrName = attr.getName();

                    if (attrName.equals(ATTR_ID) || attrName.equals(Uid.NAME)) {
                        String value = AttributeUtil.getAsStringValue(attr);
                        if (value == null)
                            invalidAttributeValue("Uid", query);
                        getField(objectClass, value, handler, options, siteId.get(), listId.get());
                    }
                } else {
                    LOG.warn("Unsupported filter, only equals filter is supported.");
                }
            } else {
                listFields(objectClass, handler, options, siteId.get(), listId.get());
            }
        } else {
            throw new InvalidAttributeValueException("The provided object class name is invalid - invalid site name provided: " + site + ". It could not be found within sites. Check if the site wasn't deleted.");
        }
    }

    private void getField(ObjectClass objectClass, String id, ResultsHandler handler, OperationOptions options, String site, String list) {
        final String itemsQuery = SITES + "/" + site + LISTS + "/" + list + ITEMS + "/" + id;
        final GraphEndpoint endpoint = getGraphEndpoint();

        handleJSONObject(objectClass, options, endpoint.executeGetRequest(itemsQuery, "expand=fields", options), handler);
    }

    private void listFields(ObjectClass objectClass, ResultsHandler handler, OperationOptions options, String site, String list) {
        final String itemsQuery = SITES + "/" + site + LISTS + "/" + list + ITEMS;
        final GraphEndpoint endpoint = getGraphEndpoint();

        JSONObjectHandler objectHandler = (operationOptions, jsonObject) -> handleJSONObject(objectClass, operationOptions, jsonObject, handler);
        endpoint.executeListRequest(itemsQuery, "expand=fields", options, false, objectHandler);
    }

    private List<JSONObject> listSites() {
        final GraphEndpoint endpoint = getGraphEndpoint();
        JSONArray json = endpoint.executeListRequest(SITES, SELECTOR_PARTIAL, null, false);
        LOG.info("JSONObject sites {0}", json);
        return handleJSONArray(json);
    }

    private List<JSONObject> listsForSite(String site) {
        final GraphEndpoint endpoint = getGraphEndpoint();
        JSONArray json = endpoint.executeListRequest(SITES + "/" + site + LISTS , null, null, false);
        LOG.info("JSONObject lists in site {0}", json);
        return handleJSONArray(json);
    }

    private List<JSONObject> columnsForSiteAndList(String site, String list) {
        final GraphEndpoint endpoint = getGraphEndpoint();
        JSONArray json = endpoint.executeListRequest(SITES + "/" + site + LISTS + "/" + list + COLUMNS, null, null, false);
        LOG.info("JSONObject columns in list {0}", json);
        return handleJSONArray(json);
    }

    protected void buildSiteListObjectClasses(SchemaBuilder schemaBuilder) {
        boolean ignorePersonalSites = getConfiguration().getIgnorePersonalSites();
        listSites().stream()
                .map(SchemaItem::new)
                .filter(item -> item.isAllowed(ignorePersonalSites))
                .filter(SchemaItem::canInitialize)
                .forEach(site -> {
                    listsForSite(site.id).stream()
                            .map(SchemaItem::new)
                            .filter(SchemaItem::canInitialize)
                            .forEach(list -> {
                                ObjectClassInfoBuilder listRecordClassBuilder = new ObjectClassInfoBuilder();
                                String schemaRecordName = site.name + SITE_LIST_DELIMITER + list.name;
                                listRecordClassBuilder.setType(schemaRecordName);

                                LOG.info("Storing schema record of name: " + schemaRecordName);

                                columnsForSiteAndList(site.id, list.id)
                                        .stream()
                                        .map(ColumnDefinition::new)
                                        .forEach(columnDefinition -> {
                                            AttributeInfoBuilder attribute = new AttributeInfoBuilder(columnDefinition.name);
                                            attribute.setRequired(columnDefinition.required)
                                                    .setType(String.class)
                                                    .setCreateable(!columnDefinition.readOnly)
                                                    .setUpdateable(!columnDefinition.readOnly)
                                                    .setReadable(true)
                                                    .setMultiValued(columnDefinition.multiValue);
                                            listRecordClassBuilder.addAttributeInfo(attribute.build());
                                        });
                                try {
                                    schemaBuilder.defineObjectClass(listRecordClassBuilder.build());
                                } catch (IllegalStateException exception) {
                                    LOG.warn("Redefinition of the schema by same name!" + exception.getLocalizedMessage());
                                }
                            });
                });
    }

    static class SchemaItem {
        String id;
        String name;
        boolean isPersonalSite;

        SchemaItem(JSONObject item) {
            id = item.optString(ATTR_ID);
            name = item.optString(ATTR_NAME);
            isPersonalSite = item.optBoolean(ATTR_IS_PERSONAL);
        }

        boolean canInitialize() {
            return !id.isEmpty() && !name.isEmpty();
        }

        boolean isAllowed(boolean isPersonalSiteEnabled) {
            if (isPersonalSiteEnabled) {
                return true;
            } else {
                return !isPersonalSite;
            }
        }
    }

    static class ColumnDefinition {
        String name;
        boolean readOnly;
        boolean required;
        boolean multiValue;

        ColumnDefinition(JSONObject item) {
            name = item.optString(ATTR_NAME);
            readOnly = item.optBoolean(ATTR_READ_ONLY);
            required = item.optBoolean(ATTR_REQUIRED);

            Object text = item.opt(ATTR_TEXT);
            if (text != null) {
                multiValue = ((JSONObject)text).optBoolean(ATTR_ALLOW_MULTILINE);
            } else {
                multiValue = false;
            }
        }
    }
}
