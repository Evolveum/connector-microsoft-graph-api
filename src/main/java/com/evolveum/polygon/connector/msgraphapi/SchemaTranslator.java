package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.SearchOp;

import java.util.*;
import java.util.stream.Collectors;

public class SchemaTranslator {

    private final Schema rawConnIdSchema;
    private final Map<String, Map<String, AttributeInfo>> connIdSchema;
    private static final Log LOG = Log.getLog(SchemaTranslator.class);

    public SchemaTranslator(GraphEndpoint graphEndpoint) {
        SchemaBuilder schemaBuilder = new SchemaBuilder(MSGraphConnector.class);
        UserProcessing userProcessing = new UserProcessing(graphEndpoint, this);
        GroupProcessing groupProcessing = new GroupProcessing(graphEndpoint);
        RoleProcessing roleProcessing = new RoleProcessing(graphEndpoint);
        LicenseProcessing licenseProcessing = new LicenseProcessing(graphEndpoint, this);
        GenericListItemProcessing genericListItemProcessing = new GenericListItemProcessing(graphEndpoint);

        userProcessing.buildUserObjectClass(schemaBuilder);
        groupProcessing.buildGroupObjectClass(schemaBuilder);
        roleProcessing.buildRoleObjectClass(schemaBuilder);
        licenseProcessing.buildLicenseObjectClass(schemaBuilder);
        if (graphEndpoint.getConfiguration().isDiscoverSchema()) {
            genericListItemProcessing.buildSiteListObjectClasses(schemaBuilder);
        }

        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildAttributesToGet(), SearchOp.class);
        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildReturnDefaultAttributes(), SearchOp.class);

        rawConnIdSchema = schemaBuilder.build();

        Map<String, Map<String, AttributeInfo>> objectClasses = new HashMap<>();

        for (ObjectClassInfo ocInfo : rawConnIdSchema.getObjectClassInfo()) {
            Map<String, AttributeInfo> attrs = new HashMap<>();
            for (AttributeInfo info : ocInfo.getAttributeInfo()) {
                attrs.put(info.getName(), info);
            }
            objectClasses.put(ocInfo.getType(), Collections.unmodifiableMap(attrs));
        }

        connIdSchema = Collections.unmodifiableMap(objectClasses);
    }

    public Schema getConnIdSchema() {
        return rawConnIdSchema;
    }

    public Map<String, Map<String, AttributeInfo>> getConnIdSchemaMap() {
        return connIdSchema;
    }

    public String[] filter(String type, OperationOptions options, String... attrs) {
        Set<String> returnedAttributes = getAttributesToGet(type, options);

        if (returnedAttributes.isEmpty()) {
            return attrs;
        }

        Set<String> returnedContainerAttributes = returnedAttributes.stream()
                .filter(attr -> attr.contains("."))
                .map(attr -> attr.substring(0, attr.indexOf(".")))
                .collect(Collectors.toSet());

        return Arrays.stream(attrs)
                .filter(attr -> returnedAttributes.contains(attr)
                        || returnedContainerAttributes.contains(attr))
                .toArray(String[]::new);
    }

    public Set<String> getAttributesToGet(String type, OperationOptions options) {
        if (!connIdSchema.containsKey(type)) {
            throw new ConnectorException("Invalid ObjectClass type: " + type);
        }

        Set<String> attributesToGet = new HashSet<>();
        if (Boolean.TRUE.equals(options.getReturnDefaultAttributes())) {
            attributesToGet.addAll(toReturnedByDefaultAttributesSet(connIdSchema.get(type)));
        }
        if (options.getAttributesToGet() != null) {
            for (String a : options.getAttributesToGet()) {
                attributesToGet.add(a);
            }
        }
        return attributesToGet;
    }

    public boolean containsToGet(String type, OperationOptions options, String attr) {
        Set<String> returnedAttributes = getAttributesToGet(type, options);
        return returnedAttributes.contains(attr);
    }

    private static Set<String> toReturnedByDefaultAttributesSet(Map<String, AttributeInfo> attrs) {
        return attrs.entrySet().stream()
                .filter(entry -> entry.getValue().isReturnedByDefault())
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());
    }
}
