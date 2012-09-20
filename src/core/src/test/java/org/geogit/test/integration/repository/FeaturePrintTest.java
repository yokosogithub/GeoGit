/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.test.integration.repository;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.custommonkey.xmlunit.XMLAssert;
import org.geogit.api.RevFeature;
import org.geogit.storage.ObjectWriter;
import org.geogit.storage.hessian.GeoToolsRevFeature;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;
import org.w3c.dom.Document;

public class FeaturePrintTest extends RepositoryTestCase {

    @Test
    public void testPrint() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectWriter<RevFeature> writ = getRepository().newFeatureWriter(
                new GeoToolsRevFeature(lines1));
        writ.write(bout);

        byte[] bytes = bout.toByteArray();

        getRepository().newBlobPrinter().print(bytes, System.out);

        bout = new ByteArrayOutputStream();
        getRepository().newBlobPrinter().print(bytes, new PrintStream(bout));

        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(bout.toByteArray()));
        assertNotNull(dom);
        XMLAssert.assertXpathExists("/feature/string", dom);
        XMLAssert.assertXpathEvaluatesTo(lines1.getProperty("sp").getValue().toString(),
                "/feature/string", dom);
        XMLAssert.assertXpathExists("/feature/int", dom);
        XMLAssert.assertXpathEvaluatesTo(lines1.getProperty("ip").getValue().toString(),
                "/feature/int", dom);
        XMLAssert.assertXpathExists("/feature/wkb", dom);
        XMLAssert.assertXpathEvaluatesTo("LINESTRING (1 1, 2 2)", "/feature/wkb", dom);

    }

    @Override
    protected void setUpInternal() throws Exception {
        return;
    }

}
