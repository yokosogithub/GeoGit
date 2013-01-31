package org.geogit.rest.repository;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.GeoGIT;
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
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

public class RepositoryFinder extends org.restlet.Finder {
    private Catalog catalog;

    public RepositoryFinder(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Resource findTarget(Request request, Response response) {

        final String repo = RESTUtils.getAttribute(request, "repository");
        final String command = RESTUtils.getAttribute(request, "command");
        Resource result;
        if (repo == null) {
            List<DataStoreInfo> geogitStores = findGeogitStores();
            request.getAttributes().put("stores", geogitStores);
            result = new RepositoryListResource();
        } else {
            GeoGitDataStore geogitDataStore = findDataStore(repo);
            GeoGIT geogit = geogitDataStore.getGeogit();

            request.getAttributes().put("store", geogitDataStore);
            request.getAttributes().put("geogit", geogit);

            if (command == null) {
                result = new RepositoryResource();
            } else {
                result = findCommandResource(geogitDataStore, command, request);
            }
        }

        result.init(getContext(), request, response);
        return result;
    }

    /**
     * @param geogitDataStore
     * @param command
     * @param request
     * @return
     */
    private Resource findCommandResource(GeoGitDataStore geogitDataStore, String command,
            Request request) {
        Form form = request.getResourceRef().getQueryAsForm();

        return null;
    }

    private List<DataStoreInfo> findGeogitStores() {
        List<DataStoreInfo> geogitStores;
        {
            Filter filter = Predicates.equal("type", GeoGitDataStoreFactory.DISPLAY_NAME);
            CloseableIterator<DataStoreInfo> stores = catalog.list(DataStoreInfo.class, filter);
            try {
                Predicate<DataStoreInfo> enabled = new Predicate<DataStoreInfo>() {
                    @Override
                    public boolean apply(@Nullable DataStoreInfo input) {
                        return input.isEnabled();
                    }
                };
                geogitStores = ImmutableList.copyOf(Iterators.filter(stores, enabled));
            } finally {
                stores.close();
            }
        }
        return geogitStores;
    }

    private GeoGitDataStore findDataStore(String repo) {
        String[] wsds = repo.split(":");
        String workspace = wsds[0];
        String datastore = wsds[1];

        DataStoreInfo geogitStoreInfo = catalog.getDataStoreByName(workspace, datastore);
        if (null == geogitStoreInfo) {
            throw new RestletException("No such repository: " + repo, Status.CLIENT_ERROR_NOT_FOUND);
        }
        if (!geogitStoreInfo.isEnabled()) {
            throw new RestletException("Repository is not enabled: " + repo,
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
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
        return geogitDataStore;
    }

}
