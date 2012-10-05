package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.geogit.api.Platform;
import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.storage.fs.IniConfigDatabase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

// TODO: Not sure if this belongs in porcelain or integration

public class ConfigOpTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public final void setUp() {
    }

    @After
    public final void tearDown() {
    }

    private void test(Platform platform, boolean global) {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));
        config.setGlobal(global);

        config.setAction(ConfigAction.CONFIG_SET).setName("section.string").setValue("1").call();

        Map<String, String> result = config.setAction(ConfigAction.CONFIG_GET)
                .setName("section.string").setValue(null).call().or(new HashMap<String, String>());
        assertEquals(result.get("section.string"), "1");

        // Test overwriting a value that already exists
        config.setAction(ConfigAction.CONFIG_SET).setName("section.string").setValue("2").call();

        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string").setValue(null)
                .call().or(new HashMap<String, String>());
        assertEquals(result.get("section.string"), "2");

        // Test unsetting a value that exists
        config.setAction(ConfigAction.CONFIG_UNSET).setName("section.string").setValue(null).call();
        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string").setValue(null)
                .call().or(new HashMap<String, String>());
        assertEquals(result.get("section.string"), null);

        // Test unsetting a value that doesn't exist
        config.setAction(ConfigAction.CONFIG_UNSET).setName("section.string").setValue(null).call();
        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string").setValue(null)
                .call().or(new HashMap<String, String>());
        assertEquals(result.get("section.string"), null);

        // Test removing a section that exists
        config.setAction(ConfigAction.CONFIG_SET).setName("section.string").setValue("1").call();
        config.setAction(ConfigAction.CONFIG_SET).setName("section.string2").setValue("2").call();
        config.setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName("section").setValue(null)
                .call();

        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string").setValue(null)
                .call().or(new HashMap<String, String>());
        assertEquals(result.get("section.string"), null);
        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string2")
                .setValue(null).call().or(new HashMap<String, String>());
        assertEquals(result.get("section.string2"), null);

        // Try listing the config file
        config.setAction(ConfigAction.CONFIG_SET).setName("section.string").setValue("1").call();
        config.setAction(ConfigAction.CONFIG_SET).setName("section.string2").setValue("2").call();

        result = config.setAction(ConfigAction.CONFIG_LIST).call()
                .or(new HashMap<String, String>());
        assertEquals(result.get("section.string"), "1");
        assertEquals(result.get("section.string2"), "2");
    }

    @Test
    public void testLocal() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        when(platform.getUserHome()).thenReturn(userhome);

        test(platform, false);
    }

    @Test
    public void testGlobal() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        test(platform, true);
    }

    @Test
    public void testNullNameValuePairForGet() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testEmptyNameAndValueForGet() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName("").setValue("").call();
    }

    @Test
    public void testEmptyNameAndValueForSet() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_SET).setName("").setValue("").call();
    }

    @Test
    public void testEmptyNameForUnset() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_UNSET).setName("").setValue(null)
                .call();
    }

    @Test
    public void testEmptyNameForRemoveSection() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName("").call();
    }

    @Test
    public void testNoNameForSet() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_SET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testNoNameForUnset() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_UNSET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testNoNameForRemoveSection() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName(null)
                .setValue(null).call();
    }

    @Test
    public void testRemovingMissingSection() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION)
                .setName("unusedsectionname").setValue(null).call();
    }

    @Test
    public void testInvalidSectionKey() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));
        Optional<Map<String, String>> result = config.setGlobal(true)
                .setAction(ConfigAction.CONFIG_GET).setName("doesnt.exist").setValue(null).call();
        assertFalse(result.isPresent());
    }

    @Test
    public void testTooManyArguments() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName("too.many")
                .setValue("arguments").call();
    }

    @Test
    public void testAccessors() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));
        config.setGlobal(true);
        assertTrue(config.getGlobal());

        config.setGlobal(false);
        assertFalse(config.getGlobal());

        config.setAction(ConfigAction.CONFIG_UNSET);
        assertEquals(config.getAction(), ConfigAction.CONFIG_UNSET);

        config.setName("section.string");
        assertEquals(config.getName(), "section.string");

        config.setValue("value");
        assertEquals(config.getValue(), "value");
    }

    @Test
    public void testNoAction() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        exception.expect(ConfigException.class);
        config.setAction(ConfigAction.CONFIG_NO_ACTION).setName("section.key").setValue(null)
                .call();
    }

    @Test
    public void testFallback() {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");

        final Platform platform = mock(Platform.class);
        when(platform.getUserHome()).thenReturn(userhome);

        final ConfigOp config = new ConfigOp(new IniConfigDatabase(platform));

        // Set a value in global config, then try to get value from local even though
        // we're not in a valid repository
        config.setAction(ConfigAction.CONFIG_SET).setGlobal(true).setName("section.key")
                .setValue("1").call();
        Optional<Map<String, String>> value = config.setAction(ConfigAction.CONFIG_GET)
                .setGlobal(false).setName("section.key").setValue(null).call();
        assertTrue(value.isPresent());
        assertEquals(value.get().get("section.key"), "1");

        value = Optional.absent();
        value = config.setAction(ConfigAction.CONFIG_GET).setGlobal(false).setName("section.key")
                .setValue("").call();
        assertTrue(value.isPresent());
        assertEquals(value.get().get("section.key"), "1");
    }
}
