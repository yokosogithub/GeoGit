/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import org.geogit.api.DefaultPlatform;
import org.geogit.api.GeoGIT;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.InjectorBuilder;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.di.GeogitModule;
import org.geogit.repository.Hints;
import org.geogit.rest.repository.CommandResource;
import org.geogit.rest.repository.RepositoryProvider;
import org.geogit.rest.repository.RepositoryRouter;
import org.geogit.storage.bdbje.JEStorageModule;
import org.geogit.storage.blueprints.BlueprintsGraphModule;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Router;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;

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

    private RepositoryProvider repoProvider;

    public Main() {
        super();
    }

    public Main(GeoGIT geogit) {
        super();
        this.repoProvider = new SingleRepositoryProvider(geogit);
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);
        assert context != null;

        Map<String, Object> attributes = context.getAttributes();

        GeoGIT geogit;
        if (attributes.containsKey("geogit")) {
            geogit = (GeoGIT) attributes.get("geogit");
        } else {
            // revisit, not used at all
            // ServletContext sc = (ServletContext) dispatcher.getContext()
            // .getAttributes().get("org.restlet.ext.servlet.ServletContext");
            // String repo = sc.getInitParameter("repository");
            String repo = null;
            if (repo == null) {
                repo = System.getProperty("org.geogit.web.repository");
            }
            if (repo == null) {
                return;
                // throw new IllegalStateException(
                // "Cannot launch geogit servlet without `repository` parameter");
            }
            geogit = loadGeoGIT(repo);
        }
        repoProvider = new SingleRepositoryProvider(geogit);
    }

    @Override
    public Router createRoot() {

        Router router = new Router() {

            @Override
            protected synchronized void init(Request request, Response response) {
                super.init(request, response);
                if (!isStarted()) {
                    return;
                }
                request.getAttributes().put(RepositoryProvider.KEY, repoProvider);
            }
        };
        router.attach("/repo", new RepositoryRouter());
        router.attach("/{command}.{extension}", CommandResource.class);
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
        GeoGIT geogit = loadGeoGIT(repo);
        Context context = new Context();
        Application application = new Main(geogit);
        application.setContext(context);
        Component comp = new Component();
        comp.getDefaultHost().attach(application);
        comp.getServers().add(Protocol.HTTP, 8182);
        comp.start();
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
