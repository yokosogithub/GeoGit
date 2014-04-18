<<<<<<< .merge_file_UldnLm
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject;
import org.geogit.api.SymRef;
import org.geogit.repository.Repository;
import org.geogit.storage.datastream.ObjectReader;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.io.Closeables;

/**
 * Utility functions for performing common communications and operations with http remotes.
 */
class HttpUtils {

    /**
     * Parse the provided ref string to a {@link Ref}. The input string should be in the following
     * format:
     * <p>
     * 'NAME HASH' for a normal ref. e.g. 'refs/heads/master abcd1234ef567890dcba'
     * <p>
     * 'NAME TARGET HASH' for a symbolic ref. e.g. 'HEAD refs/heads/master abcd1234ef567890dcba'
     * 
     * @param refString the string to parse
     * @return the parsed ref
     */
    public static Ref parseRef(String refString) {
        Ref ref = null;
        String[] tokens = refString.split(" ");
        if (tokens.length == 2) {
            // normal ref
            // NAME HASH
            String name = tokens[0];
            ObjectId objectId = ObjectId.valueOf(tokens[1]);
            ref = new Ref(name, objectId);
        } else {
            // symbolic ref
            // NAME TARGET HASH
            String name = tokens[0];
            String targetRef = tokens[1];
            ObjectId targetObjectId = ObjectId.valueOf(tokens[2]);
            Ref target = new Ref(targetRef, targetObjectId);
            ref = new SymRef(name, target);

        }
        return ref;
    }

    /**
     * Consumes the error stream of the provided connection and then closes it.
     * 
     * @param connection the connection to close
     */
    public static void consumeErrStreamAndCloseConnection(@Nullable HttpURLConnection connection) {
        if (connection == null) {
            return;
        }
        try {
            InputStream es = ((HttpURLConnection) connection).getErrorStream();
            consumeAndCloseStream(es);
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Consumes the provided input stream and then closes it.
     * 
     * @param stream the stream to consume and close
     * @throws IOException
     */
    public static void consumeAndCloseStream(InputStream stream) throws IOException {
        if (stream != null) {
            try {
                // read the response body
                while (stream.read() > -1) {
                    ; // $codepro.audit.disable extraSemicolon
                }
            } finally {
                // close the errorstream
                Closeables.closeQuietly(stream);
            }
        }
    }

    /**
     * Reads from the provided XML stream until an element with a name that matches the provided
     * name is found.
     * 
     * @param reader the XML stream
     * @param name the element name to search for
     * @throws XMLStreamException
     */
    public static void readToElementStart(XMLStreamReader reader, String name)
            throws XMLStreamException {
        while (reader.hasNext()) {
            if (reader.isStartElement() && reader.getLocalName().equals(name)) {
                break;
            }
            reader.next();
        }
    }

    /**
     * Retrieves a {@link RevObject} from the remote repository.
     * 
     * @param repositoryURL the URL of the repository
     * @param localRepository the repository to save the object to, if {@code null}, the object will
     *        not be saved
     * @param objectId the id of the object to retrieve
     * @return the retrieved object, or {@link Optional#absent()} if the object was not found
     */
    public static Optional<RevObject> getNetworkObject(URL repositoryURL,
            @Nullable Repository localRepository, ObjectId objectId) {
        HttpURLConnection connection = null;
        Optional<RevObject> object = Optional.absent();
        try {
            String expanded = repositoryURL.toString() + "/repo/objects/" + objectId.toString();
            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                ObjectReader reader = new ObjectReader();
                RevObject revObject = reader.read(objectId, is);
                if (localRepository != null) {
                    localRepository.getObjectDatabase().put(revObject);
                }
                object = Optional.of(revObject);
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }

        return object;

    }

    /**
     * Determines whether or not an object with the given {@link ObjectId} exists in the remote
     * repository.
     * 
     * @param repositoryURL the URL of the repository
     * @param objectId the id to check for
     * @return true if the object existed, false otherwise
     */
    public static boolean networkObjectExists(URL repositoryURL, ObjectId objectId) {
        HttpURLConnection connection = null;
        boolean exists = false;
        try {
            String internalIp = InetAddress.getLocalHost().getHostName();
            String expanded = repositoryURL.toString() + "/repo/exists?oid=" + objectId.toString()
                    + "&internalIp=" + internalIp;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line = rd.readLine();
                Preconditions.checkNotNull(line, "networkObjectExists returned no dat for %s",
                        expanded);
                exists = line.length() > 0 && line.charAt(0) == '1';
            } finally {
                consumeAndCloseStream(is);
            }

        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return exists;
    }

    /**
     * Updates the ref on the remote repository that matches the provided refspec to the new value.
     * 
     * @param repositoryURL the URL of the repository
     * @param refspec the refspec of the ref to update
     * @param newValue the new value for the ref
     * @param delete if true, the ref will be deleted
     * @return the updated ref
     */
    public static Ref updateRemoteRef(URL repositoryURL, String refspec, ObjectId newValue,
            boolean delete) {
        HttpURLConnection connection = null;
        Ref updatedRef = null;
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

            InputStream inputStream = connection.getInputStream();

            XMLStreamReader reader = XMLInputFactory.newFactory()
                    .createXMLStreamReader(inputStream);

            try {
                readToElementStart(reader, "ChangedRef");

                readToElementStart(reader, "name");
                final String refName = reader.getElementText();

                readToElementStart(reader, "objectId");
                final String objectId = reader.getElementText();

                readToElementStart(reader, "target");
                String target = null;
                if (reader.hasNext()) {
                    target = reader.getElementText();
                }
                reader.close();

                if (target != null) {
                    updatedRef = new SymRef(refName, new Ref(target, ObjectId.valueOf(objectId)));
                } else {
                    updatedRef = new Ref(refName, ObjectId.valueOf(objectId));
                }

            } finally {
                reader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return updatedRef;
    }

    /**
     * Gets the depth of the repository or commit if provided.
     * 
     * @param repositoryURL the URL of the repository
     * @param commit the commit whose depth should be determined, if null, the repository depth will
     *        be returned
     * @return the depth of the repository or commit, or {@link Optional#absent()} if the repository
     *         is not shallow or the commit was not found
     */
    public static Optional<Integer> getDepth(URL repositoryURL, @Nullable String commit) {
        HttpURLConnection connection = null;
        Optional<String> commitId = Optional.fromNullable(commit);
        Optional<Integer> depth = Optional.absent();
        try {
            String expanded;
            if (commitId.isPresent()) {
                expanded = repositoryURL.toString() + "/repo/getdepth?commitId=" + commitId.get();
            } else {
                expanded = repositoryURL.toString() + "/repo/getdepth";
            }

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line = rd.readLine();
                if (line != null) {
                    depth = Optional.of(Integer.parseInt(line));
                }
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return depth;
    }

    /**
     * Gets the parents of the specified commit from the remote repository.
     * 
     * @param repositoryURL the URL of the repository
     * @param commit the id of the commit whose parents to retrieve
     * @return a list of parent ids for the commit
     */
    public static ImmutableList<ObjectId> getParents(URL repositoryURL, ObjectId commit) {
        HttpURLConnection connection = null;
        Builder<ObjectId> listBuilder = new ImmutableList.Builder<ObjectId>();
        try {
            String expanded = repositoryURL.toString() + "/repo/getparents?commitId="
                    + commit.toString();

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));

                String line = rd.readLine();
                while (line != null) {
                    listBuilder.add(ObjectId.valueOf(line));
                    line = rd.readLine();
                }
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return listBuilder.build();
    }

    /**
     * Retrieves the remote ref that matches the provided refspec.
     * 
     * @param repositoryURL the URL of the repository
     * @param refspec the refspec to search for
     * @return the remote ref, or {@link Optional#absent()} if it wasn't found
     */
    public static Optional<Ref> getRemoteRef(URL repositoryURL, String refspec) {
        HttpURLConnection connection = null;
        Optional<Ref> remoteRef = Optional.absent();
        try {
            String expanded = repositoryURL.toString() + "/refparse?name=" + refspec;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            InputStream inputStream = connection.getInputStream();

            XMLStreamReader reader = XMLInputFactory.newFactory()
                    .createXMLStreamReader(inputStream);

            try {
                HttpUtils.readToElementStart(reader, "Ref");
                if (reader.hasNext()) {

                    HttpUtils.readToElementStart(reader, "name");
                    final String refName = reader.getElementText();

                    HttpUtils.readToElementStart(reader, "objectId");
                    final String objectId = reader.getElementText();

                    HttpUtils.readToElementStart(reader, "target");
                    String target = null;
                    if (reader.hasNext()) {
                        target = reader.getElementText();
                    }
                    reader.close();

                    if (target != null) {
                        remoteRef = Optional.of((Ref) new SymRef(refName, new Ref(target, ObjectId
                                .valueOf(objectId))));
                    } else {
                        remoteRef = Optional.of(new Ref(refName, ObjectId.valueOf(objectId)));
                    }
                }

            } finally {
                reader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            HttpUtils.consumeErrStreamAndCloseConnection(connection);
        }
        return remoteRef;
    }

    /**
     * Retrieves a list of features that were modified or deleted by a particular commit.
     * 
     * @param repositoryURL the URL of the repository
     * @param commit the id of the commit to check
     * @return a list of features affected by the commit
     */
    public static ImmutableList<ObjectId> getAffectedFeatures(URL repositoryURL, ObjectId commit) {
        HttpURLConnection connection = null;
        Builder<ObjectId> listBuilder = new ImmutableList.Builder<ObjectId>();
        try {
            String expanded = repositoryURL.toString() + "/repo/affectedfeatures?commitId="
                    + commit.toString();

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));

                String line = rd.readLine();
                while (line != null) {
                    listBuilder.add(ObjectId.valueOf(line));
                    line = rd.readLine();
                }
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return listBuilder.build();
    }

    /**
     * Begins a push operation to the target repository.
     * 
     * @param repositoryURL the URL of the repository
     */
    public static void beginPush(URL repositoryURL) {
        HttpURLConnection connection = null;
        try {
            String internalIp = InetAddress.getLocalHost().getHostName();
            String expanded = repositoryURL.toString() + "/repo/beginpush?internalIp=" + internalIp;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            InputStream stream = connection.getInputStream();
            HttpUtils.consumeAndCloseStream(stream);

        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            HttpUtils.consumeErrStreamAndCloseConnection(connection);
        }
    }

    /**
     * Finalizes a push operation to the target repository. If the ref that we are pushing to was
     * changed during push, the remote ref will not be updated.
     * 
     * @param repositoryURL the URL of the repository
     * @param refspec the refspec we are pushing to
     * @param newCommitId the new value of the ref
     * @param originalRefValue the value of the ref when we started pushing
     */
    public static void endPush(URL repositoryURL, String refspec, ObjectId newCommitId,
            String originalRefValue) {
        HttpURLConnection connection = null;
        try {
            String internalIp = InetAddress.getLocalHost().getHostName();
            String expanded = repositoryURL.toString() + "/repo/endpush?refspec=" + refspec
                    + "&objectId=" + newCommitId.toString() + "&internalIp=" + internalIp
                    + "&originalRefValue=" + originalRefValue;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            connection.getInputStream();
            // TODO: throw an exception if the remote ref was not updated.

        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            HttpUtils.consumeErrStreamAndCloseConnection(connection);
        }
    }
}
=======
/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevObject;
import org.geogit.api.SymRef;
import org.geogit.repository.Repository;
import org.geogit.storage.datastream.ObjectReader;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.io.Closeables;

/**
 * Utility functions for performing common communications and operations with http remotes.
 */
class HttpUtils {

    /**
     * Parse the provided ref string to a {@link Ref}. The input string should be in the following
     * format:
     * <p>
     * 'NAME HASH' for a normal ref. e.g. 'refs/heads/master abcd1234ef567890dcba'
     * <p>
     * 'NAME TARGET HASH' for a symbolic ref. e.g. 'HEAD refs/heads/master abcd1234ef567890dcba'
     * 
     * @param refString the string to parse
     * @return the parsed ref
     */
    public static Ref parseRef(String refString) {
        Ref ref = null;
        String[] tokens = refString.split(" ");
        if (tokens.length == 2) {
            // normal ref
            // NAME HASH
            String name = tokens[0];
            ObjectId objectId = ObjectId.valueOf(tokens[1]);
            ref = new Ref(name, objectId);
        } else {
            // symbolic ref
            // NAME TARGET HASH
            String name = tokens[0];
            String targetRef = tokens[1];
            ObjectId targetObjectId = ObjectId.valueOf(tokens[2]);
            Ref target = new Ref(targetRef, targetObjectId);
            ref = new SymRef(name, target);

        }
        return ref;
    }

    /**
     * Consumes the error stream of the provided connection and then closes it.
     * 
     * @param connection the connection to close
     */
    public static void consumeErrStreamAndCloseConnection(@Nullable HttpURLConnection connection) {
        if (connection == null) {
            return;
        }
        try {
            InputStream es = ((HttpURLConnection) connection).getErrorStream();
            consumeAndCloseStream(es);
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Consumes the provided input stream and then closes it.
     * 
     * @param stream the stream to consume and close
     * @throws IOException
     */
    public static void consumeAndCloseStream(InputStream stream) throws IOException {
        if (stream != null) {
            try {
                // read the response body
                while (stream.read() > -1) {
                    ; // $codepro.audit.disable extraSemicolon
                }
            } finally {
                // close the errorstream
                Closeables.closeQuietly(stream);
            }
        }
    }

    /**
     * Reads from the provided XML stream until an element with a name that matches the provided
     * name is found.
     * 
     * @param reader the XML stream
     * @param name the element name to search for
     * @throws XMLStreamException
     */
    public static void readToElementStart(XMLStreamReader reader, String name)
            throws XMLStreamException {
        while (reader.hasNext()) {
            if (reader.isStartElement() && reader.getLocalName().equals(name)) {
                break;
            }
            reader.next();
        }
    }

    /**
     * Retrieves a {@link RevObject} from the remote repository.
     * 
     * @param repositoryURL the URL of the repository
     * @param localRepository the repository to save the object to, if {@code null}, the object will
     *        not be saved
     * @param objectId the id of the object to retrieve
     * @return the retrieved object, or {@link Optional#absent()} if the object was not found
     */
    public static Optional<RevObject> getNetworkObject(URL repositoryURL,
            @Nullable Repository localRepository, ObjectId objectId) {
        HttpURLConnection connection = null;
        Optional<RevObject> object = Optional.absent();
        try {
            String expanded = repositoryURL.toString() + "/repo/objects/" + objectId.toString();
            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                ObjectReader reader = new ObjectReader();
                RevObject revObject = reader.read(objectId, is);
                if (localRepository != null) {
                    localRepository.getObjectDatabase().put(revObject);
                }
                object = Optional.of(revObject);
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }

        return object;

    }

    /**
     * Determines whether or not an object with the given {@link ObjectId} exists in the remote
     * repository.
     * 
     * @param repositoryURL the URL of the repository
     * @param objectId the id to check for
     * @return true if the object existed, false otherwise
     */
    public static boolean networkObjectExists(URL repositoryURL, ObjectId objectId) {
        HttpURLConnection connection = null;
        boolean exists = false;
        try {
            String internalIp = InetAddress.getLocalHost().getHostName();
            String expanded = repositoryURL.toString() + "/repo/exists?oid=" + objectId.toString()
                    + "&internalIp=" + internalIp;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line = rd.readLine();
                Preconditions.checkNotNull(line, "networkObjectExists returned no dat for %s",
                        expanded);
                exists = line.length() > 0 && line.charAt(0) == '1';
            } finally {
                consumeAndCloseStream(is);
            }

        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return exists;
    }

    /**
     * Updates the ref on the remote repository that matches the provided refspec to the new value.
     * 
     * @param repositoryURL the URL of the repository
     * @param refspec the refspec of the ref to update
     * @param newValue the new value for the ref
     * @param delete if true, the ref will be deleted
     * @return the updated ref
     */
    public static Ref updateRemoteRef(URL repositoryURL, String refspec, ObjectId newValue,
            boolean delete) {
        HttpURLConnection connection = null;
        Ref updatedRef = null;
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
            connection.connect();

            InputStream inputStream = connection.getInputStream();

            XMLStreamReader reader = XMLInputFactory.newFactory()
                    .createXMLStreamReader(inputStream);

            try {
                readToElementStart(reader, "ChangedRef");

                readToElementStart(reader, "name");
                final String refName = reader.getElementText();

                readToElementStart(reader, "objectId");
                final String objectId = reader.getElementText();

                readToElementStart(reader, "target");
                String target = null;
                if (reader.hasNext()) {
                    target = reader.getElementText();
                }
                reader.close();

                if (target != null) {
                    updatedRef = new SymRef(refName, new Ref(target, ObjectId.valueOf(objectId)));
                } else {
                    updatedRef = new Ref(refName, ObjectId.valueOf(objectId));
                }

            } finally {
                reader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return updatedRef;
    }

    /**
     * Gets the depth of the repository or commit if provided.
     * 
     * @param repositoryURL the URL of the repository
     * @param commit the commit whose depth should be determined, if null, the repository depth will
     *        be returned
     * @return the depth of the repository or commit, or {@link Optional#absent()} if the repository
     *         is not shallow or the commit was not found
     */
    public static Optional<Integer> getDepth(URL repositoryURL, @Nullable String commit) {
        HttpURLConnection connection = null;
        Optional<String> commitId = Optional.fromNullable(commit);
        Optional<Integer> depth = Optional.absent();
        try {
            String expanded;
            if (commitId.isPresent()) {
                expanded = repositoryURL.toString() + "/repo/getdepth?commitId=" + commitId.get();
            } else {
                expanded = repositoryURL.toString() + "/repo/getdepth";
            }

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line = rd.readLine();
                if (line != null) {
                    depth = Optional.of(Integer.parseInt(line));
                }
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return depth;
    }

    /**
     * Gets the parents of the specified commit from the remote repository.
     * 
     * @param repositoryURL the URL of the repository
     * @param commit the id of the commit whose parents to retrieve
     * @return a list of parent ids for the commit
     */
    public static ImmutableList<ObjectId> getParents(URL repositoryURL, ObjectId commit) {
        HttpURLConnection connection = null;
        Builder<ObjectId> listBuilder = new ImmutableList.Builder<ObjectId>();
        try {
            String expanded = repositoryURL.toString() + "/repo/getparents?commitId="
                    + commit.toString();

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));

                String line = rd.readLine();
                while (line != null) {
                    listBuilder.add(ObjectId.valueOf(line));
                    line = rd.readLine();
                }
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return listBuilder.build();
    }

    /**
     * Retrieves the remote ref that matches the provided refspec.
     * 
     * @param repositoryURL the URL of the repository
     * @param refspec the refspec to search for
     * @return the remote ref, or {@link Optional#absent()} if it wasn't found
     */
    public static Optional<Ref> getRemoteRef(URL repositoryURL, String refspec) {
        HttpURLConnection connection = null;
        Optional<Ref> remoteRef = Optional.absent();
        try {
            String expanded = repositoryURL.toString() + "/refparse?name=" + refspec;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            InputStream inputStream = connection.getInputStream();

            XMLStreamReader reader = XMLInputFactory.newFactory()
                    .createXMLStreamReader(inputStream);

            try {
                HttpUtils.readToElementStart(reader, "Ref");
                if (reader.hasNext()) {

                    HttpUtils.readToElementStart(reader, "name");
                    final String refName = reader.getElementText();

                    HttpUtils.readToElementStart(reader, "objectId");
                    final String objectId = reader.getElementText();

                    HttpUtils.readToElementStart(reader, "target");
                    String target = null;
                    if (reader.hasNext()) {
                        target = reader.getElementText();
                    }
                    reader.close();

                    if (target != null) {
                        remoteRef = Optional.of((Ref) new SymRef(refName, new Ref(target, ObjectId
                                .valueOf(objectId))));
                    } else {
                        remoteRef = Optional.of(new Ref(refName, ObjectId.valueOf(objectId)));
                    }
                }

            } finally {
                reader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            HttpUtils.consumeErrStreamAndCloseConnection(connection);
        }
        return remoteRef;
    }

    /**
     * Retrieves a list of features that were modified or deleted by a particular commit.
     * 
     * @param repositoryURL the URL of the repository
     * @param commit the id of the commit to check
     * @return a list of features affected by the commit
     */
    public static ImmutableList<ObjectId> getAffectedFeatures(URL repositoryURL, ObjectId commit) {
        HttpURLConnection connection = null;
        Builder<ObjectId> listBuilder = new ImmutableList.Builder<ObjectId>();
        try {
            String expanded = repositoryURL.toString() + "/repo/affectedfeatures?commitId="
                    + commit.toString();

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            // Get Response
            InputStream is = connection.getInputStream();
            try {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));

                String line = rd.readLine();
                while (line != null) {
                    listBuilder.add(ObjectId.valueOf(line));
                    line = rd.readLine();
                }
            } finally {
                consumeAndCloseStream(is);
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            consumeErrStreamAndCloseConnection(connection);
        }
        return listBuilder.build();
    }

    /**
     * Begins a push operation to the target repository.
     * 
     * @param repositoryURL the URL of the repository
     */
    public static void beginPush(URL repositoryURL) {
        HttpURLConnection connection = null;
        try {
            String internalIp = InetAddress.getLocalHost().getHostName();
            String expanded = repositoryURL.toString() + "/repo/beginpush?internalIp=" + internalIp;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            InputStream stream = connection.getInputStream();
            HttpUtils.consumeAndCloseStream(stream);

        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            HttpUtils.consumeErrStreamAndCloseConnection(connection);
        }
    }

    /**
     * Finalizes a push operation to the target repository. If the ref that we are pushing to was
     * changed during push, the remote ref will not be updated.
     * 
     * @param repositoryURL the URL of the repository
     * @param refspec the refspec we are pushing to
     * @param newCommitId the new value of the ref
     * @param originalRefValue the value of the ref when we started pushing
     */
    public static void endPush(URL repositoryURL, String refspec, ObjectId newCommitId,
            String originalRefValue) {
        HttpURLConnection connection = null;
        try {
            String internalIp = InetAddress.getLocalHost().getHostName();
            String expanded = repositoryURL.toString() + "/repo/endpush?refspec=" + refspec
                    + "&objectId=" + newCommitId.toString() + "&internalIp=" + internalIp
                    + "&originalRefValue=" + originalRefValue;

            connection = (HttpURLConnection) new URL(expanded).openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.connect();

            connection.getInputStream();
            // TODO: throw an exception if the remote ref was not updated.

        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            HttpUtils.consumeErrStreamAndCloseConnection(connection);
        }
    }
}
>>>>>>> .merge_file_l9B7po
