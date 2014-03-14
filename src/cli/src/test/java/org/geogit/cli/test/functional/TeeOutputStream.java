/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli.test.functional;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class TeeOutputStream extends FilterOutputStream {

    protected OutputStream out2;

    public TeeOutputStream(OutputStream out1, OutputStream out2) {
        super(out1);
        this.out2 = out2;
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        this.out2.flush();
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.out2.close();
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        super.write(b);
        this.out2.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        this.out2.write(b, off, len);
    }

    @Override
    public synchronized void write(int b) throws IOException {
        super.write(b);
        this.out2.write(b);
    }

}