package org.geogit.api.porcelain;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;

public class CheckoutResult {

    public enum Results {
        NO_RESULT, DETACHED_HEAD, CHECKOUT_REMOTE_BRANCH, CHECKOUT_LOCAL_BRANCH, UPDATE_OBJECTS
    }

    private ObjectId oid = null;

    private Ref newRef = null;

    private String remoteName = null;

    private ObjectId newTree = null;

    private Results result = Results.NO_RESULT;

    public ObjectId getOid() {
        return oid;
    }

    public CheckoutResult setOid(ObjectId oid) {
        this.oid = oid;
        return this;
    }

    public Ref getNewRef() {
        return newRef;
    }

    public CheckoutResult setNewRef(Ref newRef) {
        this.newRef = newRef;
        return this;
    }

    public ObjectId getNewTree() {
        return newTree;
    }

    public CheckoutResult setNewTree(ObjectId newTree) {
        this.newTree = newTree;
        return this;
    }

    public Results getResult() {
        return result;
    }

    public CheckoutResult setResult(Results result) {
        if (result == Results.NO_RESULT) {
            this.result = result;
        }
        return this;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(String remoteName) {
        this.remoteName = remoteName;
    }

}
