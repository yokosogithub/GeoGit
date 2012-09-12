/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.command.porcelain;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.geogit.api.Platform;
import org.geogit.api.porcelain.InitOp;
import org.geogit.repository.Repository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Injector;

/**
 *
 */
public class InitOpTest {

    private Platform platform;

    private Injector injector;

    private InitOp init;

    private File workingDir;

    private Repository mockRepo;

    @Before
    public void setUp() throws IOException {
        platform = Mockito.mock(Platform.class);
        injector = Mockito.mock(Injector.class);
        init = new InitOp(platform, injector);

        mockRepo = Mockito.mock(Repository.class);

        workingDir = new File("target", "inittest");
        FileUtils.deleteDirectory(workingDir);
        assertTrue(workingDir.mkdirs());

        when(platform.pwd()).thenReturn(workingDir);
    }

    @Test
    public void testCreateNewRepo() throws Exception {
        when(injector.getInstance(eq(Repository.class))).thenReturn(mockRepo);
        Repository created = init.call();
        assertSame(mockRepo, created);
        assertTrue(new File(workingDir, ".geogit").exists());
        assertTrue(new File(workingDir, ".geogit").isDirectory());
        verify(platform, atLeastOnce()).pwd();
    }

    @Test
    public void testReinitializeExistingRepo() throws Exception {
        when(injector.getInstance(eq(Repository.class))).thenReturn(mockRepo);
        Repository created = init.call();
        assertSame(mockRepo, created);
        assertTrue(new File(workingDir, ".geogit").exists());
        assertTrue(new File(workingDir, ".geogit").isDirectory());

        assertNull(init.call());// repo existed, returns null
        verify(platform, atLeastOnce()).pwd();
    }

    @Test
    public void testReinitializeExistingRepoFromInsideASubdirectory() throws Exception {
        testCreateNewRepo();

        File subDir = new File(new File(workingDir, "subdir1"), "subdir2");
        assertTrue(subDir.mkdirs());

        when(platform.pwd()).thenReturn(subDir);

        assertNull(init.call());// repo existed, returns null
        verify(platform, atLeastOnce()).pwd();
    }
}
