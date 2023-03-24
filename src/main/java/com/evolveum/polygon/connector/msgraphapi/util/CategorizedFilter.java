package com.evolveum.polygon.connector.msgraphapi.util;

import org.identityconnectors.framework.common.objects.filter.CompositeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;

public class CategorizedFilter {

    private Filter filter;
    private Boolean isSearch;


    public CategorizedFilter(Filter filter, Boolean isSearch) {
        this.filter = filter;
        this.isSearch = isSearch;
    }

    public CategorizedFilter(Filter filter) {
        this.filter = filter;
        this.isSearch = checkIfFilterIsSearch(filter);
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
}
