package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Utility for ICF attribute validation.
 * Its main purpose is to catch errors in outgoing data relevant to connector before they reach target resource API.
 *
 * @author radek.macha@ami.cz
 */
public class AttributesValidator {

    final Map<String, Predicate<List<Object>>> validationRules;

    private AttributesValidator(Map<String, Predicate<List<Object>>> validationRules) {
        this.validationRules = validationRules;
    }

    public void validate(Set<Attribute> attributes) throws InvalidAttributeValueException  {
        for (Attribute a: attributes) {
            final Predicate<List<Object>> rule = validationRules.get(a.getName());
            if (rule != null && !rule.test(a.getValue())) throw new InvalidAttributeValueException(String.format(
                    "Value of attribute %s did not match validation criteria", a.getName()
            ));
        }
    }

    /**
     * Create a builder for {@link AttributesValidator}
     *
     * @return Builder
     */
    public static AttributesValidatorBuilder builder() {
        return new AttributesValidatorBuilder();
    }

    /**
     * Validation ruleset builder.
     */
    public static class AttributesValidatorBuilder {
        final Map<String, Predicate<List<Object>>> validationRules = new TreeMap<>();

        /**
         * Creates an 'exists' validation rule on a set of mandatory attributes.
         * @param attributes Names of the attributes to consider as mandatory
         * @return This validation builder
         */
        public AttributesValidatorBuilder withAttributes(String... attributes) {
            for (String an: attributes) {
                final Predicate<List<Object>> existingRule = validationRules.get(an);
                if (existingRule == null) validationRules.put(an, l -> !l.isEmpty());
                else validationRules.put(an, l -> existingRule.test(l) && !l.isEmpty());
            }
            return this;
        }

        /**
         * Creates an 'exists and is not empty' validation rule on a set of mandatory attributes.
         * @param attributes Names of the attributes to consider as mandatory
         * @return This validation builder
         */
        public AttributesValidatorBuilder withNonEmptyAttributes(String... attributes) {
            for (String an: attributes) {
                final Predicate<List<Object>> existingRule = validationRules.get(an);
                final Predicate<List<Object>> newRule = l -> !l.isEmpty() && testAll(
                        o -> o != null && !"".equals(String.valueOf(o))
                ).test(l);
                if (existingRule == null) validationRules.put(an, newRule);
                else validationRules.put(an, l -> existingRule.test(l) && newRule.test(l));
            }
            return this;
        }

        /**
         * Creates a generic validation rule on an attribute
         * @param attributeName Name of the attribute
         * @param validationRule Validation
         * @return This validation builder
         */
        public AttributesValidatorBuilder with(String attributeName, Predicate<Object> validationRule) {
            addRule(attributeName, validationRule);
            return this;
        }

        /**
         * Creates a string regex matcher attribute validator
         * @param attributeName Name of the attribute
         * @param regex Regex to match
         * @return This validation builder
         */
        public AttributesValidatorBuilder withRegex(String attributeName, String regex) {
            validationRules.put(attributeName, testAll(o -> {
                if (!(o instanceof String)) return false;
                return Pattern.compile(regex).matcher((String) o).matches();
            }));
            return this;
        }

        /**
         * Adds a validation rule for an attribute.
         * Stacks validation rules rather then overwriting them.
         *
         * @param attributeName Name of validated attribute
         * @param rule Validation rule to add
         */
        private void addRule(String attributeName, Predicate<Object> rule) {
            if (rule == null) return;
            final Predicate<List<Object>> existingRule = validationRules.get(attributeName);
            if (existingRule == null) validationRules.put(attributeName, testAll(rule));
            else validationRules.put(attributeName, l -> existingRule.test(l) && testAll(rule).test(l));
        }

        /**
         * Transform a single value validation predicate into a predicate that validates each value in a list.
         * @param testOne Single value predicate
         * @return Multi-value predicate
         */
        private Predicate<List<Object>> testAll(Predicate<Object> testOne) {
            return objects -> {
                for (Object i : objects) if (!testOne.test(i)) return false;
                return true;
            };
        }

        /**
         * Build the attributes validator.
         * @return Attributes validator.
         */
        public AttributesValidator build() {
            return new AttributesValidator(validationRules);
        }
    }
}
