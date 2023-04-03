package com.evolveum.polygon.connector.msgraphapi.util;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.ObjectClass;

public class ResourceQuery {

    private static final Log LOG = Log.getLog(ResourceQuery.class);
    private ResourceQuery aggregate;
    private ObjectClass objectClass;

    private String objectClassUidName;

    private String objectClassNameName;

    private String searchExpression;
    private String filterExpression;
    private Boolean useCount = false;

    private static Boolean compositeOrNotUsedInSearch= false;

    private static Boolean compositeOrNotUsedInFilter= false;
    private static final String $_SEARCH = "$search=";

    private static final String $_FILTER = "$filter=";

    private static final String $_COUNT = "&$count=true";

    private static final String _AMP = "&";


    public ResourceQuery(ObjectClass objectClass, String objectClassUidName, String objectClassNameName){

        this.objectClass = objectClass;
        this.objectClassUidName = objectClassUidName;
        this.objectClassNameName = objectClassNameName;
    }

//    public ResourceQuery(ResourceQuery aggregate){
//        this.objectClass = aggregate.getObjectClass();
//        this.objectClassUidName = aggregate.getObjectClassUidName();
//        this.objectClassNameName = aggregate.getObjectClassNameName();
//        this.useCount = aggregate.useCount;
//        this.aggregate = aggregate;
//    }

    public String getSearchExpression() {
        return searchExpression !=null ? searchExpression:"";
    }

    public void setSearchExpression(String searchExpression) {
        this.searchExpression = searchExpression;
    }


    public String getFilterExpression() {
        return filterExpression !=null ? filterExpression:"";
    }

    public void setFilterExpression(String searchExpression) {
        this.filterExpression = searchExpression;
    }

    public ObjectClass getObjectClass() {
        return objectClass;
    }

    public String getObjectClassUidName() {
        return objectClassUidName;
    }

    public String getObjectClassNameName() {
        return objectClassNameName;
    }

    public boolean isEmpty() {

        return  !(filterExpression!=null && !filterExpression.isEmpty()) && !(searchExpression!=null &&
                !searchExpression.isEmpty());
    }

    private String appendCount(){

        if(useCount){

            return $_COUNT;
        }
        return "";
    }

    public void setUseCount(Boolean useCount) {
        this.useCount = useCount;
    }


    @Override
    public String toString() {

     //   evaluateAggregated();

        if(!(filterExpression!=null && !filterExpression.isEmpty()) && !(searchExpression!=null &&
                !searchExpression.isEmpty()) ){

            return null;
        }

        if((filterExpression!=null && !filterExpression.isEmpty()) && (searchExpression!=null &&
                !searchExpression.isEmpty()) ){

            return $_FILTER+ filterExpression+ _AMP+ $_SEARCH+ searchExpression;
        }

        if (filterExpression!=null && !filterExpression.isEmpty()){

            return $_FILTER+ filterExpression + appendCount();
        }

        return $_SEARCH+ searchExpression;
    }

    private void evaluateAggregated() {

        LOG.ok("Evaluating aggregated query snippets");
        if (aggregate !=null ){

            if(aggregate.hasAggregate()){

                aggregate.evaluateAggregated();
            }
            LOG.ok("Evaluating aggregated query snippets, #: {0}", aggregate.getSearchExpression());

               searchExpression = aggregate.getSearchExpression() + " " +searchExpression;

        }
    }

    public boolean hasAggregate(){
        return aggregate!=null;
    }

    public void setCompositeOrNotUsedInSearch(Boolean compositeOrNotUsedInSearch) {
        this.compositeOrNotUsedInSearch = compositeOrNotUsedInSearch;
    }

    public void setCompositeOrNotUsedInFilter(Boolean compositeOrNotUsedInFilter) {
        this.compositeOrNotUsedInFilter = compositeOrNotUsedInFilter;
    }
}
