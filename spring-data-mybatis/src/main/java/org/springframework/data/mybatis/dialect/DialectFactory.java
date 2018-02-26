package org.springframework.data.mybatis.dialect;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.Configuration;
import org.springframework.util.ConcurrentReferenceHashMap;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Jarvis Song
 */
@Slf4j
public class DialectFactory {

	public static final int NO_VERSION = -9999;

	private static Map<String, Dialect> dialects = new ConcurrentReferenceHashMap<>();

	public static Dialect getDialect(Configuration configuration) {
		String id = configuration.getEnvironment().getId();
		Dialect dialect = dialects.get(id);
		if (null != dialect) {
			return dialect;
		}

		DataSource dataSource = configuration.getEnvironment().getDataSource();
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			DatabaseMetaData metaData = conn.getMetaData();
			dialect = getDialect(metaData);
			dialects.put(id, dialect);
			log.info("Using dialect: " + dialect + " for environment: " + id);
			return dialect;
		} catch (SQLException e) {
			throw new DialectException("Could not detect dialect for environment:" + id, e);

		} finally {
			if (null != conn) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.error(e.getMessage(), e);
				}
			}
		}

	}

	private static Dialect getDialect(DatabaseMetaData metaData) throws SQLException {
		final String databaseName = metaData.getDatabaseProductName();
		final int majorVersion = interpretVersion(metaData.getDatabaseMajorVersion());
		final int minorVersion = interpretVersion(metaData.getDatabaseMinorVersion());

		if ("H2".equalsIgnoreCase(databaseName)) {
			return new H2Dialect();
		}
		if ("MySQL".equalsIgnoreCase(databaseName)) {
			return new MySQLDialect();
		}
		// if (databaseName.startsWith("Microsoft SQL Server")) {
		// return new SqlServer2012Dialect();
		// }
		// if ("Oracle".equalsIgnoreCase(databaseName)) {
		// return new OracleDialect();
		// }
		// if ("PostgreSQL".equalsIgnoreCase(databaseName)) {
		// return new PostgreSQLDialect();
		// }
		// return new AnsiSqlDialect();
		return null;
	}

	private static int interpretVersion(int result) {
		return result < 0 ? NO_VERSION : result;
	}

}
