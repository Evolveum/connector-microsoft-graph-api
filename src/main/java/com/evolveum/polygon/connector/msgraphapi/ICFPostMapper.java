package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.objects.Attribute;

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
    private final Map<String, Function<Attribute, List<Object>>> postMapping;

    private ICFPostMapper(
            Map<String, String> icfAttributeMapping,
            Map<String, Function<Attribute, List<Object>>> postMapping
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
    public String getTarget(Attribute attribute) {
        if (ICF_ATTRIBUTE_NAME_PATTERN.matcher(attribute.getName()).matches())
            return icfAttributeMapping.get(attribute.getName());
        else {
            return attribute.getName();
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
        final Function<Attribute, List<Object>> postProcessFn = postMapping.get(attribute.getName());
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
        final Function<Attribute, List<Object>> postProcessFn = postMapping.get(attribute.getName());
        if (postProcessFn != null) {
            return postProcessFn.apply(attribute);
        } else {
            return attribute.getValue();
        }
    }

    /**
     * Builder for {@link ICFPostMapper}
     */
    static class ICFPostMapperBuilder {
        private final Map<String, String> icfAttributeMapping = new TreeMap<>();
        private final Map<String, Function<Attribute, List<Object>>> postMapping = new TreeMap<>();

        public ICFPostMapperBuilder remap(String icfAttributeName, String endpointAttributeName) {
            if (icfAttributeName != null && endpointAttributeName != null)
                icfAttributeMapping.put(icfAttributeName, endpointAttributeName);
            return this;
        }

        public ICFPostMapperBuilder postProcess(String icfAttributeName, Function<Attribute, List<Object>> mapping) {
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
}
