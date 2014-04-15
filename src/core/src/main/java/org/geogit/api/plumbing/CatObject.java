/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.RevObject;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.text.TextSerializationFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

/**
 * Provides content information for repository objects
 */
public class CatObject extends AbstractGeoGitOp<CharSequence> {

    private Supplier<? extends RevObject> object;

    public CatObject setObject(Supplier<? extends RevObject> object) {
        this.object = object;
        return this;
    }

    @Override
    public CharSequence call() {
        Preconditions.checkState(object != null);
        RevObject revObject = object.get();

        TextSerializationFactory factory = new TextSerializationFactory();
        ObjectWriter<RevObject> writer = factory.createObjectWriter(revObject.getType());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String s = "id\t" + revObject.getId().toString() + "\n";
        OutputStreamWriter streamWriter = new OutputStreamWriter(output, Charsets.UTF_8);
        try {
            streamWriter.write(s);
            streamWriter.flush();
            writer.write(revObject, output);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot print object: " + revObject.getId().toString(),
                    e);
        }
        return output.toString();
    }
}
