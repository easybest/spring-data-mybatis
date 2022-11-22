/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.easybest.mybatis.repository.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.util.Optionals;
import org.springframework.lang.Nullable;

import io.easybest.mybatis.dialect.CUBRIDDialect;
import io.easybest.mybatis.dialect.ClickHouseDialect;
import io.easybest.mybatis.dialect.DB2400Dialect;
import io.easybest.mybatis.dialect.DB2Dialect;
import io.easybest.mybatis.dialect.DMDialect;
import io.easybest.mybatis.dialect.DerbyDialect;
import io.easybest.mybatis.dialect.Dialect;
import io.easybest.mybatis.dialect.EnterpriseDBDialect;
import io.easybest.mybatis.dialect.FirebirdDialect;
import io.easybest.mybatis.dialect.H2Dialect;
import io.easybest.mybatis.dialect.HANADialect;
import io.easybest.mybatis.dialect.HerdDBDialect;
import io.easybest.mybatis.dialect.HighGoDialect;
import io.easybest.mybatis.dialect.HsqlDbDialect;
import io.easybest.mybatis.dialect.ImpalaDialect;
import io.easybest.mybatis.dialect.InformixDialect;
import io.easybest.mybatis.dialect.Ingres10Dialect;
import io.easybest.mybatis.dialect.Ingres9Dialect;
import io.easybest.mybatis.dialect.IngresDialect;
import io.easybest.mybatis.dialect.KingbaseDialect;
import io.easybest.mybatis.dialect.MariaDBDialect;
import io.easybest.mybatis.dialect.MySQLDialect;
import io.easybest.mybatis.dialect.Oracle12cDialect;
import io.easybest.mybatis.dialect.Oracle8iDialect;
import io.easybest.mybatis.dialect.Oracle9iDialect;
import io.easybest.mybatis.dialect.OscarDialect;
import io.easybest.mybatis.dialect.PhoenixDialect;
import io.easybest.mybatis.dialect.PolarDBDialect;
import io.easybest.mybatis.dialect.PostgreSQLDialect;
import io.easybest.mybatis.dialect.SQLServer2005Dialect;
import io.easybest.mybatis.dialect.SQLServer2012Dialect;
import io.easybest.mybatis.dialect.SQLiteDialect;
import io.easybest.mybatis.dialect.SqlServerDialect;
import io.easybest.mybatis.dialect.XuguDialect;

/**
 * .
 *
 * @author Jarvis Song
 */
@Slf4j
public final class DialectResolver {

	private static final List<MybatisDialectProvider> DETECTORS = SpringFactoriesLoader
			.loadFactories(MybatisDialectProvider.class, DialectResolver.class.getClassLoader());

	private DialectResolver() {
	}

	public static Dialect getDialect(SqlSessionTemplate template) {

		return DETECTORS.stream().map(it -> it.getDialect(template)).flatMap(Optionals::toStream).findFirst()
				.orElseThrow(() -> new NoDialectException(
						String.format("Cannot determine a dialect for %s. Please provide a effective Dialect.",
								template.getConfiguration().getEnvironment().getId())));

	}

	public interface MybatisDialectProvider {

		Optional<Dialect> getDialect(SqlSessionTemplate template);

	}

	public static class DefaultDialectProvider implements MybatisDialectProvider {

		@Override
		public Optional<Dialect> getDialect(SqlSessionTemplate template) {

			DataSource dataSource = template.getConfiguration().getEnvironment().getDataSource();
			try (Connection conn = dataSource.getConnection()) {
				return Optional.ofNullable(getDialect(conn));
			}
			catch (SQLException ex) {
				log.error(ex.getMessage(), ex);
				return Optional.empty();
			}

		}

		@Nullable
		private static Dialect getDialect(Connection connection) throws SQLException {

			DatabaseMetaData metaData = connection.getMetaData();
			String databaseName = metaData.getDatabaseProductName();
			String driverName = "";
			try {
				driverName = metaData.getDriverName();
			}
			catch (SQLException ex) {
				// ignore
			}

			int majorVersion = metaData.getDatabaseMajorVersion();
			int minorVersion = metaData.getDatabaseMinorVersion();

			// HSQL
			if ("HSQL Database Engine".equals(databaseName)) {
				return new HsqlDbDialect();
			}

			// H2
			if ("H2".equals(databaseName)) {
				return new H2Dialect();
			}

			// Phoenix
			if ("Phoenix".equals(databaseName)) {
				return new PhoenixDialect();
			}

			// PostgreSQL
			if ("PostgreSQL".equals(databaseName)) {
				return new PostgreSQLDialect();
			}

			// MySQL
			if ("MySQL".equals(databaseName)) {
				return new MySQLDialect();
			}

			// MariaDB
			if (null != driverName && driverName.startsWith("MariaDB")) {
				return new MariaDBDialect();
			}
			// SQLite
			if ("SQLite".equals(databaseName)) {
				return new SQLiteDialect();
			}

			// HerdDB
			if ("HerdDB".equals(databaseName)) {
				return new HerdDBDialect();
			}

			// Oracle
			if ("Oracle".equals(databaseName)) {
				switch (majorVersion) {
				case 8:
					return new Oracle8iDialect();
				case 9:
				case 10:
				case 11:
					return new Oracle9iDialect();
				case 12:
				default:
					return new Oracle12cDialect();
				}
			}

			// DB2
			if ("DB2 UDB for AS/400".equals(databaseName)) {
				return new DB2400Dialect();
			}

			if (databaseName.startsWith("DB2/")) {
				return new DB2Dialect();
			}

			// Informix
			if (databaseName.startsWith("Informix")) {
				return new InformixDialect();
			}

			// SQL Server
			if (databaseName.startsWith("Microsoft SQL Server")) {
				switch (majorVersion) {
				case 8:
					return new SqlServerDialect();
				case 9:
				case 10:
					return new SQLServer2005Dialect();
				default:
					if (majorVersion < 8) {
						return new SqlServerDialect();
					}
					else {
						return new SQLServer2012Dialect();
					}
				}
			}

			// Derby
			if ("Apache Derby".equals(databaseName)) {
				return new DerbyDialect();
			}

			// DM
			if ("DM DBMS".equals(databaseName)) {
				return new DMDialect();
			}

			// PolarDB
			if ("POLARDB JDBC Driver".equals(driverName)) {
				return new PolarDBDialect();
			}

			// OSCAR
			if ("OSCAR".equals(databaseName)) {
				return new OscarDialect();
			}

			// Clickhouse
			if ("ClickHouse".equals(databaseName)) {
				return new ClickHouseDialect();
			}

			// HighGO
			if ("Highgo".equals(databaseName)) {
				return new HighGoDialect();
			}

			// XUGU
			if ("XuGu".equals(databaseName)) {
				return new XuguDialect();
			}

			// Impala
			if ("Impala".equals(databaseName)) {
				return new ImpalaDialect();
			}

			// Kingbase
			if (driverName.startsWith("Kingbase")) {
				return new KingbaseDialect();
			}

			// Firebird
			if (databaseName.startsWith("Firebird")) {
				return new FirebirdDialect();
			}

			// EnterpriseDB
			if ("EnterpriseDB".equals(databaseName)) {
				return new EnterpriseDBDialect();
			}

			// CUBRID
			if ("CUBRID".equalsIgnoreCase(databaseName)) {
				return new CUBRIDDialect();
			}

			// HANA
			if ("HDB".equals(databaseName)) {
				return new HANADialect();
			}

			// Ingres
			if ("Ingres".equalsIgnoreCase(databaseName)) {

				if (majorVersion < 9) {
					return new IngresDialect();
				}
				else if (majorVersion == 9) {
					if (minorVersion > 2) {
						return new Ingres9Dialect();
					}
					return new IngresDialect();
				}
				else if (majorVersion == 10) {
					return new Ingres10Dialect();
				}

				return new Ingres10Dialect();
			}

			log.info(String.format("Couldn't determine Dialect for \"%s\"", databaseName));
			return null;
		}

	}

	/**
	 * Exception thrown when {@link DialectResolver} cannot resolve a {@link Dialect}.
	 */
	public static class NoDialectException extends NonTransientDataAccessException {

		/**
		 * Constructor for NoDialectFoundException.
		 * @param msg the detail message
		 */
		NoDialectException(String msg) {
			super(msg);
		}

	}

}
