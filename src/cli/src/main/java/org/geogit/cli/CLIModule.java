/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.cli;

import java.util.ServiceLoader;

import com.google.inject.Module;

/**
 * Marker interface for modules that provide extra CLI commands.
 * 
 * <p>
 * The {@link GeogitCLI CLI} app uses the standard {@link ServiceLoader}
 * "Service Provider Interface" mechanism to look for implementations of this interface on the
 * classpath.
 * <p>
 * Any CLI plugin that provides extra command line commands shall include a file named
 * {@code org.geogit.cli.CLIModule} text file inside the jar's {@code META-INF/services} folder,
 * whose content is the full qualified class name of the module implementation. There can be more
 * than one module declared on each file, separated by a newline.
 */
public interface CLIModule extends Module {

}
