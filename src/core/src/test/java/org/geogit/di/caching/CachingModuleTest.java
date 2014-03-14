/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.di.caching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Iterator;

import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.TestPlatform;
import org.geogit.storage.ConfigDatabase;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.datastream.DataStreamSerializationFactory;
import org.geogit.storage.fs.IniFileConfigDatabase;
import org.geogit.storage.memory.HeapObjectDatabse;
import org.geogit.storage.memory.HeapStagingDatabase;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;

public class CachingModuleTest {

    private StagingDatabase index;

    private ObjectDatabase odb;

    private Cache<ObjectId, RevObject> odbCache;

    private Cache<ObjectId, RevObject> indexCache;

    private static final RevObject o1 = obj("o1"), o2 = obj("o2"), o3 = ft("o3");

    private static final RevObject s1 = obj("s1"), s2 = obj("s2"), s3 = ft("s3");

    @Before
    public void setUp() throws Exception {
        odbCache = mock(Cache.class);
        indexCache = mock(Cache.class);

        final ObjectDatabaseCacheFactory odbCacheFac = mock(ObjectDatabaseCacheFactory.class);
        when(odbCacheFac.get()).thenReturn(odbCache);

        final StagingDatabaseCacheFactory indexCacheFac = mock(StagingDatabaseCacheFactory.class);
        when(indexCacheFac.get()).thenReturn(indexCache);

        final Platform platform = new TestPlatform(new File("target"));

        Module module = new AbstractModule() {

            @Override
            protected void configure() {

                DataStreamSerializationFactory sfac = new DataStreamSerializationFactory();
                bind(ObjectSerializingFactory.class).toInstance(sfac);

                bind(ObjectDatabase.class).to(HeapObjectDatabse.class).in(Scopes.SINGLETON);
                bind(StagingDatabase.class).to(HeapStagingDatabase.class).in(Scopes.SINGLETON);

                ConfigDatabase config = new IniFileConfigDatabase(platform);
                bind(ConfigDatabase.class).toInstance(config);

                bind(ObjectDatabaseCacheFactory.class).toInstance(odbCacheFac);
                bind(StagingDatabaseCacheFactory.class).toInstance(indexCacheFac);
            }
        };

        Injector injector = Guice
                .createInjector(Modules.override(new CachingModule()).with(module));

        odb = injector.getInstance(ObjectDatabase.class);
        index = injector.getInstance(StagingDatabase.class);
        odb.open();
        index.open();

        odb.put(o1);
        odb.put(o2);
        odb.put(o3);
        index.put(s1);
        index.put(s2);
        index.put(s3);
    }

    private static RevObject obj(String name) {
        RevCommit c = new CommitBuilder().setTreeId(ObjectId.NULL).setMessage(name).build();
        return c;
    }

    private static RevFeatureType ft(String name) {
        SimpleFeatureType type;
        try {
            type = DataUtilities.createType("", name, RepositoryTestCase.pointsTypeSpec);
        } catch (SchemaException e) {
            throw Throwables.propagate(e);
        }
        RevFeatureType rft = RevFeatureType.build(type);
        return rft;
    }

    @Test
    public void delete() {
        odb.delete(o1.getId());
        verify(odbCache).invalidate(eq(o1.getId()));

        index.delete(s1.getId());
        verify(indexCache).invalidate(eq(s1.getId()));
    }

    @Test
    public void deleteAll() {
        Iterator<ObjectId> ids;
        ids = ImmutableList.of(o1.getId(), o2.getId()).iterator();
        odb.deleteAll(ids);
        verify(odbCache, times(1)).invalidate(eq(o1.getId()));
        verify(odbCache, times(1)).invalidate(eq(o2.getId()));

        ids = ImmutableList.of(s1.getId(), s2.getId()).iterator();
        index.deleteAll(ids);
        verify(indexCache, times(1)).invalidate(eq(s1.getId()));
        verify(indexCache, times(1)).invalidate(eq(s2.getId()));
    }

    @Test
    public void testGetCacheHit() {
        when(odbCache.getIfPresent(eq(o1.getId()))).thenReturn(o1);
        assertSame(o1, odb.get(o1.getId()));
    }

    @Test
    public void testGetCacheMiss() {
        when(odbCache.getIfPresent(eq(o3.getId()))).thenReturn(null);
        RevObject actual = odb.get(o3.getId());
        assertNotSame(o3, actual);
        assertEquals(o3, actual);

        when(indexCache.getIfPresent(eq(s3.getId()))).thenReturn(null);
        actual = index.get(s3.getId());
        assertNotSame(s3, actual);
        assertEquals(s3, actual);
}
}
