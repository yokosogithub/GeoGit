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

import org.fusesource.jansi.Ansi;

/**
 *
 */
public class AnsiDecorator extends Ansi {

    private AnsiDecorator(StringBuilder sb) {
        super(sb);
    }

    public static Ansi newAnsi(boolean ansiSupported) {
        StringBuilder sb = new StringBuilder();
        return newAnsi(ansiSupported, sb);
    }

    public static Ansi newAnsi(boolean ansiSupported, StringBuilder sb) {
        Ansi ansi = new Ansi(sb);
        if (ansiSupported) {
            return ansi;
        }

        AnsiDecorator ansiDecorator = new AnsiDecorator(sb);
        return ansiDecorator;
    }

    @Override
    public Ansi a(Attribute ignored) {
        return this;
    }

    @Override
    public Ansi bg(Color c) {
        return this;
    }

    @Override
    public Ansi bold() {
        return this;
    }

    @Override
    public Ansi boldOff() {
        return this;
    }

    @Override
    public Ansi fg(Color c) {
        return this;
    }

}
