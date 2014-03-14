/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.plumbing;

import org.geogit.api.Node;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.storage.FieldType;
import org.geogit.storage.ObjectDatabase;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.google.common.base.Suppliers;

public class CatObjectTest extends RepositoryTestCase {

    private ObjectDatabase odb;

    private static final ObjectId FAKE_ID = ObjectId.forString("fake");

    private static final String FEATURE_PREFIX = "Feature.";

    @Override
    protected void setUpInternal() throws Exception {
        odb = repo.getObjectDatabase();
    }

    @Test
    public void TestCatTreeWithoutBucketsObject() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT / 2;
        RevTree tree = createTree(numChildren);
        CharSequence desc = geogit.command(CatObject.class).setObject(Suppliers.ofInstance(tree))
                .call();
        String[] lines = desc.toString().split("\n");
        assertEquals(numChildren + 4, lines.length);
        for (int i = 4; i < lines.length; i++) {
            String[] tokens = lines[i].split("\t");
            assertEquals(FAKE_ID.toString(), tokens[3].trim());
        }

    }

    @Test
    public void TestCatTreeWithBucketsObject() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT * 2;
        RevTree tree = createTree(numChildren);
        CharSequence desc = geogit.command(CatObject.class).setObject(Suppliers.ofInstance(tree))
                .call();
        String[] lines = desc.toString().split("\n");
        assertEquals(tree.buckets().get().size() + 4, lines.length);
        for (int i = 4; i < lines.length; i++) {
            String[] tokens = lines[i].split("\t");
            assertEquals(tokens[0].trim(), "BUCKET");
        }
    }

    private RevTree createTree(int numChildren) {
        RevTreeBuilder rtb = new RevTreeBuilder(odb);
        for (int i = 0; i < numChildren; i++) {
            String key = FEATURE_PREFIX + i;
            Node ref = Node.create(key, FAKE_ID, FAKE_ID, TYPE.FEATURE, null);
            rtb.put(ref);
        }
        return rtb.build();
    }

    @Test
    public void TestCatFeatureObject() {
        RevFeatureBuilder rfb = new RevFeatureBuilder();
        RevFeature feature = rfb.build(points1);
        CharSequence desc = geogit.command(CatObject.class)
                .setObject(Suppliers.ofInstance(feature)).call();
        String[] lines = desc.toString().split("\n");

        assertEquals(points1.getProperties().size() + 2, lines.length);
        assertEquals(FieldType.STRING.name() + "\tStringProp1_1", lines[2]);
        assertEquals(FieldType.INTEGER.name() + "\t1000", lines[3]);
        assertEquals(FieldType.POINT.name() + "\tPOINT (1 1)", lines[4]);
    }

}
