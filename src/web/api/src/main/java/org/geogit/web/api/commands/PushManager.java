package org.geogit.web.api.commands;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;

public class PushManager {

    private Map<String, List<ObjectId>> incomingData;

    private static PushManager instance = new PushManager();

    private PushManager() {
        incomingData = new HashMap<String, List<ObjectId>>();
    }

    public static PushManager get() {
        return instance;
    }

    public void connectionBegin(String ipAddress) {
        if (incomingData.containsKey(ipAddress)) {
            incomingData.remove(ipAddress);
        }
        List<ObjectId> newList = new LinkedList<ObjectId>();
        incomingData.put(ipAddress, newList);
    }

    public void connectionSucceeded(GeoGIT geogit, String ipAddress) {
        // Add objects to the repository
        if (incomingData.containsKey(ipAddress)) {
            List<ObjectId> objectsToMove = incomingData.remove(ipAddress);
            for (ObjectId oid : objectsToMove) {
                RevObject toAdd = geogit.getRepository().getIndex().getDatabase().get(oid);
                geogit.getRepository().getObjectDatabase().put(toAdd);
            }
        } else {
            throw new RuntimeException("Tried to end a connection that didn't exist.");
        }
    }

    public boolean alreadyPushed(String ipAddress, ObjectId oid) {
        if (incomingData.containsKey(ipAddress)) {
            return incomingData.get(ipAddress).contains(oid);
        }
        return false;
    }

    public void addObject(String ipAddress, ObjectId oid) {
        if (incomingData.containsKey(ipAddress)) {
            incomingData.get(ipAddress).add(oid);
        } else {
            throw new RuntimeException(
                    "Tried to push an object without first opening a connection.");
        }
    }

}
