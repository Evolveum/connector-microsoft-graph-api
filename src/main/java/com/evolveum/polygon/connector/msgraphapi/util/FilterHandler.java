package com.evolveum.polygon.connector.msgraphapi.util;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.common.CollectionUtil;

import java.util.*;

public class FilterHandler implements FilterVisitor<String, String> {

    private static final Log LOG = Log.getLog(FilterHandler.class);


    //Equality operators

    private static final String EQUALS_OP ="eq";
    private static final String NOT_EQUAL_OP ="ne";
    private static final String NOT_OP ="not";
    private static final String IN_OP ="in";
    private static final String HA_OP ="has";

    // Relational operators

    private static final String LESS_OP ="lt";

    private static final String GREATER_OP ="gt";

    private static final String LESS_OR_EQ_OP ="le";

    private static final String GREATER_OR_EQUALS_OP ="ge";

    // Lambda operators

    private static final String ANY_OP ="any";

    private static final String ALL_OP ="all";


    // Conditional operators

    private static final String AND_OP ="and";

    private static final String OR_OP ="or";

    // Functions

    private static final String STARTS_WITH_OP ="startsWith";

    private static final String ENDS_WITH_OP ="endsWith";

    private static final String CONTAINS_OP ="contains";

    // DELIMITER

    private static final String _L_PAR = "(";
    private static final String _R_PAR = ")";
    private static final String _COL = ",";
    private static final String _ASTERISK = "*";

    private static final String _SLASH = "/";

    private static final String _VALUE_WRAPPER = "'";

    private static final String _PADDING = " ";
    // TODO UID.NAME and NAME.NAME has to be additionally translated
    @Override
    public String visitAndFilter (String  p, AndFilter andFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through AND filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Collection<Filter> filters = andFilter.getFilters();

        Map<String, Boolean> snippets = processCompositeFilter(filters, p);
        Set<String> items = snippets.keySet();
        Iterator<String> keyIterator =items.iterator();

        while (keyIterator.hasNext()){

            String snipp = keyIterator.next();
            Boolean isLastItem = !keyIterator.hasNext();

            if (snippets.get(snipp)){

                if (!isLastItem){
                query.append(wrapValue(snipp, _L_PAR, _R_PAR))
                        .append(_PADDING)
                        .append(AND_OP);

                } else {

                    query.append(_PADDING)
                            .append(wrapValue(snipp, _L_PAR, _R_PAR));
                }

            } else {

                if (!isLastItem){
                query.append(snipp)
                        .append(_PADDING)
                        .append(AND_OP);

                } else {

                    query.append(_PADDING)
                            .append(snipp);
                }
            }

        }


        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }


    @Override
    public String visitContainsFilter (String  p, ContainsFilter containsFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through CONTAINS filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = containsFilter.getAttribute();

        String snippet = processStringFunction(attr, CONTAINS_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }

    @Override
    public String visitContainsAllValuesFilter (String  p, ContainsAllValuesFilter containsAllValuesFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through CONTAINS ALL VALUES filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = containsAllValuesFilter.getAttribute();

        String snippet = processStringFilter(attr, EQUALS_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();

    }

    @Override
    public String visitEqualsFilter (String  p, EqualsFilter equalsFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through EQUALS filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = equalsFilter.getAttribute();

        String snippet = processStringFilter(attr, EQUALS_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }


    @Override
    public String visitExtendedFilter (String  p, Filter filter) {

        LOG.warn("WARNING: Filter 'EXTENDED FILTER' not implemented by the connector, " +
                "resulting query string will be NULL");

        return null;
    }

    @Override
    public String visitGreaterThanFilter (String  p, GreaterThanFilter greaterThanFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through GREATER THAN FILTER filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = greaterThanFilter.getAttribute();

        String snippet = processStringFilter(attr, GREATER_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }

    @Override
    public String visitGreaterThanOrEqualFilter (String  p, GreaterThanOrEqualFilter greaterThanOrEqualFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through GREATER THAN OR EQUAL FILTER filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = greaterThanOrEqualFilter.getAttribute();

        String snippet = processStringFilter(attr, GREATER_OR_EQUALS_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }

    @Override
    public String visitLessThanFilter (String  p, LessThanFilter lessThanFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through LESS THAN FILTER filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = lessThanFilter.getAttribute();

        String snippet = processStringFilter(attr, LESS_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }

    @Override
    public String visitLessThanOrEqualFilter (String  p, LessThanOrEqualFilter lessThanOrEqualFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through LESS THAN OR EQUAL FILTER filter expression with trailing query part set to: {0}"
                , p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = lessThanOrEqualFilter.getAttribute();

        String snippet = processStringFilter(attr, LESS_OR_EQ_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }

    @Override
    public String visitNotFilter (String  p, NotFilter notFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through NOT filter expression with trailing query part set to: {0}", p);


        StringBuilder query = new StringBuilder(p);

        Filter negatedFilter = notFilter.getFilter();

        Collection<Filter> filters = CollectionUtil.newList(negatedFilter);
        Map<String,Boolean> processed = processCompositeFilter(filters, p);

        if (!processed.isEmpty()) {
            for(String snipp : processed.keySet()){

                    query.append(NOT_OP).append(_PADDING);
                    query.append(wrapValue(snipp,_L_PAR,_R_PAR));
            }
        } else {

            LOG.warn("Invalid filter state, potentially malformed query snippet: {0}", query.toString());
        }

        return query.toString();
    }

    @Override
    public String visitOrFilter (String  p, OrFilter orFilter) {

            p = p != null ? p : "";

            LOG.ok("Processing through OR filter expression with trailing query part set to: {0}", p);

            StringBuilder query = new StringBuilder(p);

            Collection<Filter> filters = orFilter.getFilters();

            Map<String, Boolean> snippets = processCompositeFilter(filters, p);
            Set<String> items = snippets.keySet();
            Iterator<String> keyIterator =items.iterator();

            while (keyIterator.hasNext()){

                String snipp = keyIterator.next();
                Boolean isLastItem = !keyIterator.hasNext();

                if (snippets.get(snipp)){

                    if (!isLastItem){
                        query.append(wrapValue(snipp, _L_PAR, _R_PAR))
                                .append(_PADDING)
                                .append(OR_OP);

                    } else {

                        query.append(_PADDING)
                                .append(wrapValue(snipp, _L_PAR, _R_PAR));
                    }

                } else {

                    if (!isLastItem){
                        query.append(snipp)
                                .append(_PADDING)
                                .append(OR_OP);

                    } else {

                        query.append(_PADDING)
                                .append(snipp);
                    }
                }

            }


            LOG.ok("Generated query snippet: {0}", query);

            return query.toString();
    }

    @Override
    public String visitStartsWithFilter (String  p, StartsWithFilter startsWithFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through STARTS WITH filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = startsWithFilter.getAttribute();

        String snippet = processStringFunction(attr, STARTS_WITH_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }

    @Override
    public String visitEndsWithFilter (String  p, EndsWithFilter endsWithFilter) {

        p = p != null ? p : "";

        LOG.ok("Processing through ENDS WITH filter expression with trailing query part set to: {0}", p);

        StringBuilder query = new StringBuilder(p);

        Attribute attr = endsWithFilter.getAttribute();

        String snippet = processStringFunction(attr, ENDS_WITH_OP);

        query.append(snippet);

        LOG.ok("Generated query snippet: {0}", query);

        return query.toString();
    }

    @Override
    public String visitEqualsIgnoreCaseFilter (String  p, EqualsIgnoreCaseFilter equalsIgnoreCaseFilter) {

        LOG.warn("WARNING: Filter 'EQUALS IGNORE CASE FILTER' not implemented by the connector, " +
                "resulting query string will be NULL");

        return null;
    }

    private String wrapValue(String s){

        s = _VALUE_WRAPPER +s+ _VALUE_WRAPPER;

        return s;
    }

    private String wrapValue(String s, String leftSide, String rightSide){

        s = leftSide+s+rightSide;

        return s;
    }


    private Map<String,Boolean> processCompositeFilter(Collection<Filter> filters, String p) {

        HashMap<String,Boolean> snippets = new HashMap<String, Boolean>();

        for (Filter filter : filters) {

            if (!(filter instanceof CompositeFilter || filter instanceof NotFilter)) {

                snippets.put(filter.accept(this, p), false);
            } else
            {

                snippets.put(filter.accept(this, p), true);
            }

        }

        if(LOG.isOk()){
            LOG.ok("The following snippets were generated by the composite filter evaluation");
            for (String snippet: snippets.keySet()){

                LOG.ok("The query snippet:{0} , child of either composite filter of negation: {1}",snippet
                        , snippets.get(snippet));
            }
        }

        return snippets;
    }


    private String processStringFilter(Attribute attr, String operator) {

        StringBuilder query = new StringBuilder();

        if (attr != null) {
            String singleValue = null;
            String name = attr.getName();
            List value = attr.getValue();

            if (value != null && !value.isEmpty()) {

                singleValue = AttributeUtil.getSingleValue(attr).toString();

            } else {

            }

            query.append(name);
            query.append(_PADDING);
            query.append(operator);
            query.append(_PADDING);
            query.append(wrapValue(singleValue));
        }

        return query.toString();
    }

    private String processStringFunction(Attribute attr, String operator) {

        StringBuilder query = new StringBuilder();

        if (attr != null) {
            String singleValue = null;
            String name = attr.getName();
            List value = attr.getValue();

            if (value != null && !value.isEmpty()) {

                singleValue = AttributeUtil.getSingleValue(attr).toString();

            } else {

            }

            query.append(name);
            query.append(_COL);
            query.append(wrapValue(singleValue));

        }

        return operator +wrapValue(query.toString(), _L_PAR, _R_PAR);

    }
}
