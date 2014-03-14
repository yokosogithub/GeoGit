/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli.test.functional;

import org.junit.runner.RunWith;

import cucumber.junit.Cucumber;

/**
 * Single cucumber test runner. Its sole purpose is to serve as an entry point for junit. Step
 * definitions and hooks are defined in their own classes so they can be reused across features.
 * 
 */
// use features=... to specify one or more specific features to execute
// @Cucumber.Options(features = { "src/test/resources/org/geogit/cli/test/functional/Branch.feature"
// }, monochrome = true, format = {
// "pretty", "html:target/cucumber-report" }, strict = true)
@Cucumber.Options(monochrome = true, format = { "pretty", "html:target/cucumber-report" }, strict = true)
@RunWith(Cucumber.class)
public class RunFunctionalTest {
}