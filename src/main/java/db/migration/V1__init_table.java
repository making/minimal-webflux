package db.migration;

import java.sql.Connection;

import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

public class V1__init_table implements JdbcMigration {
	@Override
	public void migrate(Connection connection) throws Exception {
		connection.prepareStatement("CREATE TABLE tweets (\n" + //
				"  uuid       VARCHAR(36) PRIMARY KEY,\n" + //
				"  text       VARCHAR(255),\n" + //
				"  username   VARCHAR(128),\n" + //
				"  created_at TIMESTAMP\n" + //
				");").execute();
	}
}
