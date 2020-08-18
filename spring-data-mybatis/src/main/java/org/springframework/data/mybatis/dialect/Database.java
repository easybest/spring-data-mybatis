/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.dialect;

import java.lang.reflect.InvocationTargetException;

/**
 * List all supported relational database systems.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public enum Database {

	/**
	 * Microsoft SQL Server.
	 */
	SQLSERVER {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return SQLServer2012Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if (databaseName.startsWith("Microsoft SQL Server")) {
				final int majorVersion = info.getDatabaseMajorVersion();

				switch (majorVersion) {
				case 8:
					return new SQLServerDialect();
				case 9:
				case 10:
					return new SQLServer2005Dialect();
				case 11:
				case 12:
				case 13:
					return new SQLServer2012Dialect();
				default:
					if (majorVersion < 8) {
						return new SQLServerDialect();
					}
					else {
						return latestDialectInstance(this);
					}
				}
			}
			return null;
		}
	},
	/**
	 * PostgreSQL.
	 */
	POSTGRESQL {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return PostgreSQLDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ("PostgreSQL".equals(databaseName)) {
				return latestDialectInstance(this);
			}
			return null;
		}
	},
	/**
	 * HSQLDB.
	 */
	HSQL {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return HSQLDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ("HSQL Database Engine".equals(databaseName)) {
				return latestDialectInstance(this);
			}

			return null;
		}
	},

	/**
	 * H2.
	 */
	H2 {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return H2Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ("H2".equals(databaseName)) {
				return latestDialectInstance(this);
			}

			return null;
		}
	},
	/**
	 * MySQL.
	 */
	MYSQL {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return MySQLDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ("MySQL".equals(databaseName)) {
				return latestDialectInstance(this);
			}

			return null;
		}
	};

	public abstract Class<? extends Dialect> latestDialect();

	public abstract Dialect resolveDialect(DialectResolutionInfo info);

	private static Dialect latestDialectInstance(Database database) {
		try {
			return database.latestDialect().getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
			throw new DialectException(ex.getMessage(), ex);
		}
	}

}
