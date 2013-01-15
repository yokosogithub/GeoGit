package org.geogit.remote;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.repository.Repository;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

/**
 * An implementation of a remote repository that exists on a remote machine.
 * 
 * @see IRemoteRepo
 */
public class HTTPRemoteRepo implements IRemoteRepo {

    private URL repositoryURL;

    /**
     * Constructs a new {@code HTTPRemoteRepo} with the given parameters.
     * 
     * @param repositoryURL the url of the remote repository
     */
    public HTTPRemoteRepo(URL repositoryURL) {
        String url = repositoryURL.toString();
        if (url.endsWith("/")) {
            url = url.substring(0, url.lastIndexOf('/'));
        }
        try {
            this.repositoryURL = new URL(url);
        } catch (MalformedURLException e) {
            this.repositoryURL = repositoryURL;
        }
    }

    /**
     * Opens the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void open() throws IOException {

    }

    /**
     * Closes the remote repository.
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException {

    }

    /**
     * @return the remote's HEAD {@link Ref}.
     */
    @Override
    public Ref headRef() {
        HttpURLConnection connection = null;
        Ref headRef = null;
        try {
            String expanded = repositoryURL.toString() + "/repo/manifest";

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = rd.readLine()) != null) {
                if (line.startsWith("HEAD")) {
                    headRef = parseRef(line);
                }
            }
            rd.close();

        } catch (Exception e) {

            Throwables.propagate(e);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
        return headRef;
    }

    /**
     * List the remote's {@link Ref refs}.
     * 
     * @param getHeads whether to return refs in the {@code refs/heads} namespace
     * @param getTags whether to return refs in the {@code refs/tags} namespace
     * @return an immutable set of refs from the remote
     */
    @Override
    public ImmutableSet<Ref> listRefs(final boolean getHeads, final boolean getTags) {
        HttpURLConnection connection = null;
        ImmutableSet.Builder<Ref> builder = new ImmutableSet.Builder<Ref>();
        try {
            String expanded = repositoryURL.toString() + "/repo/manifest";
            // Create connection
            // String urlParameters = "repo/manifest";
            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");
            // connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // connection.setRequestProperty("Content-Length",
            // "" + Integer.toString(urlParameters.getBytes().length));
            // connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Send request
            // DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            // wr.writeBytes(urlParameters);
            // wr.flush();
            // wr.close();

            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = rd.readLine()) != null) {
                if ((getHeads && line.startsWith("refs/heads"))
                        || (getTags && line.startsWith("refs/tags"))) {
                    builder.add(parseRef(line));
                }
            }
            rd.close();

        } catch (Exception e) {

            Throwables.propagate(e);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
        return builder.build();
    }

    private Ref parseRef(String refString) {
        Ref ref = null;
        String[] tokens = refString.split(" ");
        if (tokens.length == 2) {
            // normal ref
            // NAME HASH
            ref = new Ref(tokens[0], ObjectId.valueOf(tokens[1]), RevObject.TYPE.COMMIT);
        } else {
            // symbolic ref
            // NAME TARGET HASH
            Ref target = new Ref(tokens[1], ObjectId.valueOf(tokens[2]), RevObject.TYPE.COMMIT);
            ref = new SymRef(tokens[0], target);

        }
        return ref;
    }

    /**
     * Fetch all new objects from the specified {@link Ref} from the remote.
     * 
     * @param localRepository the repository to add new objects to
     * @param ref the remote ref that points to new commit data
     */
    @Override
    public void fetchNewData(Repository localRepository, Ref ref) {
        walkCommit(ref.getObjectId(), localRepository, false);
    }

    /**
     * Push all new objects from the specified {@link Ref} to the remote.
     * 
     * @param localRepository the repository to get new objects from
     * @param ref the local ref that points to new commit data
     */
    @Override
    public void pushNewData(Repository localRepository, Ref ref) {
        walkCommit(ref.getObjectId(), localRepository, true);
        updateRemoteRef(ref.getName(), ref.getObjectId(), false);
    }

    @Override
    public void pushNewData(Repository localRepository, Ref ref, String refspec) {
        walkCommit(ref.getObjectId(), localRepository, true);
        updateRemoteRef(refspec, ref.getObjectId(), false);
    }

    @Override
    public void deleteRef(Repository localRepository, String refspec) {
        updateRemoteRef(refspec, null, true);
    }

    private void updateRemoteRef(String refspec, ObjectId newValue, boolean delete) {
        HttpURLConnection connection = null;
        try {
            String expanded;
            if (!delete) {
                expanded = repositoryURL.toString() + "/updateref?name=" + refspec + "&newValue="
                        + newValue.toString();
            } else {
                expanded = repositoryURL.toString() + "/updateref?name=" + refspec + "&delete=true";
            }

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            connection.getInputStream();

        } catch (Exception e) {

            Throwables.propagate(e);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void walkCommit(ObjectId commitId, Repository localRepo, boolean sendObject) {
        // See if we already have it
        if (sendObject) {

        } else if (localRepo.getObjectDatabase().exists(commitId)) {
            return;
        }

        Optional<RevObject> object = sendObject ? sendNetworkObject(commitId, localRepo)
                : getNetworkObject(commitId, localRepo);
        if (object.isPresent() && object.get().getType().equals(TYPE.COMMIT)) {
            RevCommit commit = (RevCommit) object.get();
            walkTree(commit.getTreeId(), localRepo, sendObject);

            for (ObjectId parentCommit : commit.getParentIds()) {
                walkCommit(parentCommit, localRepo, sendObject);
            }
        }
    }

    private void walkTree(ObjectId treeId, Repository localRepo, boolean sendObject) {
        // See if we already have it
        if (sendObject) {

        } else if (localRepo.getObjectDatabase().exists(treeId)) {
            return;
        }

        Optional<RevObject> object = sendObject ? sendNetworkObject(treeId, localRepo)
                : getNetworkObject(treeId, localRepo);
        if (object.isPresent() && object.get().getType().equals(TYPE.TREE)) {
            RevTree tree = (RevTree) object.get();

            walkLocalTree(tree, localRepo, sendObject);
        }
    }

    private void walkLocalTree(RevTree tree, Repository localRepo, boolean sendObject) {
        // walk subtrees
        if (tree.buckets().isPresent()) {
            for (ObjectId bucketId : tree.buckets().get().values()) {
                walkTree(bucketId, localRepo, sendObject);
            }
        } else {
            // get new objects
            for (Iterator<Node> children = tree.children(); children.hasNext();) {
                Node ref = children.next();
                moveObject(ref.getObjectId(), localRepo, sendObject);
                ObjectId metadataId = ref.getMetadataId().or(ObjectId.NULL);
                if (!metadataId.isNull()) {
                    moveObject(metadataId, localRepo, sendObject);
                }
            }
        }
    }

    private void moveObject(ObjectId objectId, Repository localRepo, boolean sendObject) {
        // See if we already have it
        if (sendObject) {

        } else if (localRepo.getObjectDatabase().exists(objectId)) {
            return;
        }

        Optional<RevObject> childObject = sendObject ? sendNetworkObject(objectId, localRepo)
                : getNetworkObject(objectId, localRepo);
        if (childObject.isPresent()) {
            RevObject revObject = childObject.get();
            if (TYPE.TREE.equals(revObject.getType())) {
                walkLocalTree((RevTree) revObject, localRepo, sendObject);
            }
        }
    }

    private Optional<RevObject> getNetworkObject(ObjectId objectId, Repository localRepo) {
        HttpURLConnection connection = null;
        try {
            String expanded = repositoryURL.toString() + "/repo/objects/" + objectId.toString();
            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Get Response
            InputStream is = connection.getInputStream();

            localRepo.getObjectDatabase().put(objectId, is);

        } catch (Exception e) {

            Throwables.propagate(e);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

        return localRepo.command(RevObjectParse.class).setObjectId(objectId).call();

    }

    private Optional<RevObject> sendNetworkObject(ObjectId objectId, Repository localRepo) {
        Optional<RevObject> object = localRepo.command(RevObjectParse.class).setObjectId(objectId)
                .call();

        HttpURLConnection connection = null;
        try {
            String expanded = repositoryURL.toString() + "/repo/sendobject";
            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("POST");

            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.write(objectId.getRawValue());
            InputStream rawObject = localRepo.getIndex().getDatabase().getRaw(objectId);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = rawObject.read(buffer)) != -1) {
                wr.write(buffer, 0, bytesRead);
            }
            wr.flush();
            wr.close();

            // Get Response
            InputStream is = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = rd.readLine()) != null) {
                if (line.contains("Object already existed")) {
                    return Optional.absent();
                }
            }
            rd.close();

        } catch (Exception e) {

            Throwables.propagate(e);

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
        return object;
    }
}
