/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geogit.api.RevObject.TYPE;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

/**
 * Provides a filter for sparse repositories. A default filter can be applied to all feature types,
 * and specific filters can be applied to individual feature types.
 */
public class RepositoryFilter {

    private Map<String, Filter> repositoryFilters;

    private List<FilterDescription> filterDescriptions;

    /**
     * Provides a text description of a particular filter.
     */
    public class FilterDescription {
        private String featureType;

        private String filterType;

        private String filter;

        /**
         * Constructs a new {@code FilterDescription} with the provided values.
         * 
         * @param featureType the feature type this filter applies to, use "default" as a fall back
         *        filter
         * @param filterType the type of filter, for example "CQL"
         * @param filter the filter text
         */
        public FilterDescription(String featureType, String filterType, String filter) {
            this.featureType = featureType;
            this.filterType = filterType;
            this.filter = filter;
        }

        /**
         * @return the feature type this filter applies to
         */
        public String getFeatureType() {
            return featureType;
        }

        /**
         * @return the format of the filter
         */
        public String getFilterType() {
            return filterType;
        }

        /**
         * @return the filter in string form
         */
        public String getFilter() {
            return filter;
        }
    }

    /**
     * Constructs a new {@code RepositoryFilter}.
     */
    public RepositoryFilter() {
        repositoryFilters = new HashMap<String, Filter>();
        filterDescriptions = new LinkedList<FilterDescription>();
    }

    /**
     * @return an immutable copy of the filter descriptions
     */
    public ImmutableList<FilterDescription> getFilterDescriptions() {
        return ImmutableList.copyOf(filterDescriptions);
    }

    /**
     * Adds a new filter to the repository.
     * 
     * @param featureType the feature type to filter, "default" for a fall back filter
     * @param filterType the format of the filter text, for example "CQL"
     * @param filterText the filter text
     */
    public void addFilter(String featureType, String filterType, String filterText) {
        Preconditions.checkState(featureType != null && filterType != null && filterText != null,
                "Missing filter parameter.");
        if (filterType.equals("CQL")) {
            try {
                Filter newFilter = CQL.toFilter(filterText);
                repositoryFilters.put(featureType, newFilter);
                filterDescriptions.add(new FilterDescription(featureType, filterType, filterText));
            } catch (CQLException e) {
                Throwables.propagate(e);
            }
        }
    }

    /**
     * Determines if the provided object is filtered in this repository.
     * 
     * @param type the feature type
     * @param object the object to filter
     * @return true if the object lies within the filter, false otherwise
     */
    public boolean filterObject(RevFeatureType type, RevObject object) {
        if (object.getType() == TYPE.FEATURE) {
            RevFeature revFeature = (RevFeature) object;
            FeatureBuilder builder = new FeatureBuilder(type);
            Feature feature = builder.build("TEMP_ID", revFeature);

            Filter typeFilter = repositoryFilters.get(type.getName());
            if (typeFilter == null) {
                typeFilter = repositoryFilters.get("default");
            }
            if (typeFilter.evaluate(feature)) {
                return true;
            }
        }
        return false;
    }
}
