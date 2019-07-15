package com.evolveum.polygon.connector.msconnector.rest;


import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.*;

import java.util.List;
import java.util.Set;


@ConnectorClass(displayNameKey = "msgraphconnector.connector.display", configurationClass = MSGraphConfiguration.class)
public class MSGraphConnector implements Connector,
        CreateOp,
        DeleteOp,
        SearchOp<Filter>,
        TestOp,
        SchemaOp,
        UpdateOp,
        UpdateAttributeValuesOp {


    private static final String USERDIRECTORYOBJECTMEMBER = "userDirectoryObjectMember";
    private static final String USERDIRECTORYOBJECTOWNER = "userDirectoryObjectOwner";

    private static final Log LOG = Log.getLog(MSGraphConnector.class);

    private MSGraphConfiguration configuration;
    private Schema schema = null;


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
        LOG.info("Start create. objectClass: {0}, attributes: {1}, operationOptions: {2} ", objectClass, attributes, operationOptions);

        if (objectClass == null) {
            LOG.error("Attribute of type ObjectClass not provided.");
            throw new InvalidAttributeValueException("Attribute of type ObjectClass not provided.");
        }
        if (attributes == null) {
            LOG.error("Attribute of type Set<Attribute> not provided.");
            throw new InvalidAttributeValueException("Attribute of type Set<Attribute> not provided.");
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
            UserProcessing userProcessing = new UserProcessing(configuration);
            return userProcessing.createUser(attributes);
        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            return groupProcessing.createGroup(attributes);
        } else {
            throw new UnsupportedOperationException("Unsupported object class " + objectClass);
        }

    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {
        LOG.info("Start delete. objectClass: {0}, uid: {1}, operationOption: {2} ", objectClass, uid, operationOptions);

        if (uid.getUidValue() == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Uid not provided or empty:").append(uid.getUidValue()).append(";");
            throw new InvalidAttributeValueException(sb.toString());
        }

        if (objectClass == null) {
            throw new InvalidAttributeValueException("ObjectClass value not provided");
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            UserProcessing user = new UserProcessing(configuration);
            user.deleteUser(uid);
        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing group = new GroupProcessing(configuration);
            group.deleteGroup(uid);
        } else {
            LOG.error("Attribute of type ObjectClass is not supported.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported.");
        }


    }

    @Override
    public Schema schema() {
        LOG.info("Start Schema.");

        if (this.schema == null) {
            SchemaBuilder schemaBuilder = new SchemaBuilder(MSGraphConnector.class);

            UserProcessing userProcessing = new UserProcessing(configuration);
            GroupProcessing groupProcessing = new GroupProcessing(configuration);

            userProcessing.buildUserObjectClass(schemaBuilder);
            groupProcessing.buildGroupObjectClass(schemaBuilder);

            return schemaBuilder.build();
        }

        return this.schema;

    }

    @Override
    public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
        return new FilterTranslator<Filter>() {
            @Override
            public List<Filter> translate(Filter filter) {
                return CollectionUtil.newList(filter);
            }
        };
    }

    @Override
    public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("Filter query {0}", query);

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
            UserProcessing userProcessing = new UserProcessing(configuration);
            userProcessing.executeQueryForUser(query, handler, options);
        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            groupProcessing.executeQueryForGroup(query, handler, options);
        } else {
            LOG.error("Attribute of type ObjectClass is not supported.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported.");
        }

    }

    @Override
    public void test() {
        LOG.info("Start test.");
        ObjectProcessing objectProcessing = new ObjectProcessing(this.configuration);
        objectProcessing.test();
    }

    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions options) {
        LOG.info("From update, object class: {0} uid: {1}, attributes: {2}, opeartionOption: {3}", objectClass, uid.getUidValue(), attributes, options);

        if (objectClass == null) {
            LOG.error("Parameter of type ObjectClass not provided.");
            throw new InvalidAttributeValueException("Parameter of type ObjectClass not provided.");
        }

        if (uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
            LOG.error("Parameter of type Uid not provided or is empty.");
            throw new InvalidAttributeValueException("Parameter of type Uid not provided or is empty.");
        }
        if (options == null) {
            LOG.error("Attribute of type OperationOptions not provided.");
            throw new InvalidAttributeValueException("Attribute of type OperationOptions is not provided.");
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            UserProcessing userProcessing = new UserProcessing(configuration);
            userProcessing.updateUser(uid, attributes);

        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            GroupProcessing groupProcessing = new GroupProcessing(configuration);
            groupProcessing.updateGroup(uid, attributes);

        } else {
            LOG.error("Attribute of type ObjectClass is not supported.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported.");
        }

        return uid;

    }


    @Override
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions options) {
        LOG.info("addAttributeValues, objectClass {0}, uid {1}, attributes {2}, operationOptions {3}", objectClass, uid, attributes, options);
        if (objectClass == null) {
            LOG.error("Parameter of type ObjectClass not provided.");
            throw new InvalidAttributeValueException("Parameter of type ObjectClass not provided.");
        }

        if (uid.getUidValue() == null || uid.getUidValue().isEmpty()) {
            LOG.error("Parameter of type Uid not provided or is empty.");
            throw new InvalidAttributeValueException("Parameter of type Uid not provided or is empty.");
        }
        if (options == null) {
            LOG.error("Attribute of type options not provided.");
        }

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            for (Attribute attribute : attributes) {
                UserProcessing userProcessing = new UserProcessing(configuration);
                LOG.info("attribute: {0}, attribute name: {1}, attribute value: {2}", attribute, attribute.getName(), attribute.getValue());
                if (attribute.getName().equals(USERDIRECTORYOBJECTMEMBER)) {
                    userProcessing.addUserToGroup(attribute, uid);
                } else if (attribute.getName().equals(USERDIRECTORYOBJECTOWNER)) {
                    userProcessing.addUserGroupOwner(attribute, uid);
                }
            }
        } else {
            LOG.error("Attribute of type ObjectClass is not supported.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported.");
        }

        return uid;
    }

    @Override
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions operationOptions) {
        LOG.info("removeAttributeValues, objectClass {0}, uid {1}, attributes {2}, operationOptions {3}", objectClass, uid, attributes, operationOptions);
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

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            UserProcessing userProcessing = new UserProcessing(configuration);
            for (Attribute attribute : attributes) {
                if (attribute.getName().equals(USERDIRECTORYOBJECTMEMBER)) {
                    userProcessing.removeUserFromGroup(attribute, uid);
                } else if (attribute.getName().equals(USERDIRECTORYOBJECTOWNER)) {
                    userProcessing.removeUserGroupOwner(attribute, uid);
                }
            }
        } else {
            LOG.error("Attribute of type ObjectClass is not supported.");
            throw new UnsupportedOperationException("Attribute of type ObjectClass is not supported.");
        }

        return uid;
    }

}