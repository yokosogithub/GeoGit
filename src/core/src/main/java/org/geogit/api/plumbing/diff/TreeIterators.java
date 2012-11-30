//package org.geogit.api.plumbing.diff;
//
//import java.util.Iterator;
//
//import org.geogit.api.NodeRef;
//import org.geogit.api.RevTree;
//import org.geogit.storage.ObjectDatabase;
//
//public class TreeIterators {
//
//    /**
//     * Returns an iterator with the children of a given tree
//     * 
//     * @param revTree the parent tree
//     * @param db the object database
//     * @return an iterator with the children of a given tree
//     */
//    public static Iterator<NodeRef> childrenIterator(RevTree revTree, ObjectDatabase db) {
//        return new DepthTreeIterator(revTree, db, DepthTreeIterator.Strategy.CHILDREN);
//    }
//
//    /**
//     * Returns an iterator with the features under a given tree
//     * 
//     * @param revTree the parent tree
//     * @param db the object database
//     * @return an iterator with the features of a given tree
//     */
//    public static Iterator<NodeRef> featuresIterator(RevTree revTree, ObjectDatabase db) {
//        return new DepthTreeIterator(revTree, db, DepthTreeIterator.Strategy.FEATURES_ONLY);
//    }
//
//    /**
//     * Returns an iterator with the trees under a given tree
//     * 
//     * @param revTree the parent tree
//     * @param db the object database
//     * @return an iterator with the trees under a given tree
//     */
//    public static Iterator<NodeRef> treesIterator(RevTree revTree, ObjectDatabase db) {
//        return new DepthTreeIterator(revTree, db, DepthTreeIterator.Strategy.TREES_ONLY);
//    }
//
// }
