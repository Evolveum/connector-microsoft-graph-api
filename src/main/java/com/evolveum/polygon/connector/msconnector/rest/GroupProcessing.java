package com.evolveum.polygon.connector.msconnector.rest;

import com.microsoft.graph.concurrency.ICallback;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.extensions.Group;
import com.microsoft.graph.models.extensions.OnPremisesProvisioningError;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.extensions.IDirectoryObjectCollectionWithReferencesRequest;
import com.microsoft.graph.requests.extensions.IGroupCollectionPage;
import com.microsoft.graph.requests.extensions.IGroupCollectionRequest;
import com.microsoft.graph.requests.extensions.IGroupCollectionRequestBuilder;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class GroupProcessing extends ObjectProcessing {

    //By default, only a limited set of properties are returned, use select to return more properties
    private static final String SELECT = "id,displayName,mailEnabled,mailNickname,securityEnabled,classification," +
            "createdDateTime,description,groupTypes,mail,onPremisesLastSyncDateTime,onPremisesProvisioningErrors," +
            "onPremisesSecurityIdentifier,onPremisesSyncEnabled,proxyAddresses,renewedDateTime,visibility";

    private static final String STARTSWITH = "startswith";

    private static final String ALLOWEXTERNALSENDERS = "allowExternalSenders";
    private static final String ASSIGNEDLICENSES = "assignedLicenses";
    private static final String DISABLEDPLANS = "disabledPlans";
    private static final String SKUID = "skuId";


    private static final String AUTOSUBSCRIBENEWMEMBERS = "autoSubscribeNewMembers";
    private static final String CLASSIFICATION = "classification";
    private static final String CREATEDDATETIME = "createdDateTime";
    private static final String DESCRIPTION = "description";
    private static final String DISPLAYNAME = "displayName";
    private static final String GROUPTYPES = "groupTypes";
    private static final String HASMEMBERSWITHLICENSEERRORS = "hasMembersWithLicenseErrors";
    private static final String ISSUBSCRIBEDBYMAIL = "isSubscribedByMail";
    private static final String LICENSEPROCESSINGSTATE = "licenseProcessingState";
    private static final String MAIL = "mail";
    private static final String MAILENABLED = "mailEnabled";
    private static final String MAILNICKNAME = "mailNickname";
    //onPremisesLastSyncDateTime
    private static final String ONPREMISESPROVISIONINGERRORS = "onPremisesProvisioningErrors";
    private static final String CATEGORY = "category";
    private static final String OCCURREDDATETIME = "occurredDateTime";
    private static final String PROPERTYCAUSINGERROR = "propertyCausingError";
    private static final String VALUE = "value";

    private static final String ONPREMISESLASTSYNCDATETIME = "onPremisesLastSyncDateTime";
    private static final String ONPREMISESSECURITYIDENTIFIER = "onPremisesSecurityIdentifier";
    private static final String ONPREMISESSYNCENABLED = "onPremisesSyncEnabled";
    private static final String PREFERREDDATALOCATION = "preferredDataLocation";
    private static final String PROXYADDRESSES = "proxyAddresses";
    private static final String RENEWEDDATETIME = "renewedDateTime";
    private static final String SECURITYENABLED = "securityEnabled";
    private static final String UNSEENCOUNT = "unseenCount";
    private static final String VISIBILITY = "visibility";


    private Uid groupUid;
    private CountDownLatch loginLatch;
    private boolean failure;
    private ClientException clientException;

    private static final String GROUPMEMBERS = "groupMembers";
    private static final String GROUPOWNERS = "groupOwners";


    public GroupProcessing(MSGraphConfiguration configuration) {
        super(configuration);
    }


    public void buildGroupObjectClass(SchemaBuilder schemaBuilder) {


        ObjectClassInfoBuilder groupObjClassBuilder = new ObjectClassInfoBuilder();

        groupObjClassBuilder.setType(ObjectClass.GROUP_NAME);

        //required

        //Supports $filter and $orderby
//        AttributeInfoBuilder attrDisplayName = new AttributeInfoBuilder(DISPLAYNAME);
//        attrDisplayName.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
//        groupObjClassBuilder.addAttributeInfo(attrDisplayName.build());

        AttributeInfoBuilder attrMailEnabled = new AttributeInfoBuilder(MAILENABLED);
        attrMailEnabled.setRequired(true).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrMailEnabled.build());

        //Supports $filter
        AttributeInfoBuilder attrMailNickname = new AttributeInfoBuilder(MAILNICKNAME);
        attrMailNickname.setRequired(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrMailNickname.build());

        //$filter
        AttributeInfoBuilder attrSecurityEnabled = new AttributeInfoBuilder(SECURITYENABLED);
        attrSecurityEnabled.setRequired(true).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrSecurityEnabled.build());


        //optional

        AttributeInfoBuilder attrAllowExternalSenders = new AttributeInfoBuilder(ALLOWEXTERNALSENDERS);
        attrAllowExternalSenders.setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrAllowExternalSenders.build());

        AttributeInfoBuilder attrDisabledPlans = new AttributeInfoBuilder(ASSIGNEDLICENSES + "." + DISABLEDPLANS);
        attrDisabledPlans.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true).setMultiValued(true);
        groupObjClassBuilder.addAttributeInfo(attrDisabledPlans.build());

        AttributeInfoBuilder attrSkuId = new AttributeInfoBuilder(ASSIGNEDLICENSES + "." + SKUID);
        attrSkuId.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrSkuId.build());

        AttributeInfoBuilder attrAutoSubscribeNewMembers = new AttributeInfoBuilder(AUTOSUBSCRIBENEWMEMBERS);
        attrAutoSubscribeNewMembers.setRequired(false).setType(Boolean.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrAutoSubscribeNewMembers.build());

        AttributeInfoBuilder attrClassification = new AttributeInfoBuilder(CLASSIFICATION);
        attrClassification.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrClassification.build());

        AttributeInfoBuilder attrCreatedDateTime = new AttributeInfoBuilder(CREATEDDATETIME);
        attrCreatedDateTime.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrCreatedDateTime.build());

        AttributeInfoBuilder attrDescription = new AttributeInfoBuilder(DESCRIPTION);
        attrDescription.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrDescription.build());

        //Supports $filter
        AttributeInfoBuilder attrGroupTypes = new AttributeInfoBuilder(GROUPTYPES);
        attrGroupTypes.setRequired(false).setType(String.class).setMultiValued(true).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrGroupTypes.build());

        AttributeInfoBuilder attrHasMembersWithLicenseErrors = new AttributeInfoBuilder(HASMEMBERSWITHLICENSEERRORS);
        attrHasMembersWithLicenseErrors.setRequired(false).setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrHasMembersWithLicenseErrors.build());


        AttributeInfoBuilder attrIsSubscribedByMail = new AttributeInfoBuilder(ISSUBSCRIBEDBYMAIL);
        attrIsSubscribedByMail.setRequired(false).setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrIsSubscribedByMail.build());

        AttributeInfoBuilder attrLicenseProcessingState = new AttributeInfoBuilder(LICENSEPROCESSINGSTATE);
        attrLicenseProcessingState.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrLicenseProcessingState.build());

        //Read-only, supports $filter.
        AttributeInfoBuilder attrMail = new AttributeInfoBuilder(MAIL);
        attrMail.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrMail.build());

        AttributeInfoBuilder attrCategory = new AttributeInfoBuilder(ONPREMISESPROVISIONINGERRORS + "." + CATEGORY);
        attrCategory.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrCategory.build());

        AttributeInfoBuilder attrOccurredDateTime = new AttributeInfoBuilder(ONPREMISESPROVISIONINGERRORS + "." + OCCURREDDATETIME);
        attrOccurredDateTime.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOccurredDateTime.build());

        AttributeInfoBuilder attrPropertyCausingError = new AttributeInfoBuilder(ONPREMISESPROVISIONINGERRORS + "." + PROPERTYCAUSINGERROR);
        attrPropertyCausingError.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrPropertyCausingError.build());

        AttributeInfoBuilder attrValue = new AttributeInfoBuilder(ONPREMISESPROVISIONINGERRORS + "." + VALUE);
        attrValue.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrValue.build());

        //Read-only, supports $filter.
        AttributeInfoBuilder attrOnPremisesLastSyncDateTime = new AttributeInfoBuilder(ONPREMISESLASTSYNCDATETIME);
        attrOnPremisesLastSyncDateTime.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOnPremisesLastSyncDateTime.build());

        //read-only
        AttributeInfoBuilder attrOnPremisesSecurityIdentifier = new AttributeInfoBuilder(ONPREMISESSECURITYIDENTIFIER);
        attrOnPremisesSecurityIdentifier.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOnPremisesSecurityIdentifier.build());

        //Read-only, Supports $filter.
        AttributeInfoBuilder attrOnPremisesSyncEnabled = new AttributeInfoBuilder(ONPREMISESSYNCENABLED);
        attrOnPremisesSyncEnabled.setRequired(false).setType(Boolean.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOnPremisesSyncEnabled.build());

        //not implemented
        AttributeInfoBuilder attrPreferredDataLocation = new AttributeInfoBuilder(PREFERREDDATALOCATION);
        attrPreferredDataLocation.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrPreferredDataLocation.build());

        //Read-only, Not nullable, Supports $filter, multivalued
        AttributeInfoBuilder attrProxyAddresses = new AttributeInfoBuilder(PROXYADDRESSES);
        attrProxyAddresses.setRequired(false).setType(String.class).setMultiValued(true).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrProxyAddresses.build());

        //not implemented
        AttributeInfoBuilder attrRenewedDateTime = new AttributeInfoBuilder(RENEWEDDATETIME);
        attrRenewedDateTime.setRequired(false).setType(String.class).setCreateable(false).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrRenewedDateTime.build());

        AttributeInfoBuilder attrUnseenCount = new AttributeInfoBuilder(UNSEENCOUNT);
        attrUnseenCount.setRequired(false).setType(Integer.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrUnseenCount.build());

        AttributeInfoBuilder attrVisibility = new AttributeInfoBuilder(VISIBILITY);
        attrVisibility.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(false).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrVisibility.build());

        AttributeInfoBuilder attrMembers = new AttributeInfoBuilder(GROUPMEMBERS);
        attrMembers.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrMembers.build());

        AttributeInfoBuilder attrOwners = new AttributeInfoBuilder(GROUPOWNERS);
        attrOwners.setRequired(false).setMultiValued(true).setType(String.class).setCreateable(true).setUpdateable(true).setReadable(true);
        groupObjClassBuilder.addAttributeInfo(attrOwners.build());


        schemaBuilder.defineObjectClass(groupObjClassBuilder.build());


    }


    public Uid createGroup(Set<Attribute> attributes) {
        LOG.info("Start createGroup, attributes: {0}", attributes);

        if (attributes == null || attributes.isEmpty()) {
            throw new InvalidAttributeValueException("attributes not provided or empty");
        }

        final ICallback<Group> callback = new ICallback<Group>() {
            @Override
            public void success(final Group group) {
                LOG.ok("Create group success!");
                groupUid = new Uid(group.id);
                failure = false;
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Create group failed!");
                clientException = e;
                failure = true;
                loginLatch.countDown();

            }
        };

        Group group = new Group();
        boolean mustUpdateGroup = false;

        for (Attribute attribute : attributes) {

            if (attribute.getName().equals(Name.NAME)) {
                group.displayName = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(MAILENABLED)) {
                group.mailEnabled = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(MAILNICKNAME)) {
                group.mailNickname = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(SECURITYENABLED)) {
                group.securityEnabled = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(GROUPTYPES)) {
                List<String> groupTypes = new ArrayList<>();
                for (Object groupType : attribute.getValue()) {
                    groupTypes.add((String) groupType);
                }
                group.groupTypes = groupTypes;
            } else if (attribute.getName().equals(ALLOWEXTERNALSENDERS)) {
                //this attribute must be set in a PATCH request
                mustUpdateGroup = true;
            } else if (attribute.getName().equals(AUTOSUBSCRIBENEWMEMBERS)) {
                //this attribute must be set in a PATCH request
                mustUpdateGroup = true;
            } else if (attribute.getName().equals(DESCRIPTION)) {
                group.description = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(VISIBILITY)) {
                group.visibility = AttributeUtil.getAsStringValue(attribute);
            }

        }
        //create group
        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().groups().buildRequest().post(group, callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }


        //patch group
        if (mustUpdateGroup) {
            Group updateGroup = new Group();
            for (Attribute attribute : attributes) {
                if (attribute.getName().equals(AUTOSUBSCRIBENEWMEMBERS)) {
                    updateGroup.autoSubscribeNewMembers = AttributeUtil.getBooleanValue(attribute);
                } else if (attribute.getName().equals(ALLOWEXTERNALSENDERS)) {
                    updateGroup.allowExternalSenders = AttributeUtil.getBooleanValue(attribute);
                }
            }
            loginLatch = new CountDownLatch(1);
            getGraphServiceClient().groups(groupUid.getUidValue()).buildRequest().patch(updateGroup);
            try {
                loginLatch.await();
            } catch (InterruptedException e) {
                throw new ConnectorException(e);
            }
        }


        if (failure) {
            throw new ClientException("Error while creating group!", clientException);
        }

        return groupUid;
    }

    public void deleteGroup(Uid uid) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }

        LOG.info("Delete group with UID: {0}", uid.getUidValue());

        final ICallback<Group> callback = new ICallback<Group>() {
            @Override
            public void success(Group group) {
                LOG.ok("Delete group success!");
                failure = false;
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Delete group failed!");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().groups().byId(uid.getUidValue()).buildRequest().delete(callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
        if (failure) {
            throw new ClientException("Error while deleting group!", clientException);
        }


    }

    public void executeQueryForGroup(Filter query, final ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQueryForGroup()");
        Integer top = options.getPageSize();
        Integer skip = options.getPagedResultsOffset();
        if (query instanceof EqualsFilter) {
            LOG.info("query instanceof EqualsFilter");
            if (((EqualsFilter) query).getAttribute() instanceof Uid) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof Uid");

                Uid uid = (Uid) ((EqualsFilter) query).getAttribute();
                if (uid.getUidValue() == null) {
                    invalidAttributeValue("Uid", query);
                }

                final ICallback<Group> callback = new ICallback<Group>() {
                    @Override
                    public void success(Group group) {
                        LOG.ok("Get group success!");
                        convertGroupToConnectorObject(group, handler);
                        failure = false;
                        loginLatch.countDown();
                    }

                    @Override
                    public void failure(ClientException e) {
                        LOG.error("Get group failed!");
                        clientException = e;
                        failure = true;
                        loginLatch.countDown();
                    }
                };

                loginLatch = new CountDownLatch(1);

                List<Option> requestOptions = new ArrayList<>();
                requestOptions.add(new QueryOption("$select", SELECT));

                getGraphServiceClient().groups(uid.getUidValue()).buildRequest(requestOptions).get(callback);
                try {
                    loginLatch.await();
                } catch (InterruptedException e) {
                    throw new ConnectorException(e);
                }
                if (failure) {
                    throw new ClientException("Error while execute query for group!", clientException);
                }

            } else if (((EqualsFilter) query).getAttribute().getName().equals(Name.NAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("displayName", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=displayName eq 'attribute'
                requestOptions.add(new QueryOption("$filter", "displayName" + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IGroupCollectionRequest request = createRequest(requestOptions, top);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(GROUPTYPES)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof groupTypes");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("groupTypes", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);
                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=groupTypes eq 'attribute'
                requestOptions.add(new QueryOption("$filter", "groupTypes/any(c:c" + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "')"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(MAIL)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof mail");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mail", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=mail eq 'attribute'
                requestOptions.add(new QueryOption("$filter", "mail" + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(MAILNICKNAME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof mailNickname");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mailNickname", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=mailNickname eq 'attribute'
                requestOptions.add(new QueryOption("$filter", "mailNickname" + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "'"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(ONPREMISESLASTSYNCDATETIME)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof onPremisesLastSyncDateTime");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("onPremisesLastSyncDateTime", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=onPremisesLastSyncDateTime eq 'attribute'
                requestOptions.add(new QueryOption("$filter", "onPremisesLastSyncDateTime" + "%20eq%20" + "'" + attributeValue + "'"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(ONPREMISESSYNCENABLED)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof onPremisesSyncEnabled");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("onPremisesSyncEnabled", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=onPremisesSyncEnabled eq boolean
                requestOptions.add(new QueryOption("$filter", "onPremisesSyncEnabled" + "%20eq%20" + attributeValue));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(PROXYADDRESSES)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof proxyAddresses");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("proxyAddresses", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=proxyAddresses eq 'attribute'
                requestOptions.add(new QueryOption("$filter", "proxyAddresses/any(c:c" + "%20eq%20" + "'" + getEncodedAttributeValue(attributeValue) + "')"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((EqualsFilter) query).getAttribute().getName().equals(SECURITYENABLED)) {
                LOG.info("((EqualsFilter) query).getAttribute() instanceof securityEnabled");

                List<Object> allValues = ((EqualsFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("securityEnabled", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<Option>();
                //$filter=securityEnabled eq boolean
                requestOptions.add(new QueryOption("$filter", "securityEnabled" + "%20eq%20" + attributeValue));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            }
        } else if (query instanceof StartsWithFilter) {
            LOG.info("query instanceof StartsWithFilter");
            if (((StartsWithFilter) query).getAttribute().getName().equals(Name.NAME)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof displayName");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("displayName", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(displayName,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + DISPLAYNAME + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(MAIL)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof mail");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mail", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(mail,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + MAIL + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(MAILNICKNAME)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof mailNickname");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("mailNickname", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=startswith(mailNickname,'attribute')
                requestOptions.add(new QueryOption("$filter", STARTSWITH + "(" + MAILNICKNAME + ",'" + getEncodedAttributeValue(attributeValue) + "')"));
                IGroupCollectionRequest request = getGraphServiceClient().groups().buildRequest(requestOptions);
                GroupCollectionPageProcessing(handler, request, skip);
            } else if (((StartsWithFilter) query).getAttribute().getName().equals(PROXYADDRESSES)) {
                LOG.info("((StartsWithFilter) query).getAttribute() instanceof proxyAddresses");

                List<Object> allValues = ((StartsWithFilter) query).getAttribute().getValue();
                if (allValues == null || allValues.get(0) == null) {
                    invalidAttributeValue("proxyAddresses", query);
                }

                String attributeValue = allValues.get(0).toString();

                LOG.info("value {0}", attributeValue);

                List<Option> requestOptions = new ArrayList<>();
                //$filter=proxyAddresses/any(x:startswith(x,'attribute'))
                requestOptions.add(new QueryOption("$filter", PROXYADDRESSES + "/any" + "(x:" + STARTSWITH + "(x,'" + getEncodedAttributeValue(attributeValue) + "'))"));
                IGroupCollectionRequest request = createRequest(requestOptions, top);
                GroupCollectionPageProcessing(handler, request, skip);
            }


        } else if (query == null) {
            LOG.info("query==null");
            IGroupCollectionRequest request;

            //no requestOptions
            if (top != null) {
                request = getGraphServiceClient().groups().buildRequest().top(top);
            } else {
                request = getGraphServiceClient().groups().buildRequest();
            }

            GroupCollectionPageProcessing(handler, request, skip);
        }
    }


    private IGroupCollectionRequest createRequest(List<Option> requestOptions, Integer top) {
        if (top != null) {
            getGraphServiceClient().groups().buildRequest(requestOptions).top(top);
        }
        return getGraphServiceClient().groups().buildRequest(requestOptions);
    }

    private void GroupCollectionPageProcessing(ResultsHandler handler, IGroupCollectionRequest request, Integer skip) {
        if (skip == null) {
            GroupCollectionPageProcessing(handler, request);
        } else {
            IGroupCollectionPage page;
            IGroupCollectionRequestBuilder builder;
            int count = 1;
            do {
                page = request.get();

                if (count == skip) {
                    for (Group group : page.getCurrentPage()) {
                        convertGroupToConnectorObject(group, handler);
                    }
                    return;
                }
                builder = page.getNextPage();
                if (builder == null) {
                    request = null;
                } else {
                    request = builder.buildRequest();
                }
                count++;
            } while (builder != null);
        }
    }

    private void GroupCollectionPageProcessing(ResultsHandler handler, IGroupCollectionRequest request) {
        IGroupCollectionPage page;
        IGroupCollectionRequestBuilder builder;
        do {
            page = request.get();
            for (Group group : page.getCurrentPage()) {
                convertGroupToConnectorObject(group, handler);
            }
            builder = page.getNextPage();
            if (builder == null) {
                request = null;
            } else {
                request = builder.buildRequest();
            }
        } while (builder != null);
    }

    public void updateGroup(Uid uid, Set<Attribute> attributes) {
        LOG.info("Start updateGroup, attributes: {0} , uid: {1}", attributes, uid);

        if (attributes == null || attributes.isEmpty()) {
            throw new InvalidAttributeValueException("attributes not provided or empty");
        }

        final ICallback<Group> callback = new ICallback<Group>() {
            @Override
            public void success(final Group group) {
                LOG.ok("Update group success!");
                loginLatch.countDown();
            }

            @Override
            public void failure(ClientException e) {
                LOG.error("Update group failed!");
                clientException = e;
                failure = true;
                loginLatch.countDown();
            }
        };

        Group group = new Group();

        for (Attribute attribute : attributes) {

            if (attribute.getName().equals(Name.NAME)) {
                group.displayName = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(MAILENABLED)) {
                group.mailEnabled = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(MAILNICKNAME)) {
                group.mailNickname = AttributeUtil.getAsStringValue(attribute);
            } else if (attribute.getName().equals(SECURITYENABLED)) {
                group.securityEnabled = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(GROUPTYPES)) {
                List<String> groupTypes = new ArrayList<>();
                for (Object groupType : attribute.getValue()) {
                    groupTypes.add((String) groupType);
                }
                group.groupTypes = groupTypes;
            } else if (attribute.getName().equals(ALLOWEXTERNALSENDERS)) {
                group.allowExternalSenders = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(AUTOSUBSCRIBENEWMEMBERS)) {
                group.autoSubscribeNewMembers = AttributeUtil.getBooleanValue(attribute);
            } else if (attribute.getName().equals(DESCRIPTION)) {
                group.description = AttributeUtil.getAsStringValue(attribute);
            }

        }

        loginLatch = new CountDownLatch(1);
        getGraphServiceClient().groups(uid.getUidValue()).buildRequest().patch(group, callback);
        try {
            loginLatch.await();
        } catch (InterruptedException e) {
            throw new ConnectorException(e);
        }
        if (failure) {
            throw new ClientException("Error while updating group!", clientException);
        }
    }


    private void invalidAttributeValue(String attrName, Filter query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Value of").append(attrName).append("attribute not provided for query: ").append(query);
        throw new InvalidAttributeValueException(sb.toString());
    }


    private void convertGroupToConnectorObject(Group group, ResultsHandler handler) {
        LOG.info("convertGroupToConnectorObject, group: {0}, handler {1}", group.mail, handler);
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        builder.setObjectClass(ObjectClass.GROUP);

        if (group.id != null) {
            builder.setUid(new Uid(group.id));
        }
        if (group.displayName != null) {
            builder.setName(group.displayName);
        }
        if (group.mailEnabled != null) {
            builder.addAttribute(MAILENABLED, group.mailEnabled);
        }
        if (group.mailNickname != null) {
            builder.addAttribute(MAILNICKNAME, group.mailNickname);
        }
        if (group.securityEnabled != null) {
            builder.addAttribute(SECURITYENABLED, group.securityEnabled);
        }
        if (group.classification != null) {
            builder.addAttribute(CLASSIFICATION, group.classification);
        }
        if (group.createdDateTime != null) {
            builder.addAttribute(CREATEDDATETIME, group.createdDateTime.getTime().toString());
        }
        if (group.description != null) {
            builder.addAttribute(DESCRIPTION, group.description);
        }
        if (group.groupTypes != null) {
            builder.addAttribute(GROUPTYPES, group.groupTypes);
        }
        if (group.mail != null) {
            builder.addAttribute(MAIL, group.mail);
        }
        if (group.onPremisesLastSyncDateTime != null) {
            builder.addAttribute(ONPREMISESLASTSYNCDATETIME, group.onPremisesLastSyncDateTime.getTime().toString());
        }
        if (group.onPremisesProvisioningErrors != null) {
            List<String> category = new ArrayList<>();
            List<String> occurredDateTime = new ArrayList<>();
            List<String> propertyCausingError = new ArrayList<>();
            List<String> value = new ArrayList<>();
            for (OnPremisesProvisioningError error : group.onPremisesProvisioningErrors) {
                if (error.category != null) {
                    category.add(error.category);
                }
                if (error.occurredDateTime != null) {
                    occurredDateTime.add(error.occurredDateTime.getTime().toString());
                }
                if (error.propertyCausingError != null) {
                    propertyCausingError.add(error.propertyCausingError);
                }
                if (error.value != null) {
                    value.add(error.value);
                }
            }
            builder.addAttribute(ONPREMISESPROVISIONINGERRORS + "." + CATEGORY, category);
            builder.addAttribute(ONPREMISESPROVISIONINGERRORS + "." + OCCURREDDATETIME, occurredDateTime);
            builder.addAttribute(ONPREMISESPROVISIONINGERRORS + "." + PROPERTYCAUSINGERROR, propertyCausingError);
            builder.addAttribute(ONPREMISESPROVISIONINGERRORS + "." + VALUE, value);
        }
        if (group.onPremisesSecurityIdentifier != null) {
            builder.addAttribute(ONPREMISESSECURITYIDENTIFIER, group.onPremisesLastSyncDateTime);
        }
        if (group.onPremisesSyncEnabled != null) {
            builder.addAttribute(ONPREMISESSYNCENABLED, group.onPremisesSyncEnabled);
        }
        if (group.proxyAddresses != null) {
            builder.addAttribute(PROXYADDRESSES, group.proxyAddresses);
        }
        if (group.renewedDateTime != null) {
            builder.addAttribute(RENEWEDDATETIME, group.renewedDateTime.getTime().toString());
        }
        if (group.visibility != null) {
            builder.addAttribute(VISIBILITY, group.visibility);
        }

        convertGroupMembers(group.id, builder);
        convertGroupOwners(group.id, builder);


        ConnectorObject connectorObject = builder.build();
        handler.handle(connectorObject);
    }


    private void convertGroupMembers(String groupId, ConnectorObjectBuilder builder) {
        LOG.info("convertGroupMembers, groupId {0}", groupId);

        IDirectoryObjectCollectionWithReferencesRequest request = getGraphServiceClient().groups(groupId).members().buildRequest();
        //list with users in group
        List<String> listDirectoryObject = getListDirectoryObject(request, USER);

        builder.addAttribute(GROUPMEMBERS, listDirectoryObject);
    }

    private void convertGroupOwners(String groupId, ConnectorObjectBuilder builder) {
        LOG.info("convertGroupOwners, groupId {0}", groupId);

        IDirectoryObjectCollectionWithReferencesRequest request = getGraphServiceClient().groups(groupId).owners().buildRequest();
        List<String> listDirectoryObject = getListDirectoryObject(request, USER);

        builder.addAttribute(GROUPOWNERS, listDirectoryObject);
    }


}


