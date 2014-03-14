/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletContext;

import org.geogit.api.DefaultPlatform;
import org.geogit.api.GeoGIT;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.InjectorBuilder;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.di.GeogitModule;
import org.geogit.repository.Hints;
import org.geogit.storage.bdbje.JEStorageModule;
import org.geogit.storage.blueprints.BlueprintsGraphModule;
import org.geogit.web.api.repo.AffectedFeaturesResource;
import org.geogit.web.api.repo.ApplyChangesResource;
import org.geogit.web.api.repo.BatchedObjectResource;
import org.geogit.web.api.repo.BeginPush;
import org.geogit.web.api.repo.DepthResource;
import org.geogit.web.api.repo.EndPush;
import org.geogit.web.api.repo.FilteredChangesResource;
import org.geogit.web.api.repo.ManifestResource;
import org.geogit.web.api.repo.MergeFeatureResource;
import org.geogit.web.api.repo.ObjectExistsResource;
import org.geogit.web.api.repo.ObjectResource;
import org.geogit.web.api.repo.ParentResource;
import org.geogit.web.api.repo.SendObjectResource;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

/**
 * Both an embedded jetty launcher
 */
public class Main extends Application {

    static {
        setup();
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        assert context != null;

        ConcurrentMap<String, Object> attributes = context.getAttributes();
        if (!attributes.containsKey("geogit")) {
            ServletContext sc = (ServletContext) context.getServerDispatcher().getContext()
                    .getAttributes().get("org.restlet.ext.servlet.ServletContext");
            String repo = sc.getInitParameter("repository");
            if (repo == null) {
                repo = System.getProperty("org.geogit.web.repository");
            }
            if (repo == null) {
                throw new IllegalStateException(
                        "Cannot launch geogit servlet without `repository` parameter");
            }
            context.getAttributes().put("geogit", loadGeoGIT(repo));
        }
    }

    @Override
    public Restlet createInboundRoot() {
        Router router = new Router();
        router.attach("/repo", makeRepoRouter());
        router.attach("/{command}", CommandResource.class);
        return router;
    }

    static GeoGIT loadGeoGIT(String repo) {
        Platform platform = new DefaultPlatform();
        platform.setWorkingDir(new File(repo));
        Injector inj = GlobalInjectorBuilder.builder.build();
        GeoGIT geogit = new GeoGIT(inj, platform.pwd());

        if (geogit.command(ResolveGeogitDir.class).call().isPresent()) {
            geogit.getRepository();
            return geogit;
        }

        return geogit;
    }

    static void startServer(String repo) throws Exception {
        Context context = new Context();
        context.getAttributes().put("geogit", loadGeoGIT(repo));

        Application application = new Main();
        application.setContext(context);
        Component comp = new Component();
        comp.getDefaultHost().attach(application);
        comp.getServers().add(Protocol.HTTP, 8182);
        comp.start();
    }

    static Router makeRepoRouter() {
        Router router = new Router();
        router.attach("/manifest", ManifestResource.class);
        router.attach("/objects/{id}", new ObjectResource());
        router.attach("/batchobjects", BatchedObjectResource.class);
        router.attach("/sendobject", SendObjectResource.class);
        router.attach("/exists", ObjectExistsResource.class);
        router.attach("/beginpush", BeginPush.class);
        router.attach("/endpush", EndPush.class);
        router.attach("/getdepth", DepthResource.class);
        router.attach("/getparents", ParentResource.class);
        router.attach("/affectedfeatures", AffectedFeaturesResource.class);
        router.attach("/filteredchanges", FilteredChangesResource.class);
        router.attach("/applychanges", ApplyChangesResource.class);
        router.attach("/mergefeature", MergeFeatureResource.class);
        return router;
    }

    static void setup() {
        GlobalInjectorBuilder.builder = new InjectorBuilder() {
            @Override
            public Injector build(Hints hints) {
                return Guice
                        .createInjector(Modules.override(new GeogitModule()).with(
                                new JEStorageModule(), new BlueprintsGraphModule(),
                                new HintsModule(hints)));
            }
        };
    }

    public static void main(String[] args) throws Exception {
        LinkedList<String> argList = new LinkedList<String>(Arrays.asList(args));
        if (argList.size() == 0) {
            System.out.println("provide geogit repo path");
            System.exit(1);
        }
        String repo = argList.pop();
        startServer(repo);
    }
}
