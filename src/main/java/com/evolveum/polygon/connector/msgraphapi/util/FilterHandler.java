package com.evolveum.polygon.connector.msgraphapi.util;

import com.evolveum.polygon.connector.msgraphapi.UserProcessing;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.common.CollectionUtil;

import java.util.*;

public class FilterHandler implements FilterVisitor<String, ResourceQuery> {

    private static final Log LOG = Log.getLog(FilterHandler.class);


    //Equality operators

    private static final String EQUALS_OP = "eq";
    private static final String NOT_EQUAL_OP = "ne";
    private static final String NOT_OP = "not";

    private static final String NOT_S_OP = "NOT";
    private static final String IN_OP = "in";
    private static final String HA_OP = "has";

    // Relational operators

    private static final String LESS_OP = "lt";

    private static final String GREATER_OP = "gt";

    private static final String LESS_OR_EQ_OP = "le";

    private static final String GREATER_OR_EQUALS_OP = "ge";

    // Lambda operators

    private static final String ANY_OP = "any";

    private static final String ALL_OP = "all";


    // Conditional operators

    private static final String AND_OP = "and";

    private static final String AND_S_OP = "AND";

    private static final String OR_OP = "or";

    private static final String OR_S_OP = "OR";

    // Functions

    private static final String STARTS_WITH_OP = "startsWith";

    private static final String ENDS_WITH_OP = "endsWith";

    private static final String CONTAINS_OP = "contains";

    // DELIMITER

    private static final String _L_PAR = "(";
    private static final String _R_PAR = ")";
    private static final String _COL = ",";
    private static final String _ASTERISK = "*";

    private static final String _SLASH = "/";

    private static final String _VALUE_WRAPPER = "'";

    private static final String _EXP_WRAPPER = "\"";

    private static final String _PADDING = " ";

    private Boolean afterFirtsOperation = false;

    private Boolean conjuctionInitial = false;

    private Boolean orInitial = false;

    private Boolean negationInitial = false;
    private Boolean filteringOpUsed = false;

    private Boolean containsOpUsed = false;


    @Override
    public String visitAndFilter(ResourceQuery p, AndFilter andFilter) {
        Boolean wasFirst = false;

        conjuctionInitial = !afterFirtsOperation;

        if (conjuctionInitial) {

            wasFirst = true;
        }

        afterFirtsOperation = true;

        LOG.ok("Processing through AND filter expression");

        StringBuilder query = new StringBuilder();

        Collection<Filter> filters = andFilter.getFilters();

        Map<String, CategorizedFilter> snippets = processCompositeFilter(filters, p);
        Set<String> items = snippets.keySet();
        Iterator<String> keyIterator = items.iterator();

        String leftSideSnippet = null;
        Boolean wasSearch = false;

        while (keyIterator.hasNext()) {

            String snipp = keyIterator.next();
            Boolean isLastItem = !keyIterator.hasNext();

            CategorizedFilter filter = snippets.get(snipp);
            Boolean isSearch = filter.getIsSearch();

            if (filter.getFilter() instanceof CompositeFilter || filter.getFilter() instanceof NotFilter) {

                if (!isSearch) {

                    isSearch = checkIfFilterOrChildHasSearch(filter.getFilter());
                }

                // if (!wasFirst || (wasFirst && !containsOpUsed)) {

                if (!isLastItem) {
                    query.append(wrapValue(snipp, _L_PAR, _R_PAR))
                            .append(_PADDING);

                    if (!isSearch) {

                        query.append(AND_OP);
                    } else {

                        query.append(AND_S_OP);
                    }

                } else {

                    query.append(_PADDING)
                            .append(wrapValue(snipp, _L_PAR, _R_PAR));
                }
                // } else {
                if (leftSideSnippet != null) {

                    if ((wasSearch != isSearch) && wasFirst) {

                        if (!isSearch) {

                            p.setFilterExpression(snipp);
                            p.setSearchExpression(leftSideSnippet);
                        } else {

                            p.setSearchExpression(snipp);
                            p.setFilterExpression(leftSideSnippet);
                        }
                    } else if (wasFirst) {

                        if (isSearch) {

                            p.setSearchExpression(query.toString());
                        } else {

                            p.setFilterExpression(query.toString());
                        }
                    } else if ((wasSearch != isSearch) && !wasFirst){

                        LOG.warn("Invalid filter combination, conjunction of other filters supported only with " +
                                "contains filter as a left or right side of the first 'AND' filter clause. Please see documentation");
                    }
                } else {

                    leftSideSnippet = snipp;
                }
                //}

            } else {

                //if (!wasFirst || (wasFirst && !containsOpUsed)) {
                if (!isLastItem) {
                    query.append(snipp)
                            .append(_PADDING);

                    if (!isSearch) {

                        query.append(AND_OP);
                    } else {

                        query.append(AND_S_OP);
                    }

                } else {

                    query.append(_PADDING)
                            .append(snipp);
                }

                // } else {

                if (leftSideSnippet != null) {

                    if ((wasSearch != isSearch) && wasFirst) {

                        if (!isSearch) {

                            p.setFilterExpression(snipp);
                            p.setSearchExpression(leftSideSnippet);
                        } else {

                            p.setSearchExpression(snipp);
                            p.setFilterExpression(leftSideSnippet);
                        }
                    } else if (wasFirst) {

                        if (isSearch) {

                            p.setSearchExpression(query.toString());
                        } else {

                            p.setFilterExpression(query.toString());
                        }
                    } else if ((wasSearch != isSearch) && !wasFirst){

                        LOG.warn("Invalid filter combination, conjunction of other filters supported only with " +
                                "contains filter as a left or right side of the first 'AND' filter clause. Please see documentation");
                    }
                } else {

                    leftSideSnippet = snipp;
                }
            }
            wasSearch = isSearch;
        }

        // }

        if (wasFirst) {

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", query);
        return query.toString();
    }


    @Override
    public String visitContainsFilter(ResourceQuery p, ContainsFilter containsFilter) {

        if (afterFirtsOperation) {

            checkContainsSearchConditions();
            containsOpUsed = true;
        }

        LOG.ok("Processing through CONTAINS filter expression");


        Attribute attr = containsFilter.getAttribute();

        String snippet = processContainsOperation(attr, p);


        if (!afterFirtsOperation) {
            p.setSearchExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }


    @Override
    public String visitContainsAllValuesFilter(ResourceQuery p, ContainsAllValuesFilter containsAllValuesFilter) {

/*        LOG.ok("Processing through CONTAINS ALL VALUES filter expression");

        if (afterFirtsOperation) {

            checkFilterConditions();
        }

        Attribute attr = containsAllValuesFilter.getAttribute();

        if (ObjectClass.GROUP.equals(p.getObjectClass())){

//TODO
        } else if (ObjectClass.ACCOUNT.equals(p.getObjectClass())){

//TODO
        }


        String snippet = processAnyFilter(attr, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;*/

        LOG.warn("WARNING: Filter 'CONTAINS ALL VALUES' not implemented by the connector for the object class: {0}, " +
                "resulting query string will be NULL", p.getObjectClass());

        return null;

    }

    @Override
    public String visitEqualsFilter(ResourceQuery p, EqualsFilter equalsFilter) {

        LOG.ok("Processing through EQUALS filter expression");

        if (afterFirtsOperation) {

            checkFilterConditions();
        }

        Attribute attr = equalsFilter.getAttribute();

        String snippet = processStringFilter(attr, EQUALS_OP, p);

        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }


    @Override
    public String visitExtendedFilter(ResourceQuery p, Filter filter) {

        LOG.warn("WARNING: Filter 'EXTENDED FILTER' not implemented by the connector, " +
                "resulting query string will be NULL");

        return null;
    }

    @Override
    public String visitGreaterThanFilter(ResourceQuery p, GreaterThanFilter greaterThanFilter) {

        LOG.ok("Processing through GREATER THAN FILTER filter expression");

        if (afterFirtsOperation) {

            checkFilterConditions();
        }

        Attribute attr = greaterThanFilter.getAttribute();

        String snippet = processStringFilter(attr, GREATER_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }

    @Override
    public String visitGreaterThanOrEqualFilter(ResourceQuery p, GreaterThanOrEqualFilter greaterThanOrEqualFilter) {

        LOG.ok("Processing through GREATER THAN OR EQUAL FILTER filter expression");

        if (afterFirtsOperation) {
            checkFilterConditions();
        }

        Attribute attr = greaterThanOrEqualFilter.getAttribute();

        String snippet = processStringFilter(attr, GREATER_OR_EQUALS_OP, p);

        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }

    @Override
    public String visitLessThanFilter(ResourceQuery p, LessThanFilter lessThanFilter) {

        LOG.ok("Processing through LESS THAN FILTER filter expression");

        if (afterFirtsOperation) {
            checkFilterConditions();
        }

        Attribute attr = lessThanFilter.getAttribute();

        String snippet = processStringFilter(attr, LESS_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }

    @Override
    public String visitLessThanOrEqualFilter(ResourceQuery p, LessThanOrEqualFilter lessThanOrEqualFilter) {

        LOG.ok("Processing through LESS THAN OR EQUAL FILTER filter expression"
                , p);

        if (afterFirtsOperation) {
            checkFilterConditions();
        }

        Attribute attr = lessThanOrEqualFilter.getAttribute();

        String snippet = processStringFilter(attr, LESS_OR_EQ_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }

    @Override
    public String visitNotFilter(ResourceQuery p, NotFilter notFilter) {

        LOG.ok("Processing through NOT filter expression");

        Boolean wasFirst = !afterFirtsOperation;

        negationInitial = wasFirst;

        afterFirtsOperation = true;

        p.setUseCount(true);

        Filter negatedFilter = notFilter.getFilter();
        StringBuilder query = new StringBuilder();

        Collection<Filter> filters = CollectionUtil.newList(negatedFilter);
        Map<String, CategorizedFilter> processed = processCompositeFilter(filters, p);

        if (!processed.isEmpty()) {
            for (String snipp : processed.keySet()) {

                CategorizedFilter filter = processed.get(snipp);
                Boolean isSearch = filter.getIsSearch();

                if (filter.getFilter() instanceof CompositeFilter || filter.getFilter() instanceof NotFilter) {

                    if (!isSearch) {

                        isSearch = checkIfFilterOrChildHasSearch(filter.getFilter());
                    }

                    if (!isSearch) {

                        query.append(NOT_OP).append(_PADDING);

                    } else {

                        query.append(NOT_S_OP).append(_PADDING);
                    }
                    query.append(wrapValue(snipp, _L_PAR, _R_PAR));
                } else {

                    if (!isSearch) {

                        query.append(NOT_OP).append(_PADDING);

                    } else {

                        query.append(NOT_S_OP).append(_PADDING);
                    }

                    query.append(wrapValue(snipp, _L_PAR, _R_PAR));
                }


            }
        } else {

            LOG.warn("Invalid filter state, potentially malformed query snippet: {0}", query.toString());
        }

        if (wasFirst && containsOpUsed) {
            p.setSearchExpression(query.toString());

            LOG.ok("Generated query snippet for negated contains operation: {0}", p.toString());
            return p.toString();

        } else if (wasFirst) {

            p.setFilterExpression(query.toString());

            LOG.ok("Generated final query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", query);
        return query.toString();
    }

    @Override
    public String visitOrFilter(ResourceQuery p, OrFilter orFilter) {

        Boolean wasFirst = !afterFirtsOperation;

        orInitial = wasFirst;

        afterFirtsOperation = true;

        LOG.ok("Processing through OR filter expression");

        StringBuilder query = new StringBuilder();

        Collection<Filter> filters = orFilter.getFilters();

        Map<String, CategorizedFilter> snippets = processCompositeFilter(filters, p);
        Set<String> items = snippets.keySet();
        Iterator<String> keyIterator = items.iterator();

        while (keyIterator.hasNext()) {

            String snipp = keyIterator.next();
            Boolean isLastItem = !keyIterator.hasNext();

            CategorizedFilter filter = snippets.get(snipp);
            Boolean isSearch = filter.getIsSearch();


            if (filter.getFilter() instanceof CompositeFilter || filter.getFilter() instanceof NotFilter) {

                if (!isSearch) {

                    isSearch = checkIfFilterOrChildHasSearch(filter.getFilter());
                }


                if (!isLastItem) {
                    query.append(wrapValue(snipp, _L_PAR, _R_PAR))
                            .append(_PADDING);

                    if (isSearch) {

                        query.append(OR_S_OP);

                    } else {

                        query.append(OR_OP);
                    }
                } else {

                    query.append(_PADDING)
                            .append(wrapValue(snipp, _L_PAR, _R_PAR));
                }

            } else {

                if (!isLastItem) {
                    query.append(snipp)
                            .append(_PADDING);

                    if (isSearch) {

                        query.append(OR_S_OP);

                    } else {

                        query.append(OR_OP);
                    }

                } else {

                    query.append(_PADDING)
                            .append(snipp);
                }
            }

        }

        if (wasFirst && containsOpUsed) {

            p.setSearchExpression(query.toString());

            LOG.ok("Generated query snippet for OR with contains OP used: {0}", p.toString());
            return p.toString();
        } else if (wasFirst) {

            p.setFilterExpression(query.toString());

            LOG.ok("Generated final query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", query);
        return query.toString();
    }

    @Override
    public String visitStartsWithFilter(ResourceQuery p, StartsWithFilter startsWithFilter) {

        LOG.ok("Processing through STARTS WITH filter expression");

        if (!afterFirtsOperation) {

            checkFilterConditions();
        }
        Attribute attr = startsWithFilter.getAttribute();

        String snippet = processStringFunction(attr, STARTS_WITH_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }

    @Override
    public String visitEndsWithFilter(ResourceQuery p, EndsWithFilter endsWithFilter) {
        LOG.ok("Processing through ENDS WITH filter expression");

        if (afterFirtsOperation) {

            checkFilterConditions();
        }

        Attribute attr = endsWithFilter.getAttribute();

        String snippet = processStringFunction(attr, ENDS_WITH_OP, p);

        p.setUseCount(true);

        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p.toString());
            return p.toString();
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return snippet;
    }

    @Override
    public String visitEqualsIgnoreCaseFilter(ResourceQuery p, EqualsIgnoreCaseFilter equalsIgnoreCaseFilter) {

        LOG.warn("WARNING: Filter 'EQUALS IGNORE CASE FILTER' not implemented by the connector, " +
                "resulting query string will be NULL");

        return null;
    }

    private String wrapValue(String s) {

        s = _VALUE_WRAPPER + s + _VALUE_WRAPPER;

        return s;
    }

    private String wrapValue(String s, String leftSide, String rightSide) {

        s = leftSide + s + rightSide;

        return s;
    }


    private Map<String, CategorizedFilter> processCompositeFilter(Collection<Filter> filters, ResourceQuery p) {

        HashMap<String, CategorizedFilter> snippets = new HashMap<>();

        for (Filter filter : filters) {

            snippets.put(filter.accept(this, p), new CategorizedFilter(filter));

        }

        if (LOG.isOk()) {
            LOG.ok("The following snippets were generated by the composite filter evaluation");
            for (String snippet : snippets.keySet()) {

                LOG.ok("The query snippet:{0} , child of either composite filter of negation: {1}", snippet
                        , snippets.get(snippet).getFilter());
            }
        }

        return snippets;
    }


    private String processStringFilter(Attribute attr, String operator, ResourceQuery resourceQuery) {

        StringBuilder query = new StringBuilder();

        List<String> nonWrapped = CollectionUtil.newList(GREATER_OR_EQUALS_OP, GREATER_OP, LESS_OP, LESS_OR_EQ_OP);


        if (attr != null) {
            String singleValue = null;
            String name = attr.getName();
            List value = attr.getValue();

            if (Uid.NAME.equals(name)) {

                name = resourceQuery.getObjectClassUidName();
            }

            if (Name.NAME.equals(name)) {

                name = resourceQuery.getObjectClassNameName();
            }

            if (value != null && !value.isEmpty()) {

                singleValue = AttributeUtil.getSingleValue(attr).toString();

            } else {

            }

            query.append(name);
            query.append(_PADDING);
            query.append(operator);
            query.append(_PADDING);
            if (!nonWrapped.contains(operator)) {

                query.append(wrapValue(singleValue));
            } else {

                query.append(singleValue);
            }
        }

        return query.toString();
    }

    private String processAnyFilter(Attribute attr, ResourceQuery resourceQuery) {

        StringBuilder query = new StringBuilder();

        if (attr != null) {
            String singleValue = null;
            String name = attr.getName();
            List value = attr.getValue();

            if (Uid.NAME.equals(name)) {

                name = resourceQuery.getObjectClassUidName();
            }

            if (Name.NAME.equals(name)) {

                name = resourceQuery.getObjectClassNameName();
            }

            if (value != null && !value.isEmpty()) {

                singleValue = AttributeUtil.getSingleValue(attr).toString();

            } else {

            }

            query.append("i:i");
            query.append(_PADDING);
            query.append(EQUALS_OP);
            query.append(_PADDING);
            query.append(wrapValue(singleValue));

            String part = wrapValue(query.toString(), _L_PAR, _R_PAR);

            query = new StringBuilder(name);
            query.append(_SLASH);
            query.append(ANY_OP);
            query.append(part);
        }

        return query.toString();
    }

    private String processStringFunction(Attribute attr, String operator, ResourceQuery resourceQuery) {

        StringBuilder query = new StringBuilder();

        if (attr != null) {
            String singleValue = null;
            String name = attr.getName();
            List value = attr.getValue();

            if (Uid.NAME.equals(name)) {

                name = resourceQuery.getObjectClassUidName();
            }

            if (Name.NAME.equals(name)) {

                name = resourceQuery.getObjectClassNameName();
            }

            if (value != null && !value.isEmpty()) {

                singleValue = AttributeUtil.getSingleValue(attr).toString();

            } else {

            }

            query.append(name);
            query.append(_COL);
            query.append(wrapValue(singleValue));

        }

        return operator + wrapValue(query.toString(), _L_PAR, _R_PAR);

    }

    private String processContainsOperation(Attribute attr, ResourceQuery resourceQuery) {

        StringBuilder query = new StringBuilder();


        if (attr != null) {
            String singleValue = null;
            String name = attr.getName();
            List value = attr.getValue();

            if (Uid.NAME.equals(name)) {

                name = resourceQuery.getObjectClassUidName();
            }

            if (Name.NAME.equals(name)) {

                name = resourceQuery.getObjectClassNameName();
            }

            if (value != null && !value.isEmpty()) {

                singleValue = AttributeUtil.getSingleValue(attr).toString();

            } else {

            }

            query.append(name);
            query.append(":");
            query.append(singleValue);

        }

        return wrapValue(query.toString(), _EXP_WRAPPER, _EXP_WRAPPER);
    }

    private void checkContainsSearchConditions() {

        if (filteringOpUsed && !conjuctionInitial) {

            LOG.warn("Invalid filter combination, conjunction of other filters supported only with " +
                    "contains filter as a left or right side of the first 'AND' filter clause. Please see documentation");
        }

        if (filteringOpUsed && negationInitial) {

            LOG.warn("Invalid filter combination, conjunction of other filters supported only with " +
                    "contains filter as a left or right side of the first 'AND' filter clause. Please see documentation");
        }

        if (filteringOpUsed && orInitial) {

            LOG.warn("Invalid filter combination, conjunction of other filters supported only with " +
                    "contains filter as a left or right side of the first 'AND' filter clause. Please see documentation");
        }

        containsOpUsed = true;
    }

    private void checkFilterConditions() {

        if (containsOpUsed && orInitial) {

            LOG.warn("Invalid filter combination, conjunction of other filters supported only with " +
                    "contains filter as a left or right side of the first 'AND' filter clause. Please see documentation");
        }

        if (containsOpUsed && negationInitial) {

            LOG.warn("Invalid filter combination, conjunction of other filters supported only with " +
                    "contains filter as a left or right side of the first 'AND' filter clause. Please see documentation");
        }

        filteringOpUsed = true;

    }

    private Boolean checkIfFilterOrChildHasSearch(Filter filter) {
        if (filter instanceof ContainsFilter) {

            return true;
        }

        if (filter instanceof CompositeFilter) {

            Iterator<Filter> iterator = ((CompositeFilter) filter).getFilters().iterator();

            while (iterator.hasNext()) {

                if (checkIfFilterOrChildHasSearch((iterator.next()))) {

                    return true;
                }

            }

        }

        if (filter instanceof NotFilter) {

            return checkIfFilterOrChildHasSearch(((NotFilter) filter).getFilter());

        }

        return false;
    }

}
