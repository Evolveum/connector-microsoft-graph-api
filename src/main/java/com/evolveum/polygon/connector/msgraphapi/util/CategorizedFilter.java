package com.evolveum.polygon.connector.msgraphapi.util;

import org.identityconnectors.framework.common.objects.filter.CompositeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;

public class CategorizedFilter {

    private Filter filter;
    private Boolean isSearch;

    private String translatedQueryPart;

    public CategorizedFilter(Filter filter, String translatedQueryPart, Boolean isSearch) {
        this.filter = filter;
        this.isSearch = isSearch;
        this.translatedQueryPart = translatedQueryPart;
    }

    public CategorizedFilter(Filter filter, String translatedQueryPart) {
        this.filter = filter;
        this.isSearch = checkIfFilterIsSearch(filter);
        this.translatedQueryPart = translatedQueryPart;
    }

    private Boolean checkIfFilterIsSearch(Filter filter) {

        if (filter instanceof ContainsFilter) {

            return true;
        }

        if (filter instanceof CompositeFilter) {

            for (Filter f : ((CompositeFilter) filter).getFilters()) {

                if (f instanceof ContainsFilter) {

                    return true;
                }
            }

            return false;

        } else if (filter instanceof NotFilter) {

            if (((NotFilter) filter).getFilter() instanceof CompositeFilter) {

                return true;
            }

            return false;
        }

        return false;
    }

    public Filter getFilter() {
        return filter;
    }

    public Boolean getIsSearch() {
        return isSearch;
    }

    public String getTranslatedQueryPart() {
        return translatedQueryPart;
    }
}

