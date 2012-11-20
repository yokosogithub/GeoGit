package org.geogit.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.geogit.api.porcelain.ConfigException;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;

// TODO: Not sure if this belongs in porcelain or integration

public class ConfigOpTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public final void setUpInternal() {
    }

    @After
    public final void tearDownInternal() {
    }

    private void test(boolean global) {
        final ConfigOp config = geogit.command(ConfigOp.class);
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
        test(false);
    }

    @Test
    public void testGlobal() {
        test(true);
    }

    @Test
    public void testNullNameValuePairForGet() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testEmptyNameAndValueForGet() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName("").setValue("").call();
    }

    @Test
    public void testEmptyNameAndValueForSet() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_SET).setName("").setValue("").call();
    }

    @Test
    public void testEmptyNameForUnset() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_UNSET).setName("").setValue(null)
                .call();
    }

    @Test
    public void testEmptyNameForRemoveSection() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName("").call();
    }

    @Test
    public void testNoNameForSet() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_SET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testNoNameForUnset() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_UNSET).setName(null).setValue(null)
                .call();
    }

    @Test
    public void testNoNameForRemoveSection() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION).setName(null)
                .setValue(null).call();
    }

    @Test
    public void testRemovingMissingSection() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_REMOVE_SECTION)
                .setName("unusedsectionname").setValue(null).call();
    }

    @Test
    public void testInvalidSectionKey() {
        final ConfigOp config = geogit.command(ConfigOp.class);
        Optional<Map<String, String>> result = config.setGlobal(true)
                .setAction(ConfigAction.CONFIG_GET).setName("doesnt.exist").setValue(null).call();
        assertFalse(result.isPresent());
    }

    @Test
    public void testTooManyArguments() {
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setGlobal(true).setAction(ConfigAction.CONFIG_GET).setName("too.many")
                .setValue("arguments").call();
    }

    @Test
    public void testAccessors() {
        final ConfigOp config = geogit.command(ConfigOp.class);
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
        final ConfigOp config = geogit.command(ConfigOp.class);

        exception.expect(ConfigException.class);
        config.setAction(ConfigAction.CONFIG_NO_ACTION).setName("section.key").setValue(null)
                .call();
    }

    @Test
    public void testFallback() {
        final ConfigOp config = geogit.command(ConfigOp.class);

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
