package com.evolveum.polygon.connector.msgraphapi.util;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.common.CollectionUtil;

import java.util.*;

public class FilterHandler implements FilterVisitor<ResourceQuery, ResourceQuery> {

    private static final Log LOG = Log.getLog(FilterHandler.class);


    //Equality operators
    private final static String USERS = "/users";
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
    public ResourceQuery visitAndFilter(ResourceQuery p, AndFilter andFilter) {
        Boolean wasFirst = false;

        conjuctionInitial = !afterFirtsOperation;

        if (conjuctionInitial) {

            wasFirst = true;
        }

        afterFirtsOperation = true;

        LOG.ok("Processing through AND filter expression: {0}.", andFilter);

        Collection<Filter> filters = andFilter.getFilters();

        Map<String, CategorizedFilter> snippets = processCompositeFilter(filters, p);
        Set<String> items = snippets.keySet();
        Iterator<String> keyIterator = items.iterator();

        LOG.ok("Evaluation of AND filter expression: {0}. With previously constructed filter snippet: {1}",
                andFilter, p);
        while (keyIterator.hasNext()) {

            String snipp = keyIterator.next();

            CategorizedFilter categorizedFilter = snippets.get(snipp);
            Boolean isSearch = categorizedFilter.getIsSearch();
            Filter filter = categorizedFilter.getFilter();
                         if (filter instanceof CompositeFilter || filter instanceof ContainsAllValuesFilter) {

                         } else if ( filter instanceof NotFilter && snipp.isEmpty()){

                         } else {

                             if(filter instanceof NotFilter){
                                 isSearch = checkIfFilterOrChildHasSearch(categorizedFilter.getFilter());
                             }

                             if (isSearch) {

                                 String previousSearchSnippet = p.getSearchExpression();
                                 if(previousSearchSnippet!=null && !previousSearchSnippet.isEmpty()){

                                     StringBuilder sb = new StringBuilder();
                                     sb.append(wrapValue(snipp, _L_PAR, _R_PAR));
                                     sb.append(_PADDING);
                                     sb.append(AND_S_OP);
                                     sb.append(_PADDING);
                                     sb.append(wrapValue(previousSearchSnippet, _L_PAR, _R_PAR));

                                     p.setSearchExpression(sb.toString());
                                 } else {

                                     p.setSearchExpression(snipp);
                                 }

                             } else {

                                 String previousFilterSnippet = p.getFilterExpression();
                                 if(previousFilterSnippet!=null && !previousFilterSnippet.isEmpty()){

                                     StringBuilder sb = new StringBuilder();
                                     sb.append(wrapValue(snipp, _L_PAR, _R_PAR));
                                     sb.append(_PADDING);
                                     sb.append(AND_OP);
                                     sb.append(_PADDING);
                                     sb.append(wrapValue(previousFilterSnippet, _L_PAR, _R_PAR));

                                     p.setFilterExpression(sb.toString());

                                 } else {

                                     p.setFilterExpression(snipp);
                                 }
                             }
                         }
        }

        if (wasFirst) {

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        if(containsOpUsed){
            p.setCompositeOrNotUsedInSearch(true);
        } else {
            p.setCompositeOrNotUsedInFilter(true);
        }

        LOG.ok("And filter returned as empty (Aggregated handling)");
        return new ResourceQuery();
    }


    @Override
    public ResourceQuery visitContainsFilter(ResourceQuery p, ContainsFilter containsFilter) {

        if (afterFirtsOperation) {

            checkContainsSearchConditions();
            containsOpUsed = true;
        }

        LOG.ok("Processing through CONTAINS filter expression");


        Attribute attr = containsFilter.getAttribute();

        String snippet = processContainsOperation(attr, p);


        if (!afterFirtsOperation) {
            p.setSearchExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);

        return new ResourceQuery(null , snippet);
    }


    @Override
    public ResourceQuery visitContainsAllValuesFilter(ResourceQuery p, ContainsAllValuesFilter containsAllValuesFilter) {

        LOG.ok("Processing through CONTAINS ALL VALUES filter expression");

//        if (afterFirtsOperation) {
//
//            throw new ConnectorException("Filter 'CONTAINS ALL VALUES' not implemented by the connector for the object " +
//                    "class: "+ p.getObjectClass() + "as a part of a complex filter.");
//        }

        Attribute attr = containsAllValuesFilter.getAttribute();
        String snippet = null;

        if (ObjectClass.GROUP.equals(p.getObjectClass())) {
            final String attributeName = containsAllValuesFilter.getAttribute().getName();
            List<Object> cavL = containsAllValuesFilter.getAttribute().getValue();

            if(cavL!=null && !cavL.isEmpty()){

                Object attributeValue = cavL.get(0);
                if(attributeValue!=null){

                    String pathSegmentFromAttrName;

                    if (attributeName.equals("members")) {
                        pathSegmentFromAttrName = "memberOf";
                    } else {
                        pathSegmentFromAttrName = "ownedObjects";
                    }

                    snippet = USERS + "/" + attributeValue + "/" + pathSegmentFromAttrName + "/microsoft.graph.group";
                }
            } else {

                throw new ConnectorException("Filter 'CONTAINS ALL VALUES' no attribute value present in filter");
            }

        } else {

            throw new ConnectorException("Filter 'CONTAINS ALL VALUES' not implemented by the connector for the object " +
                    "class: "+ p.getObjectClass());
        }

        LOG.ok("Generated query snippet: {0}", snippet);

           p.setIdOrMembershipExpression(snippet);
           p.setUseCount(true);
            return p;

    }

    @Override
    public ResourceQuery visitEqualsFilter(ResourceQuery p, EqualsFilter equalsFilter) {

        LOG.ok("Processing through EQUALS filter expression");

        if (afterFirtsOperation) {

            checkFilterConditions();
        }

        Attribute attr = equalsFilter.getAttribute();

        String snippet = processStringFilter(attr, EQUALS_OP, p);

        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);

        return new ResourceQuery(snippet,null);
    }


    @Override
    public ResourceQuery visitExtendedFilter(ResourceQuery p, Filter filter) {

       throw new ConnectorException("Filter 'EXTENDED FILTER' not implemented by the connector");
    }

    @Override
    public ResourceQuery visitGreaterThanFilter(ResourceQuery p, GreaterThanFilter greaterThanFilter) {

        LOG.ok("Processing through GREATER THAN FILTER filter expression");

        if (afterFirtsOperation) {

            checkFilterConditions();
        }

        Attribute attr = greaterThanFilter.getAttribute();

        String snippet = processStringFilter(attr, GREATER_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return new ResourceQuery(null, snippet);
    }

    @Override
    public ResourceQuery visitGreaterThanOrEqualFilter(ResourceQuery p, GreaterThanOrEqualFilter greaterThanOrEqualFilter) {

        LOG.ok("Processing through GREATER THAN OR EQUAL FILTER filter expression");

        if (afterFirtsOperation) {
            checkFilterConditions();
        }

        Attribute attr = greaterThanOrEqualFilter.getAttribute();

        String snippet = processStringFilter(attr, GREATER_OR_EQUALS_OP, p);

        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return new ResourceQuery(null, snippet);
    }

    @Override
    public ResourceQuery visitLessThanFilter(ResourceQuery p, LessThanFilter lessThanFilter) {

        LOG.ok("Processing through LESS THAN FILTER filter expression");

        if (afterFirtsOperation) {
            checkFilterConditions();
        }

        Attribute attr = lessThanFilter.getAttribute();

        String snippet = processStringFilter(attr, LESS_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return new ResourceQuery(null, snippet);
    }

    @Override
    public ResourceQuery visitLessThanOrEqualFilter(ResourceQuery p, LessThanOrEqualFilter lessThanOrEqualFilter) {

        LOG.ok("Processing through LESS THAN OR EQUAL FILTER filter expression");

        if (afterFirtsOperation) {
            checkFilterConditions();
        }

        Attribute attr = lessThanOrEqualFilter.getAttribute();

        String snippet = processStringFilter(attr, LESS_OR_EQ_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return new ResourceQuery(null, snippet);
    }

    @Override
    public ResourceQuery visitNotFilter(ResourceQuery p, NotFilter notFilter) {

        LOG.ok("Processing through NOT filter expression {0}", notFilter);

        Boolean wasFirst = !afterFirtsOperation;

        negationInitial = wasFirst;

        afterFirtsOperation = true;

        p.setUseCount(true);

        Filter negatedFilter = notFilter.getFilter();
        StringBuilder query = new StringBuilder();

        Collection<Filter> filters = CollectionUtil.newList(negatedFilter);
        Map<String, CategorizedFilter> processed = processCompositeFilter(filters, p);

        LOG.ok("Evaluating NOT filter expression {0}. With previously constructed filter snippet: {1}",
                notFilter, p);

        if (!processed.isEmpty()) {
            for (String snipp : processed.keySet()) {

                CategorizedFilter filter = processed.get(snipp);
                Boolean hasSearch = filter.getIsSearch();

                if (filter.getFilter() instanceof CompositeFilter || filter.getFilter() instanceof NotFilter) {

                    boolean hasOtherThanSearch = checkIfFilterOrChildHasOtherThanSearch(filter.getFilter());

                    if (!hasSearch) {

                        hasSearch = checkIfFilterOrChildHasSearch(filter.getFilter());
                    }

                    if (hasOtherThanSearch) {

                        query = new StringBuilder();
                        query.append(NOT_OP).append(_PADDING);
                        query.append(wrapValue(p.getFilterExpression(), _L_PAR, _R_PAR));
                        p.setFilterExpression(query.toString());
                    }

                    if (hasSearch) {

                        query = new StringBuilder();
                        query.append(NOT_S_OP).append(_PADDING);
                        query.append(wrapValue(p.getSearchExpression(), _L_PAR, _R_PAR));
                        p.setSearchExpression(query.toString());
                    }

                    return new ResourceQuery();
                } else {

                    if (!hasSearch) {

                        query.append(NOT_OP).append(_PADDING);
                        query.append(wrapValue(snipp, _L_PAR, _R_PAR));

                        if(wasFirst){

                            p.setFilterExpression(query.toString());
                        }

                    } else {

                        query.append(NOT_S_OP).append(_PADDING);
                        query.append(wrapValue(snipp, _L_PAR, _R_PAR));

                        if(wasFirst){

                            p.setSearchExpression(query.toString());
                        }
                    }
                }
            }
        } else {
            LOG.warn("Invalid filter state, potentially malformed query snippet: {0}", query);
        }

          if (wasFirst) {

            p.setFilterExpression(query.toString());

            LOG.ok("Generated final query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", query);

        if(containsOpUsed){
            p.setCompositeOrNotUsedInSearch(true);

            return new ResourceQuery(null, query.toString());
        } else {
            p.setCompositeOrNotUsedInFilter(true);

            return new ResourceQuery(query.toString(), null);
        }

    }

    @Override
    public ResourceQuery visitOrFilter(ResourceQuery p, OrFilter orFilter) {

        Boolean isSearch = checkIfFilterOrChildHasSearch(orFilter);

       if(checkIfFilterOrChildHasOtherThanSearch(orFilter) && isSearch){


           new ConnectorException("Invalid filter combination, conjunction of other filters with contains queries " +
                   "supported only with contains filter as a left or right side of the first 'AND' filter clause. " +
                   "Please see documentation");
       }

        Boolean wasFirst = !afterFirtsOperation;

        orInitial = wasFirst;

        afterFirtsOperation = true;

        LOG.ok("Processing through OR filter expression: {0}.", orFilter);

        Collection<Filter> filters = orFilter.getFilters();

        Map<String, CategorizedFilter> snippets = processCompositeFilter(filters, p);
        Set<String> items = snippets.keySet();
        Iterator<String> keyIterator = items.iterator();

        LOG.ok("Evaluation of OR filter expression: {0}. With previously constructed filter snippet: {1}",
                orFilter, p);

        while (keyIterator.hasNext()) {

            String snipp = keyIterator.next();

            CategorizedFilter filter = snippets.get(snipp);

            if (filter.getFilter() instanceof CompositeFilter) {

            } else if ( filter.getFilter() instanceof NotFilter && snipp.isEmpty()){

            } else {

                if(filter.getFilter() instanceof NotFilter){
                    isSearch = checkIfFilterOrChildHasSearch(filter.getFilter());
                }

                    if (isSearch) {
                        String previousSearchSnippet = p.getSearchExpression();

                        if(previousSearchSnippet!=null && !previousSearchSnippet.isEmpty()){


                            StringBuilder sb = new StringBuilder();

                            sb.append(wrapValue(snipp, _L_PAR, _R_PAR));
                            sb.append(_PADDING);
                            sb.append(OR_S_OP);
                            sb.append(_PADDING);
                            sb.append(wrapValue(previousSearchSnippet, _L_PAR, _R_PAR));

                            p.setSearchExpression(sb.toString());
                        } else {
                            p.setSearchExpression(snipp);
                        }

                    } else {

                        String previousFilterSnippet = p.getFilterExpression();
                        if(previousFilterSnippet!=null && !previousFilterSnippet.isEmpty()) {

                            StringBuilder sb = new StringBuilder();
                            sb.append(wrapValue(snipp, _L_PAR, _R_PAR));
                            sb.append(_PADDING);
                            sb.append(OR_OP);
                            sb.append(_PADDING);
                            sb.append(wrapValue(previousFilterSnippet, _L_PAR, _R_PAR));

                            p.setFilterExpression(sb.toString());
                        } else {

                            p.setFilterExpression(snipp);
                        }
                    }
            }
        }

        if (wasFirst) {
            LOG.ok("Generated query snippet for OR OP used: {0}", p);
            return p;
        }

        if(containsOpUsed){
            p.setCompositeOrNotUsedInSearch(true);
        } else {
            p.setCompositeOrNotUsedInFilter(true);
        }

        return new ResourceQuery();
    }

    @Override
    public ResourceQuery visitStartsWithFilter(ResourceQuery p, StartsWithFilter startsWithFilter) {

        LOG.ok("Processing through STARTS WITH filter expression");

        if (!afterFirtsOperation) {

            checkFilterConditions();
        }
        Attribute attr = startsWithFilter.getAttribute();

        String snippet = processStringFunction(attr, STARTS_WITH_OP, p);


        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return new ResourceQuery(snippet, null);
    }

    @Override
    public ResourceQuery visitEndsWithFilter(ResourceQuery p, EndsWithFilter endsWithFilter) {
        LOG.ok("Processing through ENDS WITH filter expression");

        if (afterFirtsOperation) {

            checkFilterConditions();
        }

        Attribute attr = endsWithFilter.getAttribute();

        String snippet = processStringFunction(attr, ENDS_WITH_OP, p);

        p.setUseCount(true);

        if (!afterFirtsOperation) {

            p.setFilterExpression(snippet);

            LOG.ok("Generated query snippet: {0}", p);
            return p;
        }

        LOG.ok("Generated query snippet: {0}", snippet);
        return new ResourceQuery(snippet, null);
    }

    @Override
    public ResourceQuery visitEqualsIgnoreCaseFilter(ResourceQuery p, EqualsIgnoreCaseFilter equalsIgnoreCaseFilter) {

        throw new ConnectorException("Filter 'EQUALS IGNORE CASE FILTER' not implemented by the connector. ");

    }

    private String wrapValue(String s) {
        // Escape single quote
        s = s.replace("'", "''");
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

            String query =  filter.accept(this, p).fetchSnippet();
            snippets.put(query, new CategorizedFilter(filter, query));

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
            // Escape double quote
            query.append(singleValue.replace("\"", "\\\""));

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


    private Boolean checkIfFilterOrChildHasOtherThanSearch(Filter filter) {
        if (filter instanceof AttributeFilter && !(filter instanceof ContainsFilter)) {

            return true;
        }

        if (filter instanceof CompositeFilter) {

            Iterator<Filter> iterator = ((CompositeFilter) filter).getFilters().iterator();

            while (iterator.hasNext()) {

                if (checkIfFilterOrChildHasOtherThanSearch((iterator.next()))) {

                    return true;
                }

            }

        }

        if (filter instanceof NotFilter) {

            return checkIfFilterOrChildHasOtherThanSearch(((NotFilter) filter).getFilter());
        }

        return false;
    }

}
