package org.geogit.geotools.cli.test.sqlserver.functional;

import org.geogit.cli.test.functional.AbstractGeogitFunctionalTest;

public abstract class AbstractSQLServerFunctionalTest extends AbstractGeogitFunctionalTest {

    protected String getSQLServerDatabaseParameters() throws Exception {
        IniSQLServerProperties properties = new IniSQLServerProperties();
        StringBuilder sb = new StringBuilder();
        sb.append(" --host ");
        sb.append(properties.get("database.host", String.class).or("localhost"));

        sb.append(" --port ");
        sb.append(properties.get("database.port", String.class).or("1433"));

        sb.append(" --schema ");
        sb.append(properties.get("database.schema", String.class).or("dbo"));

        sb.append(" --database ");
        sb.append(properties.get("database.database", String.class).or("test"));

        sb.append(" --user ");
        sb.append(properties.get("database.user", String.class).or("sa"));

        sb.append(" --password ");
        sb.append(properties.get("database.password", String.class).or("sa"));

        return sb.toString();
    }
}
