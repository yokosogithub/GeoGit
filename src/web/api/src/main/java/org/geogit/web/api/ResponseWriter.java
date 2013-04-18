package org.geogit.web.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.codehaus.jettison.AbstractXMLStreamWriter;
import org.geogit.api.GeogitSimpleFeature;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.Remote;
import org.geogit.api.RevCommit;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTag;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.DiffIndex;
import org.geogit.api.plumbing.DiffWorkTree;
import org.geogit.api.plumbing.diff.AttributeDiff;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffEntry.ChangeType;
import org.opengis.feature.type.PropertyDescriptor;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Provides a wrapper for writing common GeoGit objects to a provided {@link XMLStreamWriter}.
 */
public class ResponseWriter {

    protected final XMLStreamWriter out;

    /**
     * Constructs a new {code ResponseWriter} with the given {@link XMLStreamWriter}.
     * 
     * @param out the output stream to write to
     */
    public ResponseWriter(XMLStreamWriter out) {
        this.out = out;
        if (out instanceof AbstractXMLStreamWriter) {
            configureJSONOutput((AbstractXMLStreamWriter) out);
        }
    }

    private void configureJSONOutput(AbstractXMLStreamWriter out) {
    }

    /**
     * Ends the document stream.
     * 
     * @throws XMLStreamException
     */
    public void finish() throws XMLStreamException {
        out.writeEndElement(); // results
        out.writeEndDocument();
    }

    /**
     * Begins the document stream.
     * 
     * @throws XMLStreamException
     */
    public void start() throws XMLStreamException {
        start(true);
    }

    /**
     * Begins the document stream with the provided success flag.
     * 
     * @param success whether or not the operation was successful
     * @throws XMLStreamException
     */
    public void start(boolean success) throws XMLStreamException {
        out.writeStartDocument();
        out.writeStartElement("response");
        writeElement("success", Boolean.toString(success));
    }

    /**
     * Writes the given header elements to the stream. The array should be organized into key/value
     * pairs. For example {@code [key, value, key, value]}.
     * 
     * @param els the elements to write
     * @throws XMLStreamException
     */
    public void writeHeaderElements(String... els) throws XMLStreamException {
        out.writeStartElement("header");
        for (int i = 0; i < els.length; i += 2) {
            writeElement(els[i], els[i + 1]);
        }
        out.writeEndElement();
    }

    /**
     * Writes the given error elements to the stream. The array should be organized into key/value
     * pairs. For example {@code [key, value, key, value]}.
     * 
     * @param errors the errors to write
     * @throws XMLStreamException
     */
    public void writeErrors(String... errors) throws XMLStreamException {
        out.writeStartElement("errors");
        for (int i = 0; i < errors.length; i += 2) {
            writeElement(errors[i], errors[i + 1]);
        }
        out.writeEndElement();
    }

    /**
     * @return the {@link XMLStreamWriter} for this instance
     */
    public XMLStreamWriter getWriter() {
        return out;
    }

    /**
     * Writes the given element to the stream.
     * 
     * @param element the element name
     * @param content the element content
     * @throws XMLStreamException
     */
    public void writeElement(String element, @Nullable String content) throws XMLStreamException {
        out.writeStartElement(element);
        if (content != null) {
            out.writeCharacters(content);
        }
        out.writeEndElement();
    }

    /**
     * Writes staged changes to the stream.
     * 
     * @param setFilter the configured {@link DiffIndex} command
     * @param start the change number to start writing from
     * @param length the number of changes to write
     * @throws XMLStreamException
     */
    public void writeStaged(DiffIndex setFilter, int start, int length) throws XMLStreamException {
        writeDiffEntries("staged", start, length, setFilter.call());
    }

    /**
     * Writes unstaged changes to the stream.
     * 
     * @param setFilter the configured {@link DiffWorkTree} command
     * @param start the change number to start writing from
     * @param length the number of changes to write
     * @throws XMLStreamException
     */
    public void writeUnstaged(DiffWorkTree setFilter, int start, int length)
            throws XMLStreamException {
        writeDiffEntries("unstaged", start, length, setFilter.call());
    }

    @SuppressWarnings("rawtypes")
    private void advance(Iterator it, int cnt) {
        for (int i = 0; i < cnt && it.hasNext(); i++) {
            it.next();
        }
    }

    /**
     * Writes a set of {@link DiffEntry}s to the stream.
     * 
     * @param name the element name
     * @param start the change number to start writing from
     * @param length the number of changes to write
     * @param entries an iterator for the DiffEntries to write
     * @throws XMLStreamException
     */
    public void writeDiffEntries(String name, int start, int length, Iterator<DiffEntry> entries)
            throws XMLStreamException {
        advance(entries, start);
        if (length < 0) {
            length = Integer.MAX_VALUE;
        }
        for (int i = 0; i < length && entries.hasNext(); i++) {
            DiffEntry entry = entries.next();
            out.writeStartElement(name);
            writeElement("changeType", entry.changeType().toString());
            NodeRef oldObject = entry.getOldObject();
            NodeRef newObject = entry.getNewObject();
            if (oldObject == null) {
                writeElement("newPath", newObject.path());
                writeElement("newObjectId", newObject.objectId().toString());
                writeElement("path", "");
                writeElement("oldObjectId", ObjectId.NULL.toString());
            } else if (newObject == null) {
                writeElement("newPath", "");
                writeElement("newObjectId", ObjectId.NULL.toString());
                writeElement("path", oldObject.path());
                writeElement("oldObjectId", oldObject.objectId().toString());
            } else {
                writeElement("newPath", newObject.path());
                writeElement("newObjectId", newObject.objectId().toString());
                writeElement("path", oldObject.path());
                writeElement("oldObjectId", oldObject.objectId().toString());

            }
            out.writeEndElement();
        }
    }

    /**
     * Writes a set of {@link RevCommit}s to the stream.
     * 
     * @param entries an iterator for the RevCommits to write
     * @param page the page number to write
     * @param elementsPerPage the number of commits per page
     * @throws XMLStreamException
     */
    public void writeCommits(Iterator<RevCommit> entries, int page, int elementsPerPage)
            throws XMLStreamException {
        advance(entries, page * elementsPerPage);
        int counter = 0;
        while (entries.hasNext() && counter < elementsPerPage) {
            RevCommit entry = entries.next();
            out.writeStartElement("commit");
            writeElement("id", entry.getId().toString());
            writeElement("tree", entry.getTreeId().toString());

            ImmutableList<ObjectId> parentIds = entry.getParentIds();
            out.writeStartElement("parents");
            for (ObjectId parentId : parentIds) {
                writeElement("id", parentId.toString());
            }
            out.writeEndElement();

            writePerson("author", entry.getAuthor());
            writePerson("committer", entry.getCommitter());

            out.writeStartElement("message");
            if (entry.getMessage() != null) {
                out.writeCData(entry.getMessage());
            }
            out.writeEndElement();

            out.writeEndElement();
            counter++;
        }
    }

    /**
     * Writes a {@link RevPerson} to the stream.
     * 
     * @param enclosingElement the element name
     * @param p the RevPerson to writes
     * @throws XMLStreamException
     */
    public void writePerson(String enclosingElement, RevPerson p) throws XMLStreamException {
        out.writeStartElement(enclosingElement);
        writeElement("name", p.getName().orNull());
        writeElement("email", p.getEmail().orNull());
        writeElement("timestamp", Long.toString(p.getTimestamp()));
        writeElement("timeZoneOffset", Long.toString(p.getTimeZoneOffset()));
        out.writeEndElement();
    }

    /**
     * Writes the response for the {@link Commit} command to the stream.
     * 
     * @param diff the changes returned from the command
     * @throws XMLStreamException
     */
    public void writeCommitResponse(Iterator<DiffEntry> diff) throws XMLStreamException {
        int adds = 0, deletes = 0, changes = 0;
        DiffEntry diffEntry;
        while (diff.hasNext()) {
            diffEntry = diff.next();
            switch (diffEntry.changeType()) {
            case ADDED:
                ++adds;
                break;
            case REMOVED:
                ++deletes;
                break;
            case MODIFIED:
                ++changes;
                break;
            }
        }
        writeElement("added", Integer.toString(adds));
        writeElement("changed", Integer.toString(changes));
        writeElement("deleted", Integer.toString(deletes));
    }

    /**
     * Writes the response for the {@link LsTree} command to the stream.
     * 
     * @param iter the iterator of {@link NodeRefs}
     * @param verbose if true, more detailed information about each node will be provided
     * @throws XMLStreamException
     */
    public void writeLsTreeResponse(Iterator<NodeRef> iter, boolean verbose)
            throws XMLStreamException {

        while (iter.hasNext()) {
            NodeRef node = iter.next();
            out.writeStartElement("node");
            writeElement("path", node.path());
            if (verbose) {
                writeElement("metadataId", node.getMetadataId().toString());
                writeElement("type", node.getType().toString().toLowerCase());
                writeElement("objectId", node.objectId().toString());
            }
            out.writeEndElement();
        }

    }

    /**
     * Writes the response for the {@link UpdateRefWeb} command to the stream.
     * 
     * @param ref the ref returned from the command
     * @throws XMLStreamException
     */
    public void writeUpdateRefResponse(Ref ref) throws XMLStreamException {
        out.writeStartElement("ChangedRef");
        writeElement("name", ref.getName());
        writeElement("objectId", ref.getObjectId().toString());
        if (ref instanceof SymRef) {
            writeElement("target", ((SymRef) ref).getTarget());
        }
        out.writeEndElement();
    }

    /**
     * Writes the response for the {@link RefParseWeb} command to the stream.
     * 
     * @param ref the ref returned from the command
     * @throws XMLStreamException
     */
    public void writeRefParseResponse(Ref ref) throws XMLStreamException {
        out.writeStartElement("Ref");
        writeElement("name", ref.getName());
        writeElement("objectId", ref.getObjectId().toString());
        if (ref instanceof SymRef) {
            writeElement("target", ((SymRef) ref).getTarget());
        }
        out.writeEndElement();
    }

    /**
     * Writes an empty ref response for when a {@link Ref} was not found.
     * 
     * @throws XMLStreamException
     */
    public void writeEmptyRefResponse() throws XMLStreamException {
        out.writeStartElement("RefNotFound");
        out.writeEndElement();
    }

    /**
     * Writes the response for the {@link BranchWebOp} command to the stream.
     * 
     * @param localBranches the local branches of the repository
     * @param remoteBranches the remote branches of the repository
     * @throws XMLStreamException
     */
    public void writeBranchListResponse(List<Ref> localBranches, List<Ref> remoteBranches)
            throws XMLStreamException {

        out.writeStartElement("Local");
        for (Ref branch : localBranches) {
            out.writeStartElement("Branch");
            writeElement("name", branch.localName());
            out.writeEndElement();
        }
        out.writeEndElement();

        out.writeStartElement("Remote");
        for (Ref branch : remoteBranches) {
            if (!(branch instanceof SymRef)) {
                out.writeStartElement("Branch");
                writeElement("remoteName", branch.namespace().replace(Ref.REMOTES_PREFIX + "/", ""));
                writeElement("name", branch.localName());
                out.writeEndElement();
            }
        }
        out.writeEndElement();

    }

    /**
     * Writes the response for the {@link RemoteWebOp} command to the stream.
     * 
     * @param remotes the list of the {@link Remote}s of this repository
     * @throws XMLStreamException
     */
    public void writeRemoteListResponse(List<Remote> remotes) throws XMLStreamException {
        for (Remote remote : remotes) {
            out.writeStartElement("Remote");
            writeElement("name", remote.getName());
            out.writeEndElement();
        }
    }

    /**
     * Writes the response for the {@link TagWebOp} command to the stream.
     * 
     * @param tags the list of {@link RevTag}s of this repository
     * @throws XMLStreamException
     */
    public void writeTagListResponse(List<RevTag> tags) throws XMLStreamException {
        for (RevTag tag : tags) {
            out.writeStartElement("Tag");
            writeElement("name", tag.getName());
            out.writeEndElement();
        }
    }

    /**
     * Writes a set of feature diffs to the stream.
     * 
     * @param diffs a map of {@link PropertyDescriptor} to {@link AttributeDiffs} that specify the
     *        difference between two features
     * @throws XMLStreamException
     */
    public void writeFeatureDiffResponse(Map<PropertyDescriptor, AttributeDiff> diffs)
            throws XMLStreamException {
        Set<Entry<PropertyDescriptor, AttributeDiff>> entries = diffs.entrySet();
        Iterator<Entry<PropertyDescriptor, AttributeDiff>> iter = entries.iterator();
        while (iter.hasNext()) {
            Entry<PropertyDescriptor, AttributeDiff> entry = iter.next();
            out.writeStartElement("diff");
            writeElement("attributename", entry.getKey().getName().toString());
            writeElement("changetype", entry.getValue().getType().toString());
            if (entry.getValue().getOldValue() != null
                    && entry.getValue().getOldValue().isPresent()) {
                writeElement("oldvalue", entry.getValue().getOldValue().get().toString());
            }
            if (entry.getValue().getNewValue() != null
                    && entry.getValue().getNewValue().isPresent()) {
                writeElement("newvalue", entry.getValue().getNewValue().get().toString());
            }
            out.writeEndElement();
        }
    }

    /**
     * Writes the response for all feature changes between two commits to the stream.
     * 
     * @param features the features that changed
     * @param changes the change type of each feature
     * @throws XMLStreamException
     */
    public void writeDiffResponse(Iterator<GeogitSimpleFeature> features,
            Iterator<ChangeType> changes) throws XMLStreamException {

        while (features.hasNext()) {
            GeogitSimpleFeature feature = features.next();
            ChangeType change = changes.next();
            out.writeStartElement("Feature");
            writeElement("change", change.toString());
            writeElement("id", feature.getID().toString());
            List<Object> attributes = feature.getAttributes();
            for (Object attribute : attributes) {
                if (attribute instanceof Geometry) {
                    writeElement("geometry", ((Geometry) attribute).toText());
                    break;
                }
            }

            out.writeEndElement();
        }

    }
}
