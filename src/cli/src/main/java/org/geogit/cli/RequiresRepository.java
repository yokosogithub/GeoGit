/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a given {@link CLICommand} can only be run if a proper geogit
 * repository is in place, and hence {@link CLICommand#run(GeogitCLI)} is guaranteed to be called
 * with a non null {@link GeogitCLI#getGeogit() geogit} instance.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRepository {

}
