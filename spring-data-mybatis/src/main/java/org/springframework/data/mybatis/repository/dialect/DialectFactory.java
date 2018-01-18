package org.springframework.data.mybatis.repository.dialect;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class DialectFactory {

	private static Map<String, Dialect> instances = new HashMap<>();
	private static Dialect dialect = new Dialect();

	

	public static Dialect getDialect(String databaseId) {
		if (StringUtils.isEmpty(databaseId)) {
			return dialect;
		}

		Dialect dialect = instances.get(databaseId);
		if (null != dialect) {
			return dialect;
		}

		if ("h2".equalsIgnoreCase(databaseId)) {
			dialect = new H2Dialect();
		} else if ("mysql".equalsIgnoreCase(databaseId)) {
			dialect = new MySQLDialect();
		} else if ("oracle".equalsIgnoreCase(databaseId)) {
			dialect = new OracleDialect();
		} else if ("PostgreSQL".equalsIgnoreCase(databaseId)) {
			dialect = new PostgreSQLDialect();
		} else if ("SQLServer".equalsIgnoreCase(databaseId)) {
			dialect = new SQLServerDialect();
		}

		if (null != dialect) {
			instances.put(databaseId, dialect);
		}

		return DialectFactory.dialect;
	}

}
