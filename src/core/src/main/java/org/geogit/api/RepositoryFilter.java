package org.geogit.api;

import java.util.HashMap;
import java.util.Map;

import org.geogit.api.RevObject.TYPE;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class RepositoryFilter {

    public Map<String, Filter> repositoryFilters;

    public Map<String, Pair<String, String>> readableRepositoryFilters;

    public RepositoryFilter() {
        repositoryFilters = new HashMap<String, Filter>();
        readableRepositoryFilters = new HashMap<String, Pair<String, String>>();
    }

    public void addFilter(String featureType, String filterType, String filterText) {
        Preconditions.checkState(featureType != null && filterType != null && filterText != null,
                "Missing filter parameter.");
        if (filterType.equals("CQL")) {
            try {
                Filter newFilter = CQL.toFilter(filterText);
                repositoryFilters.put(featureType, newFilter);
                readableRepositoryFilters.put(featureType, new Pair<String, String>(filterType,
                        filterText));
            } catch (CQLException e) {
                Throwables.propagate(e);
            }
        }
    }

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
