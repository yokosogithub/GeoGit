/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.test.integration.repository;

import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

/**
 *
 */
public class WorkingTreeTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        //
    }

    @Test
    public void testInitFeatureType() throws Exception {
        // WorkingTree workingTree = getRepository().getWorkingTree();
        // RevFeatureType revFeatureType = new GeoToolsRevFeatureType(pointsType);
        // NodeRef treeRef = workingTree.init(revFeatureType);
        // assertNotNull(treeRef);
        //
        // RevObject metadataObject = geogit.command(RevObjectParse.class)
        // .setObjectId(treeRef.getMetadataId()).call();
    }
}
