package org.geogit.cli.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.geogit.cli.CLICommand;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.StagingDatabase;

/**
 * Annotation for {@link CLICommand}s giving geogit a hint that the operation is read only for
 * {@link StagingDatabase}, but may need write access to {@link ObjectDatabase}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface StagingDatabaseReadOnly {

}
