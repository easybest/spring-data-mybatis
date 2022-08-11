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
import java.util.Locale;
import java.util.Optional;

import javax.sql.DataSource;

import io.easybest.mybatis.dialect.DMDialect;
import io.easybest.mybatis.dialect.Dialect;
import io.easybest.mybatis.dialect.H2Dialect;
import io.easybest.mybatis.dialect.HsqlDbDialect;
import io.easybest.mybatis.dialect.MariaDBDialect;
import io.easybest.mybatis.dialect.MysqlDialect;
import io.easybest.mybatis.dialect.Oracle12cDialect;
import io.easybest.mybatis.dialect.Oracle8iDialect;
import io.easybest.mybatis.dialect.Oracle9iDialect;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.data.util.Optionals;
import org.springframework.lang.Nullable;

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
			String databaseName = metaData.getDatabaseProductName().toLowerCase(Locale.ENGLISH);
			String driverName = null;
			try {
				driverName = metaData.getDriverName();
			}
			catch (SQLException ex) {
				// ignore
			}

			int majorVersion = metaData.getDatabaseMajorVersion();

			if (databaseName.contains("hsql")) {
				return HsqlDbDialect.INSTANCE;
			}

			if ("h2".equals(databaseName)) {
				return H2Dialect.INSTANCE;
			}

			if ("mysql".equals(databaseName)) {
				return MysqlDialect.INSTANCE;
			}

			if (null != driverName && driverName.startsWith("MariaDB")) {
				return MariaDBDialect.INSTANCE;
			}

			if ("oracle".equals(databaseName)) {
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

			if ("dm dbms".equals(databaseName)) {
				return new DMDialect();
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
