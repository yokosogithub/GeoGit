/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.di;

import java.util.Map;

import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.GraphDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.RefDatabase;
import org.geogit.storage.StagingDatabase;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;

public class PluginsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ObjectDatabase.class).toProvider(PluginObjectDatabaseProvider.class).in(Scopes.SINGLETON);
        bind(StagingDatabase.class).toProvider(PluginStagingDatabaseProvider.class).in(Scopes.SINGLETON);
        bind(RefDatabase.class).toProvider(PluginRefDatabaseProvider.class).in(Scopes.SINGLETON);
        bind(GraphDatabase.class).toProvider(PluginGraphDatabaseProvider.class).in(Scopes.SINGLETON);
    }

    private static class PluginObjectDatabaseProvider extends FormatSelector<ObjectDatabase> {
        private final PluginDefaults defaults;

        @Override
        protected final VersionedFormat readConfig(ConfigDatabase config) {
            String format = null, version = null;
            try {
                format = config.get("storage.objects").orNull();
                version = config.get(format + ".version").orNull();
            } catch (RuntimeException e) {
                // ignore, the config may not be available when we need this.
            }
            if (format == null || version == null) {
                // .get, not .orNull. we should only be using the plugin providers when there are
                // plugins set up
                return defaults.getObjects().get();
            } else {
                return new VersionedFormat(format, version);
            }
        }

        @Inject
        public PluginObjectDatabaseProvider(PluginDefaults defaults, ConfigDatabase config,
                Map<VersionedFormat, Provider<ObjectDatabase>> plugins) {
            super(config, plugins);
            this.defaults = defaults;
        }
    }

    private static class PluginStagingDatabaseProvider extends FormatSelector<StagingDatabase> {
        private final PluginDefaults defaults;

        @Override
        protected final VersionedFormat readConfig(ConfigDatabase config) {
            String format = null, version = null;
            try {
                format = config.get("storage.staging").orNull();
                version = config.get(format + ".version").orNull();
            } catch (RuntimeException e) {
                // ignore, the config may not be available when we need this
            }

            if (format == null || version == null) {
                // .get, not .orNull. we should only be using the plugin providers when there are
                // plugins set up
                return defaults.getStaging().get();
            } else {
                return new VersionedFormat(format, version);
            }
        }

        @Inject
        public PluginStagingDatabaseProvider(PluginDefaults defaults, ConfigDatabase config,
                Map<VersionedFormat, Provider<StagingDatabase>> plugins) {
            super(config, plugins);
            this.defaults = defaults;
        }
    }

    private static class PluginRefDatabaseProvider extends FormatSelector<RefDatabase> {
        private final PluginDefaults defaults;

        @Override
        protected final VersionedFormat readConfig(ConfigDatabase config) {
            String format = null, version = null;
            try {
                format = config.get("storage.refs").orNull();
                version = config.get(format + ".version").orNull();
            } catch (RuntimeException e) {
                // ignore, the config may not be available when we need this.
            }

            if (format == null || version == null) {
                // .get, not .orNull. we should only be using the plugin providers when there are
                // plugins set up
                return defaults.getRefs().get();
            } else {
                return new VersionedFormat(format, version);
            }
        }

        @Inject
        public PluginRefDatabaseProvider(PluginDefaults defaults, ConfigDatabase config,
                Map<VersionedFormat, Provider<RefDatabase>> plugins) {
            super(config, plugins);
            this.defaults = defaults;
        }
    }

    private static class PluginGraphDatabaseProvider extends FormatSelector<GraphDatabase> {
        private final PluginDefaults defaults;

        @Override
        protected final VersionedFormat readConfig(ConfigDatabase config) {
            String format = null, version = null;
            try {
                format = config.get("storage.graph").orNull();
                version = config.get(format + ".version").orNull();
            } catch (RuntimeException e) {
                // ignore, the config may not be available when we need this
            }

            if (format == null || version == null) {
                // .get, not .orNull. we should only be using the plugin providers when there are
                // plugins set up
                return defaults.getGraph().get();
            } else {
                return new VersionedFormat(format, version);
            }
        }

        @Inject
        public PluginGraphDatabaseProvider(PluginDefaults defaults, ConfigDatabase config,
                Map<VersionedFormat, Provider<GraphDatabase>> plugins) {
            super(config, plugins);
            this.defaults = defaults;
        }
    }
}
