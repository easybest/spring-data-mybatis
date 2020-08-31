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

/**
 * Exposes information about the database and JDBC driver that can be used in resolving
 * the appropriate Dialect to use. The information here mimics part of the JDBC
 * {@link java.sql.DatabaseMetaData} contract, specifically the portions about database
 * and driver names and versions.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public interface DialectResolutionInfo {

	/**
	 * Constant used to indicate that no version is defined.
	 */
	int NO_VERSION = -9999;

	String getDatabaseName();

	int getDatabaseMajorVersion();

	int getDatabaseMinorVersion();

	String getDriverName();

	int getDriverMajorVersion();

	int getDriverMinorVersion();

}
