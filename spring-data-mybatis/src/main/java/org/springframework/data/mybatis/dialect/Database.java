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
			return MySQL57Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ("MySQL".equals(databaseName)) {
				final int majorVersion = info.getDatabaseMajorVersion();
				final int minorVersion = info.getDatabaseMinorVersion();

				if (majorVersion < 5) {
					return new MySQLDialect();
				}
				else if (majorVersion == 5) {
					if (minorVersion < 5) {
						return new MySQL5Dialect();
					}
					else if (minorVersion < 7) {
						return new MySQL55Dialect();
					}
					else {
						return new MySQL57Dialect();
					}
				}

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
