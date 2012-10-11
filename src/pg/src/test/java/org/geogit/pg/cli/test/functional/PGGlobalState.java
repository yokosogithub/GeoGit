/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.pg.cli.test.functional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 */
public class PGGlobalState {

    public static File currentDirectory;

    public static ByteArrayInputStream stdIn;

    public static ByteArrayOutputStream stdOut;

}
