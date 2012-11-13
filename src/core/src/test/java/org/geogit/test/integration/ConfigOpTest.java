package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
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

    TestPlatform testPlatform;

    @Before
    public final void setUp() {
        final File userhome = tempFolder.newFolder("testUserHomeDir");
        final File workingDir = tempFolder.newFolder("testWorkingDir");
        tempFolder.newFolder("testWorkingDir/.geogit");
        testPlatform = new TestPlatform(workingDir);
        testPlatform.setUserHome(userhome);
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
        assertNull(result.get("section.string"));

        // Test unsetting a value that doesn't exist
        config.setAction(ConfigAction.CONFIG_UNSET).setName("section.string").setValue(null).call();
        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string").setValue(null)
                .call().or(new HashMap<String, String>());
        assertNull(result.get("section.string"));

        // Test removing a section that exists
        config.setAction(ConfigAction.CONFIG_SET).setName("section.string").setValue("1").call();
        config.setAction(ConfigAction.CONFIG_SET).setName("section.string2").setValue("2").call();
        config.setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName("section").setValue(null)
                .call();

        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string").setValue(null)
                .call().or(new HashMap<String, String>());
        assertNull(result.get("section.string"));
        result = config.setAction(ConfigAction.CONFIG_GET).setName("section.string2")
                .setValue(null).call().or(new HashMap<String, String>());
        assertNull(result.get("section.string2"));

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
        test(testPlatform, false);
    }

    @Test
    public void testGlobal() {
        test(testPlatform, true);
    }

    @Test
    public void testNullNameValuePairForGet() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testEmptyNameAndValueForGet() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName("").setValue("").call();
    }

    @Test
    public void testEmptyNameAndValueForSet() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_SET).setName("").setValue("").call();
    }

    @Test
    public void testEmptyNameForUnset() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_UNSET).setName("").setValue(null)
                .call();
    }

    @Test
    public void testEmptyNameForRemoveSection() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName("").call();
    }

    @Test
    public void testNoNameForSet() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_SET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testNoNameForUnset() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_UNSET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testNoNameForRemoveSection() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName(null)
                .setValue(null).call();
    }

    @Test
    public void testRemovingMissingSection() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION)
                .setName("unusedsectionname").setValue(null).call();
    }

    @Test
    public void testInvalidSectionKey() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));
        Optional<Map<String, String>> result = config.setGlobal(true)
                .setAction(ConfigAction.CONFIG_GET).setName("doesnt.exist").setValue(null).call();
        assertFalse(result.isPresent());
    }

    @Test
    public void testTooManyArguments() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName("too.many")
                .setValue("arguments").call();
    }

    @Test
    public void testAccessors() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));
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
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

        exception.expect(ConfigException.class);
        config.setAction(ConfigAction.CONFIG_NO_ACTION).setName("section.key").setValue(null)
                .call();
    }

    @Test
    public void testFallback() {
        final ConfigOp config = new ConfigOp(new IniConfigDatabase(testPlatform));

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
