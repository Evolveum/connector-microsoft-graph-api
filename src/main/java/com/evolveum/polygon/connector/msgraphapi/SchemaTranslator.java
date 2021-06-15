package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.SearchOp;

import java.util.*;
import java.util.stream.Collectors;

public class SchemaTranslator {

    private final Schema rawConnIdSchema;
    private final Map<String, Map<String, AttributeInfo>> connIdSchema;

    public SchemaTranslator(GraphEndpoint graphEndpoint) {
        SchemaBuilder schemaBuilder = new SchemaBuilder(MSGraphConnector.class);
        UserProcessing userProcessing = new UserProcessing(graphEndpoint, this);
        GroupProcessing groupProcessing = new GroupProcessing(graphEndpoint);
        LicenseProcessing licenseProcessing = new LicenseProcessing(graphEndpoint, this);

        userProcessing.buildUserObjectClass(schemaBuilder);
        groupProcessing.buildGroupObjectClass(schemaBuilder);
        licenseProcessing.buildLicenseObjectClass(schemaBuilder);

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

    public String[] filter(String type, OperationOptions options, String... attrs) {
        Set<String> returnedAttributes = getAttributesToGet(type, options);
        Set<String> returnedContainerAttributes = returnedAttributes.stream()
                .filter(attr -> attr.contains("."))
                .map(attr -> attr.substring(0, attr.indexOf(".")))
                .distinct()
                .collect(Collectors.toSet());

        String[] filtered = Arrays.stream(attrs)
                .filter(attr -> returnedAttributes.contains(attr)
                        || returnedContainerAttributes.contains(attr))
                .toArray(String[]::new);

        return filtered;
    }

    public Set<String> getAttributesToGet(String type, OperationOptions options) {
        if (!connIdSchema.containsKey(type)) {
            throw new ConnectorException("Invalid ObjectClass type: " + type);
        }

        Set<String> attributesToGet = null;
        if (Boolean.TRUE.equals(options.getReturnDefaultAttributes())) {
            attributesToGet = new HashSet<>();
            attributesToGet.addAll(toReturnedByDefaultAttributesSet(connIdSchema.get(type)));
        }
        if (options.getAttributesToGet() != null) {
            if (attributesToGet == null) {
                attributesToGet = new HashSet<>();
            }
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
