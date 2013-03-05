package org.geogit.api.plumbing;

import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UpdateIndexTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Test
    public void testUpdateIndex() {
        exception.expect(UnsupportedOperationException.class);
        geogit.command(UpdateIndex.class).call();
    }

}
