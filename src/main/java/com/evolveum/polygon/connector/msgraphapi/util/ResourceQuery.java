package com.evolveum.polygon.connector.msgraphapi.util;

import org.identityconnectors.framework.common.objects.ObjectClass;

public class ResourceQuery {


    ObjectClass objectClass;

    private String objectClassUidName;

    private String objectClassNameName;

    private String searchExpression;
    private String filterExpression;
    private Boolean hasSearchAndFilter=false;
    private static final String $_SEARCH = "$search";

    private static final String $_FILTER = "$filter";

    private static final String _AMP = "&";


    public ResourceQuery(ObjectClass objectClass, String objectClassUidName, String objectClassNameName){

        this.objectClass = objectClass;
        this.objectClassUidName = objectClassUidName;
        this.objectClassNameName = objectClassNameName;
    }

    public String getSearchExpression() {
        return searchExpression;
    }

    public void setSearchExpression(String searchExpression) {
        this.searchExpression = searchExpression;
    }


    public String getFilterExpression() {
        return filterExpression;
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

    public Boolean getHasSearchAndFilter() {
        return hasSearchAndFilter;
    }

    @Override
    public String toString() {

        if(!(filterExpression!=null && !filterExpression.isEmpty()) && !(searchExpression!=null &&
                !searchExpression.isEmpty()) ){

            return null;
        }

        if((filterExpression!=null && !filterExpression.isEmpty()) && (searchExpression!=null &&
                !searchExpression.isEmpty()) ){

            return $_FILTER+ filterExpression+ _AMP+ $_SEARCH+ searchExpression;
        }

        if (filterExpression!=null && !filterExpression.isEmpty()){

            return $_FILTER+ filterExpression;
        }

        return $_SEARCH+ searchExpression;
    }
}
