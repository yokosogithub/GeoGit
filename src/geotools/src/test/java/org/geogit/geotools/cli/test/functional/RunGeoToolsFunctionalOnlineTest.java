/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.geotools.cli.test.functional;

import org.junit.runner.RunWith;

import cucumber.junit.Cucumber;

/**
 * Single cucumber test runner. Its sole purpose is to serve as an entry point for junit. Step
 * definitions and hooks are defined in their own classes so they can be reused across features.
 * 
 */
@RunWith(Cucumber.class)
@Cucumber.Options(monochrome = true, format = { "pretty", "html:target/cucumber-report" }, strict = true, //
// the glue option tells cucumber where to look for step definitions
glue = { "org.geogit.cli.test.functional", "org.geogit.geotools.cli.test.functional" })
public class RunGeoToolsFunctionalOnlineTest {
}