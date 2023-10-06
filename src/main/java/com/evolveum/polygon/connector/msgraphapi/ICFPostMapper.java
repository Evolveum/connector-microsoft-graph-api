package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaUtil;
import org.identityconnectors.framework.common.objects.AttributeUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Configuration of the post-mapping of ICF attributes to target object.
 * Handles re-mapping of ICF attributes and other post-ICF value mappings.
 */
public class ICFPostMapper {

    protected static final Pattern ICF_ATTRIBUTE_NAME_PATTERN = Pattern.compile("__[A-Z]+__");

    private final Map<String, String> icfAttributeMapping;
    private final Map<String, Function<UnionAttribute, List<Object>>> postMapping;

    private ICFPostMapper(
            Map<String, String> icfAttributeMapping,
            Map<String, Function<UnionAttribute, List<Object>>> postMapping
    ) {
        this.icfAttributeMapping = icfAttributeMapping;
        this.postMapping = postMapping;
    }

    /**
     * Create a post mapper builder
     *
     * @return Builder
     */
    public static ICFPostMapperBuilder builder() {
        return new ICFPostMapperBuilder();
    }

    /**
     * Get the target attribute of an attribute (handles ICF attribute remapping)
     *
     * @param attribute ICF attribute name
     *
     * @return Target name for the attribute (remapped) or <code>null</code> if attribute is ICF-native and has no remap
     */
    public String getTarget(String attribute) {
        if (ICF_ATTRIBUTE_NAME_PATTERN.matcher(attribute).matches())
            return icfAttributeMapping.get(attribute);
        else {
            return attribute;
        }
    }

    /**
     * Return single value of attribute. If there are multiple values to the attribute, return the first one.
     *
     * @param attribute Attribute to retrieve value for
     *
     * @return Attribute single value
     */
    public Object getSingleValue(Attribute attribute) {
        return getSingleValue(new UnionAttribute(attribute));
    }

    public Object getSingleValue(AttributeDelta attributeDelta) {
        return getSingleValue(new UnionAttribute(attributeDelta));
    }

    private Object getSingleValue(UnionAttribute attribute) {
        final Function<UnionAttribute, List<Object>> postProcessFn = postMapping.get(attribute.getName());
        if (postProcessFn != null) {
            final List<Object> value = postProcessFn.apply(attribute);
            return value.isEmpty() ? null : value.get(0);
        } else {
            return attribute.getValue().isEmpty() ? null : attribute.getValue().get(0);
        }
    }

    /**
     * Get list of values for given attribute.
     *
     * @param attribute Attribute to retrieve value for
     *
     * @return Attribute multivalue
     */
    public List<Object> getMultiValue(Attribute attribute) {
        final Function<UnionAttribute, List<Object>> postProcessFn = postMapping.get(attribute.getName());
        if (postProcessFn != null) {
            return postProcessFn.apply(new UnionAttribute(attribute));
        } else {
            return attribute.getValue();
        }
    }

    public List<Object> getMultiValueToAdd(AttributeDelta attributeDelta) {
        final Function<UnionAttribute, List<Object>> postProcessFn = postMapping.get(attributeDelta.getName());
        if (postProcessFn != null) {
            return postProcessFn.apply(new UnionAttribute(attributeDelta));
        } else {
            return attributeDelta.getValuesToAdd();
        }
    }

    public List<Object> getMultiValueToRemove(AttributeDelta attributeDelta) {
        final Function<UnionAttribute, List<Object>> postProcessFn = postMapping.get(attributeDelta.getName());
        if (postProcessFn != null) {
            return postProcessFn.apply(new UnionAttribute(attributeDelta));
        } else {
            return attributeDelta.getValuesToRemove();
        }
    }

    /**
     * Builder for {@link ICFPostMapper}
     */
    static class ICFPostMapperBuilder {
        private final Map<String, String> icfAttributeMapping = new TreeMap<>();
        private final Map<String, Function<UnionAttribute, List<Object>>> postMapping = new TreeMap<>();

        public ICFPostMapperBuilder remap(String icfAttributeName, String endpointAttributeName) {
            if (icfAttributeName != null && endpointAttributeName != null)
                icfAttributeMapping.put(icfAttributeName, endpointAttributeName);
            return this;
        }

        public ICFPostMapperBuilder postProcess(String icfAttributeName, Function<UnionAttribute, List<Object>> mapping) {
            if (icfAttributeName == null || mapping == null) return this;
            if (postMapping.put(icfAttributeName, mapping) != null) throw new ConfigurationException(String.format(
                    "Multiple post-processing directives for ICF attribute [%s]", icfAttributeName
            ));
            return this;
        }

        /**
         * Build the post-mapper
         *
         * @return Post-mapper
         */
        public ICFPostMapper build() {
            return new ICFPostMapper(
                    Collections.unmodifiableMap(icfAttributeMapping),
                    Collections.unmodifiableMap(postMapping)
            );
        }
    }

    static class UnionAttribute {
        final Attribute attribute;
        final AttributeDelta attributeDelta;

        public UnionAttribute(Attribute attribute) {
            this.attribute = attribute;
            this.attributeDelta = null;
        }

        public UnionAttribute(AttributeDelta attributeDelta) {
            this.attribute = null;
            this.attributeDelta = attributeDelta;
        }

        public String getName() {
            if (attribute != null) {
                return attribute.getName();
            }
            return attributeDelta.getName();
        }

        public List<Object> getValue() {
            if (attribute != null) {
                return attribute.getValue();
            }
            return attributeDelta.getValuesToReplace();
        }
        public Object getSingleValue() {
            if (attribute != null) {
                return AttributeUtil.getSingleValue(attribute);
            }
            return AttributeDeltaUtil.getSingleValue(attributeDelta);
        }

        public boolean isCreate() {
            return attribute != null;
        }

        public boolean isUpdate() {
            return attributeDelta != null;
        }

        public List<Object> getValuesToAdd() {
            return attributeDelta.getValuesToAdd();
        }

        public List<Object> getValuesToRemove() {
            return attributeDelta.getValuesToRemove();
        }

    }
}
