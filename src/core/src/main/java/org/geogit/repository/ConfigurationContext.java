/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.repository;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ConfigurationContext extends ClassPathXmlApplicationContext {
    private static ConfigurationContext instance;

    public static synchronized ConfigurationContext getInstance() {
        if (instance == null) {
            // Stopwatch sw = new Stopwatch().start();
            instance = new ConfigurationContext();
            // sw.stop();
            // System.err.println("App context loaded in " + sw);
        }
        return instance;
    }

    private ConfigurationContext() {
        super("org/geogit/storage/applicationContext.xml");

    }
}
