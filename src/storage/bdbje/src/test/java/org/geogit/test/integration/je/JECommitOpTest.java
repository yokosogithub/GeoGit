/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration.je;

import org.geogit.di.GeogitModule;
import org.geogit.di.PlumbingCommands;
import org.geogit.di.PorcelainCommands;
import org.geogit.storage.bdbje.JEStorageModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class JECommitOpTest extends org.geogit.test.integration.CommitOpTest {
    @Override
    protected Injector createInjector() {
        return Guice.createInjector(new GeogitModule(), new JEStorageModule(),
                new PlumbingCommands(), new PorcelainCommands());
    }

}
