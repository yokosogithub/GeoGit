/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli.test.functional;

import static org.geogit.cli.test.functional.GlobalState.deleteAndReplaceFeatureType;
import static org.geogit.cli.test.functional.GlobalState.exitCode;
import static org.geogit.cli.test.functional.GlobalState.geogitCLI;
import static org.geogit.cli.test.functional.GlobalState.insert;
import static org.geogit.cli.test.functional.GlobalState.insertAndAdd;
import static org.geogit.cli.test.functional.GlobalState.insertAndAddFeatures;
import static org.geogit.cli.test.functional.GlobalState.insertFeatures;
import static org.geogit.cli.test.functional.GlobalState.platform;
import static org.geogit.cli.test.functional.GlobalState.runAndParseCommand;
import static org.geogit.cli.test.functional.GlobalState.runCommand;
import static org.geogit.cli.test.functional.GlobalState.setUpDirectories;
import static org.geogit.cli.test.functional.GlobalState.setupGeogit;
import static org.geogit.cli.test.functional.GlobalState.stdOut;
import static org.geogit.cli.test.functional.GlobalState.tempFolder;
import static org.geogit.cli.test.functional.TestFeatures.feature;
import static org.geogit.cli.test.functional.TestFeatures.idP1;
import static org.geogit.cli.test.functional.TestFeatures.lines1;
import static org.geogit.cli.test.functional.TestFeatures.lines2;
import static org.geogit.cli.test.functional.TestFeatures.lines3;
import static org.geogit.cli.test.functional.TestFeatures.points1;
import static org.geogit.cli.test.functional.TestFeatures.points1_modified;
import static org.geogit.cli.test.functional.TestFeatures.points2;
import static org.geogit.cli.test.functional.TestFeatures.points3;
import static org.geogit.cli.test.functional.TestFeatures.pointsName;
import static org.geogit.cli.test.functional.TestFeatures.pointsType;
import static org.geogit.cli.test.functional.TestFeatures.setupFeatures;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.FeatureDiff;
import org.geogit.api.plumbing.diff.GenericAttributeDiffImpl;
import org.geogit.api.plumbing.diff.Patch;
import org.geogit.api.plumbing.diff.PatchSerializer;
import org.geogit.api.porcelain.MergeConflictsException;
import org.geogit.api.porcelain.MergeOp;
import org.geogit.repository.Hints;
import org.geogit.repository.WorkingTree;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.Feature;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import cucumber.annotation.en.Given;
import cucumber.annotation.en.Then;
import cucumber.annotation.en.When;
import cucumber.runtime.java.StepDefAnnotation;

/**
 *
 */
@StepDefAnnotation
public class DefaultStepDefinitions {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    @cucumber.annotation.Before
    public void before() throws Exception {
        tempFolder = new TemporaryFolder();
        tempFolder.create();
        setupFeatures();
    }

    @cucumber.annotation.After
    public void after() {
        if (GlobalState.geogitCLI != null) {
            GlobalState.geogitCLI.close();
            GlobalState.geogitCLI = null;
        }
        if (GlobalState.consoleReader != null) {
            GlobalState.consoleReader.shutdown();
            GlobalState.consoleReader = null;
        }
        System.gc();
        System.runFinalization();
        tempFolder.delete();
        // assertFalse(
        // "this test is messing up with the config, make sure it uses CLITestInjectorBuilder",
        // new File("/home/groldan/.geogitconfig").exists());
    }

    @Given("^I am in an empty directory$")
    public void I_am_in_an_empty_directory() throws Throwable {
        setUpDirectories();
        assertEquals(0, platform.pwd().list().length);
        setupGeogit();
    }

    @When("^I run the command \"([^\"]*)\"$")
    public void I_run_the_command_X(String commandSpec) throws Throwable {
        String[] args = commandSpec.split(" ");
        File pwd = platform.pwd();
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].replace("${currentdir}", pwd.getAbsolutePath());
        }
        runCommand(args);
    }

    @Then("^it should exit with non-zero exit code$")
    public void it_should_exit_with_non_zero_exit_code() throws Throwable {
        assertFalse("exited with exit code " + exitCode, exitCode == 0);
    }

    @Then("^it should exit with zero exit code$")
    public void it_should_exit_with_zero_exit_code() throws Throwable {
        assertEquals(0, exitCode);
    }

    @Then("^it should answer \"([^\"]*)\"$")
    public void it_should_answer_exactly(String expected) throws Throwable {
        File pwd = platform.pwd();
        expected = expected.replace("${currentdir}", pwd.getAbsolutePath()).toLowerCase()
                .replaceAll("\\\\", "/");
        String actual = stdOut.toString().replaceAll(LINE_SEPARATOR, "").replaceAll("\\\\", "/")
                .trim().toLowerCase();
        assertEquals(expected, actual);
    }

    @Then("^the response should contain \"([^\"]*)\"$")
    public void the_response_should_contain(String expected) throws Throwable {
        String actual = stdOut.toString().replaceAll(LINE_SEPARATOR, "").replaceAll("\\\\", "/");
        if (!actual.contains(expected))
            System.err.println(actual);
        assertTrue("'" + actual + "' does not contain '" + expected + "'",
                actual.contains(expected));
    }

    @Then("^the response should not contain \"([^\"]*)\"$")
    public void the_response_should_not_contain(String expected) throws Throwable {
        String actual = stdOut.toString().replaceAll(LINE_SEPARATOR, "").replaceAll("\\\\", "/");
        assertFalse(actual, actual.contains(expected));
    }

    @Then("^the response should contain ([^\"]*) lines$")
    public void the_response_should_contain_x_lines(int lines) throws Throwable {
        String output = stdOut.toString();
        String[] lineStrings = output.split(LINE_SEPARATOR);
        assertEquals(output, lines, lineStrings.length);
    }

    @Then("^the response should start with \"([^\"]*)\"$")
    public void the_response_should_start_with(String expected) throws Throwable {
        String actual = stdOut.toString().replaceAll(LINE_SEPARATOR, "");
        assertTrue(actual, actual.startsWith(expected));
    }

    @Then("^the repository directory shall exist$")
    public void the_repository_directory_shall_exist() throws Throwable {
        List<String> output = runAndParseCommand(true, "rev-parse", "--resolve-geogit-dir");
        assertEquals(output.toString(), 1, output.size());
        String location = output.get(0);
        assertNotNull(location);
        File repoDir = new File(location);
        assertTrue("Repository directory not found: " + repoDir.getAbsolutePath(), repoDir.exists());
    }

    @Given("^I have a remote ref called \"([^\"]*)\"$")
    public void i_have_a_remote_ref_called(String expected) throws Throwable {
        String ref = "refs/remotes/origin/" + expected;
        geogitCLI.getGeogit(Hints.readWrite()).command(UpdateRef.class).setName(ref)
                .setNewValue(ObjectId.NULL).call();
        Optional<Ref> refValue = geogitCLI.getGeogit(Hints.readWrite()).command(RefParse.class)
                .setName(ref).call();
        assertTrue(refValue.isPresent());
        assertEquals(refValue.get().getObjectId(), ObjectId.NULL);
    }

    @Given("^I have an unconfigured repository$")
    public void I_have_an_unconfigured_repository() throws Throwable {
        setUpDirectories();
        setupGeogit();

        List<String> output = runAndParseCommand(true, "init");
        assertEquals(output.toString(), 1, output.size());
        assertNotNull(output.get(0));
        assertTrue(output.get(0), output.get(0).startsWith("Initialized"));
    }

    @Given("^I have a merge conflict state$")
    public void I_have_a_merge_conflict_state() throws Throwable {
        I_have_conflicting_branches();
        Ref branch = geogitCLI.getGeogit(Hints.readOnly()).command(RefParse.class)
                .setName("branch1").call().get();
        try {
            geogitCLI.getGeogit(Hints.readWrite()).command(MergeOp.class)
                    .addCommit(Suppliers.ofInstance(branch.getObjectId())).call();
            fail();
        } catch (MergeConflictsException e) {
        }
    }

    @Given("^I have conflicting branches$")
    public void I_have_conflicting_branches() throws Throwable {
        // Create the following revision graph
        // ............o
        // ............|
        // ............o - Points 1 added
        // .........../|\
        // branch2 - o | o - branch1 - Points 1 modifiedB and points 2 added
        // ............|
        // ............o - points 1 modified
        // ............|
        // ............o - master - HEAD - Lines 1 modified
        // branch1 and master are conflicting
        Feature points1ModifiedB = feature(pointsType, idP1, "StringProp1_3", new Integer(2000),
                "POINT(1 1)");
        Feature points1Modified = feature(pointsType, idP1, "StringProp1_2", new Integer(1000),
                "POINT(1 1)");
        insertAndAdd(points1);
        runCommand(true, "commit -m Commit1");
        runCommand(true, "branch branch1");
        runCommand(true, "branch branch2");
        insertAndAdd(points1Modified);
        runCommand(true, "commit -m Commit2");
        insertAndAdd(lines1);
        runCommand(true, "commit -m Commit3");
        runCommand(true, "checkout branch1");
        insertAndAdd(points1ModifiedB);
        insertAndAdd(points2);
        runCommand(true, "commit -m Commit4");
        runCommand(true, "checkout branch2");
        insertAndAdd(points3);
        runCommand(true, "commit -m Commit5");
        runCommand(true, "checkout master");
    }

    @Given("^I set up a hook$")
    public void I_set_up_a_hook() throws Throwable {
        File hooksDir = new File(platform.pwd(), ".geogit/hooks");
        File hook = new File(hooksDir, "pre_commit.js");
        String script = "exception = Packages.org.geogit.api.hooks.CannotRunGeogitOperationException;\n"
                + "msg = params.get(\"message\");\n"
                + "if (msg.length() < 5){\n"
                + "\tthrow new exception(\"Commit messages must have at least 5 letters\");\n"
                + "}\n" + "params.put(\"message\", msg.toLowerCase());";
        Files.write(script, hook, Charset.forName("UTF-8"));
    }

    @Given("^there is a remote repository$")
    public void there_is_a_remote_repository() throws Throwable {
        I_am_in_an_empty_directory();

        final File currDir = platform.pwd();

        List<String> output = runAndParseCommand(true, "init", "remoterepo");

        assertEquals(output.toString(), 1, output.size());
        assertNotNull(output.get(0));
        assertTrue(output.get(0), output.get(0).startsWith("Initialized"));

        final File remoteRepo = new File(currDir, "remoterepo");
        GlobalState.remoteRepo = remoteRepo;
        platform.setWorkingDir(remoteRepo);
        setupGeogit();
        runCommand(true, "config", "--global", "user.name", "John Doe");
        runCommand(true, "config", "--global", "user.email", "JohnDoe@example.com");
        insertAndAdd(points1);
        runCommand(true, "commit -m Commit1");
        runCommand(true, "branch -c branch1");
        insertAndAdd(points2);
        runCommand(true, "commit -m Commit2");
        insertAndAdd(points3);
        runCommand(true, "commit -m Commit3");
        runCommand(true, "checkout master");
        insertAndAdd(lines1);
        runCommand(true, "commit -m Commit4");
        insertAndAdd(lines2);
        runCommand(true, "commit -m Commit5");

        platform.setWorkingDir(currDir);
        setupGeogit();
    }

    @Given("^I have a repository$")
    public void I_have_a_repository() throws Throwable {
        I_have_an_unconfigured_repository();
        runCommand(true, "config", "--global", "user.name", "John Doe");
        runCommand(true, "config", "--global", "user.email", "JohnDoe@example.com");
    }

    @Given("^I have a repository with a remote$")
    public void I_have_a_repository_with_a_remote() throws Throwable {
        there_is_a_remote_repository();

        List<String> output = runAndParseCommand("init", "localrepo");
        assertEquals(output.toString(), 1, output.size());
        assertNotNull(output.get(0));
        assertTrue(output.get(0), output.get(0).startsWith("Initialized"));

        platform.setWorkingDir(new File(platform.pwd(), "localrepo"));
        setupGeogit();

        runCommand(true, "config", "--global", "user.name", "John Doe");
        runCommand(true, "config", "--global", "user.email", "JohnDoe@example.com");

        File remoteRepo = GlobalState.remoteRepo;
        String remotePath = remoteRepo.getAbsolutePath();
        runCommand(true, "remote", "add", "origin", remotePath);
    }

    @Given("^I have staged \"([^\"]*)\"$")
    public void I_have_staged(String feature) throws Throwable {
        if (feature.equals("points1")) {
            insertAndAdd(points1);
        } else if (feature.equals("points2")) {
            insertAndAdd(points2);
        } else if (feature.equals("points3")) {
            insertAndAdd(points3);
        } else if (feature.equals("points1_modified")) {
            insertAndAdd(points1_modified);
        } else if (feature.equals("lines1")) {
            insertAndAdd(lines1);
        } else if (feature.equals("lines2")) {
            insertAndAdd(lines2);
        } else if (feature.equals("lines3")) {
            insertAndAdd(lines3);
        } else {
            throw new Exception("Unknown Feature");
        }
    }

    @Given("^I have 6 unstaged features$")
    public void I_have_6_unstaged_features() throws Throwable {
        insertFeatures();
    }

    @Given("^I have unstaged \"([^\"]*)\"$")
    public void I_have_unstaged(String feature) throws Throwable {
        if (feature.equals("points1")) {
            insert(points1);
        } else if (feature.equals("points2")) {
            insert(points2);
        } else if (feature.equals("points3")) {
            insert(points3);
        } else if (feature.equals("points1_modified")) {
            insert(points1_modified);
        } else if (feature.equals("lines1")) {
            insert(lines1);
        } else if (feature.equals("lines2")) {
            insert(lines2);
        } else if (feature.equals("lines3")) {
            insert(lines3);
        } else {
            throw new Exception("Unknown Feature");
        }
    }

    @Given("^I have unstaged an empty feature type$")
    public void I_have_unstaged_an_empty_feature_type() throws Throwable {
        insert(points1);
        GeoGIT geogit = geogitCLI.newGeoGIT();
        final WorkingTree workTree = geogit.getRepository().getWorkingTree();
        workTree.delete(pointsName, idP1);
        geogit.close();
    }

    @Given("^I stage 6 features$")
    public void I_stage_6_features() throws Throwable {
        insertAndAddFeatures();
    }

    @Given("^I have several commits$")
    public void I_have_several_commits() throws Throwable {
        insertAndAdd(points1, points2);
        runCommand(true, "commit -m Commit1");
        insertAndAdd(points3, lines1);
        runCommand(true, "commit -m Commit2");
        insertAndAdd(lines2, lines3);
        runCommand(true, "commit -m Commit3");
        insertAndAdd(points1_modified);
        runCommand(true, "commit -m Commit4");
    }

    @Given("^I have several branches")
    public void I_have_several_branches() throws Throwable {
        insertAndAdd(points1);
        runCommand(true, "commit -m Commit1");
        runCommand(true, "branch -c branch1");
        insertAndAdd(points2);
        runCommand(true, "commit -m Commit2");
        insertAndAdd(points3);
        runCommand(true, "commit -m Commit3");
        runCommand(true, "branch -c branch2");
        insertAndAdd(lines1);
        runCommand(true, "commit -m Commit4");
        runCommand(true, "checkout master");
        insertAndAdd(lines2);
        runCommand(true, "commit -m Commit5");
    }

    @Given("I modify and add a feature")
    public void I_modify_and_add_a_feature() throws Throwable {
        insertAndAdd(points1_modified);
    }

    @Given("I modify a feature")
    public void I_modify_a_feature() throws Throwable {
        insert(points1_modified);
    }

    @Given("^I modify a feature type$")
    public void I_modify_a_feature_type() throws Throwable {
        deleteAndReplaceFeatureType();
    }

    @Then("^if I change to the respository subdirectory \"([^\"]*)\"$")
    public void if_I_change_to_the_respository_subdirectory(String subdirSpec) throws Throwable {
        String[] subdirs = subdirSpec.split("/");
        File dir = platform.pwd();
        for (String subdir : subdirs) {
            dir = new File(dir, subdir);
        }
        assertTrue(dir.exists());
        platform.setWorkingDir(dir);
        setupGeogit();
    }

    @Given("^I have a patch file$")
    public void I_have_a_patch_file() throws Throwable {
        Patch patch = new Patch();
        String path = NodeRef.appendChild(pointsName, points1.getIdentifier().getID());
        Map<PropertyDescriptor, AttributeDiff> map = Maps.newHashMap();
        Optional<?> oldValue = Optional.fromNullable(points1.getProperty("sp").getValue());
        GenericAttributeDiffImpl diff = new GenericAttributeDiffImpl(oldValue, Optional.of("new"));
        map.put(pointsType.getDescriptor("sp"), diff);
        FeatureDiff feaureDiff = new FeatureDiff(path, map, RevFeatureType.build(pointsType),
                RevFeatureType.build(pointsType));
        patch.addModifiedFeature(feaureDiff);
        File file = new File(platform.pwd(), "test.patch");
        BufferedWriter writer = Files.newWriter(file, Charsets.UTF_8);
        PatchSerializer.write(writer, patch);
        writer.flush();
        writer.close();
    }

    @Given("^I am inside a repository subdirectory \"([^\"]*)\"$")
    public void I_am_inside_a_repository_subdirectory(String subdirSpec) throws Throwable {
        String[] subdirs = subdirSpec.split("/");
        File dir = platform.pwd();
        for (String subdir : subdirs) {
            dir = new File(dir, subdir);
        }
        assertTrue(dir.mkdirs());
        platform.setWorkingDir(dir);
    }
}
