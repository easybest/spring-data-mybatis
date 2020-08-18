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
package org.springframework.data.mybatis.dialect.internal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.data.mybatis.dialect.DialectException;
import org.springframework.data.mybatis.dialect.DialectResolutionInfo;

/**
 * An implementation of DialectResolutionInfo that delegates calls to a wrapped
 * {@link DatabaseMetaData}.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class DatabaseMetaDataDialectResolutionInfoAdapter implements DialectResolutionInfo {

	private final DatabaseMetaData databaseMetaData;

	public DatabaseMetaDataDialectResolutionInfoAdapter(DataSource dataSource) {
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			this.databaseMetaData = conn.getMetaData();
		}
		catch (SQLException ex) {
			throw new DialectException(ex.getMessage(), ex);
		}
		finally {
			if (null != conn) {
				try {
					conn.close();
				}
				catch (SQLException ex) {
					throw new DialectException(ex.getMessage(), ex);
				}
			}
		}
	}

	public DatabaseMetaDataDialectResolutionInfoAdapter(DatabaseMetaData databaseMetaData) {
		this.databaseMetaData = databaseMetaData;
	}

	private static int interpretVersion(int result) {
		return (result < 0) ? NO_VERSION : result;
	}

	@Override
	public String getDatabaseName() {
		try {
			return this.databaseMetaData.getDatabaseProductName();
		}
		catch (SQLException ex) {
			throw new DialectException(ex.getMessage(), ex);
		}
	}

	@Override
	public int getDatabaseMajorVersion() {
		try {
			return interpretVersion(this.databaseMetaData.getDatabaseMajorVersion());
		}
		catch (SQLException ex) {
			throw new DialectException(ex.getMessage(), ex);
		}
	}

	@Override
	public int getDatabaseMinorVersion() {
		try {
			return interpretVersion(this.databaseMetaData.getDatabaseMinorVersion());
		}
		catch (SQLException ex) {
			throw new DialectException(ex.getMessage(), ex);
		}
	}

	@Override
	public String getDriverName() {
		try {
			return this.databaseMetaData.getDriverName();
		}
		catch (SQLException ex) {
			throw new DialectException(ex.getMessage(), ex);
		}
	}

	@Override
	public int getDriverMajorVersion() {
		return interpretVersion(this.databaseMetaData.getDriverMajorVersion());
	}

	@Override
	public int getDriverMinorVersion() {
		return interpretVersion(this.databaseMetaData.getDriverMinorVersion());
	}

}
