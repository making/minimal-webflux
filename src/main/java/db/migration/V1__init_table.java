package db.migration;

import java.sql.Connection;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V1__init_table extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		connection.prepareStatement("CREATE TABLE tweets (\n" + //
				"  uuid       VARCHAR(36) PRIMARY KEY,\n" + //
				"  text       VARCHAR(255),\n" + //
				"  username   VARCHAR(128),\n" + //
				"  created_at BIGINT\n" + //
				");").execute();
	}
}
