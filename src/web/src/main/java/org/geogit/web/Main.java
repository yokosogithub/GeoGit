package org.geogit.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import org.geogit.api.DefaultPlatform;
import org.geogit.api.GeoGIT;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.InjectorBuilder;
import org.geogit.api.Platform;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.di.GeogitModule;
import org.geogit.storage.bdbje.JEStorageModule;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

/**
 *
 */
public class Main {

    static GeoGIT loadGeoGIT(String repo) {
        Platform platform = new DefaultPlatform();
        platform.setWorkingDir(new File(repo));
        Injector inj = GlobalInjectorBuilder.builder.get();
        GeoGIT geogit = new GeoGIT(inj, platform.pwd());

        if (null != geogit.command(ResolveGeogitDir.class).call()) {
            geogit.getRepository();
            return geogit;
        }

        return geogit;
    }

    static void startServer(String repo) throws Exception {
        Context context = new Context();
        context.getAttributes().put("geogit", loadGeoGIT(repo));

        Application application = new Application(context);

        Router router = new Router();
        router.attach("/{command}", CommandResource.class);

        application.setInboundRoot(router);

        Component comp = new Component();
        comp.getDefaultHost().attach(application);
        comp.getServers().add(Protocol.HTTP, 8182);
        comp.start();
    }

    static void setup() {
        GlobalInjectorBuilder.builder = new InjectorBuilder() {
            @Override
            public Injector get() {
                return Guice.createInjector(Modules.override(new GeogitModule())
                        .with(new JEStorageModule()));
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
        setup();
        startServer(repo);
    }
}
