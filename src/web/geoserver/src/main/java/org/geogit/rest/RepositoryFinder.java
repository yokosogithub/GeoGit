package org.geogit.rest;

import java.io.IOException;
import java.util.List;

import org.geogit.api.CommandLocator;
import org.geogit.geotools.data.GeoGitDataStore;
import org.geogit.geotools.data.GeoGitDataStoreFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import com.google.common.collect.ImmutableList;

public class RepositoryFinder extends org.restlet.Finder {
    private Catalog catalog;

    public RepositoryFinder(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        final String repo = RESTUtils.getAttribute(request, "repository");
        Resource result;
        if (repo == null) {

            List<DataStoreInfo> geogitStores;
            {
                Filter filter = Predicates.equal("type", GeoGitDataStoreFactory.DISPLAY_NAME);
                CloseableIterator<DataStoreInfo> stores = catalog.list(DataStoreInfo.class, filter);
                try {
                    geogitStores = ImmutableList.copyOf(stores);
                } finally {
                    stores.close();
                }
            }
            request.getAttributes().put("stores", geogitStores);
            result = new RepositoryListResource();
            result.init(getContext(), request, response);
        } else {
            String[] wsds = repo.split(":");
            String workspace = wsds[0];
            String datastore = wsds[1];
            DataStoreInfo geogitStoreInfo = catalog.getDataStoreByName(workspace, datastore);
            DataAccess<? extends FeatureType, ? extends Feature> dataStore;
            try {
                dataStore = geogitStoreInfo.getDataStore(null);
            } catch (IOException e) {
                throw new RestletException("Error accessing datastore " + repo,
                        Status.SERVER_ERROR_INTERNAL, e);
            }
            if (!(dataStore instanceof GeoGitDataStore)) {
                throw new RestletException(repo + " is not a Geogit DataStore: "
                        + geogitStoreInfo.getType(), Status.CLIENT_ERROR_BAD_REQUEST);
            }
            GeoGitDataStore geogitDataStore = (GeoGitDataStore) dataStore;
            CommandLocator commandLocator = geogitDataStore.getCommandLocator(null);

            result = new RepositoryResource(commandLocator);
        }
        return result;
    }

}
