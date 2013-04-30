/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.web.api.repo;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.geogit.api.CommitBuilder;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.WriteTree;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.remote.BinaryPackedChanges;
import org.geogit.remote.HttpFilteredDiffIterator;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.ObjectSerializingFactory;
import org.geogit.storage.datastream.DataStreamSerializationFactory;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.google.common.base.Optional;
import com.google.common.base.Suppliers;

public class ApplyChangesResource extends ServerResource {
    @Override
    protected Representation post(Representation entity) throws ResourceException {
        InputStream input = null;
        ObjectId newCommitId = ObjectId.NULL;
        try {
            input = getRequest().getEntity().getStream();
            final GeoGIT ggit = (GeoGIT) getApplication().getContext().getAttributes()
                    .get("geogit");

            final Repository repository = ggit.getRepository();

            // read in commit object
            final ObjectSerializingFactory factory = new DataStreamSerializationFactory();
            ObjectReader<RevCommit> reader = factory.createCommitReader();
            RevCommit commit = reader.read(ObjectId.NULL, input); // I don't need to know the
                                                                  // original ObjectId

            // read in parents
            List<ObjectId> newParents = new LinkedList<ObjectId>();
            int numParents = input.read();
            for (int i = 0; i < numParents; i++) {
                ObjectId parentId = readObjectId(input);
                newParents.add(parentId);
            }

            // read in the changes
            BinaryPackedChanges unpacker = new BinaryPackedChanges(repository);
            Iterator<DiffEntry> changes = new HttpFilteredDiffIterator(input, unpacker);

            RevTree rootTree = RevTree.EMPTY;

            if (newParents.size() > 0) {
                ObjectId mappedCommit = newParents.get(0);

                Optional<ObjectId> treeId = repository.command(ResolveTreeish.class)
                        .setTreeish(mappedCommit).call();
                if (treeId.isPresent()) {
                    rootTree = repository.getTree(treeId.get());
                }
            }

            // Create new commit
            ObjectId newTreeId = repository.command(WriteTree.class)
                    .setOldRoot(Suppliers.ofInstance(rootTree))
                    .setDiffSupplier(Suppliers.ofInstance((Iterator<DiffEntry>) changes)).call();

            CommitBuilder builder = new CommitBuilder(commit);

            builder.setParentIds(newParents);
            builder.setTreeId(newTreeId);

            RevCommit mapped = builder.build();
            repository.getObjectDatabase().put(mapped);
            newCommitId = mapped.getId();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new StringRepresentation(newCommitId.toString(), MediaType.TEXT_PLAIN);
    }

    private ObjectId readObjectId(final InputStream in) throws IOException {
        byte[] rawBytes = new byte[20];
        int amount = 0;
        int len = 20;
        int offset = 0;
        while ((amount = in.read(rawBytes, offset, len - offset)) != 0) {
            if (amount < 0)
                throw new EOFException("Came to end of input");
            offset += amount;
            if (offset == len)
                break;
        }
        ObjectId id = new ObjectId(rawBytes);
        return id;
    }
}
