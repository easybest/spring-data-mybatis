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
package org.springframework.data.mybatis.querydsl;

import java.io.Serializable;

import org.apache.ibatis.type.JdbcType;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Property implements Serializable {

	private static final long serialVersionUID = -6184974239170584300L;

	private String name;

	private String columnName;

	private Class<?> javaType;

	private JdbcType jdbcType;

	private String pathType;

	private final Domain domain;

	public Property(Domain domain) {
		this.domain = domain;
	}

	public boolean isNeedPattern() {
		return !"StringPath".equals(this.pathType) && !"BooleanPath".equals(this.pathType);
	}

	public String getPathType() {
		return this.pathType;
	}

	public String getPathFactory() {
		if (null == this.pathType) {
			return null;
		}

		return this.pathType.replace("Path", "");
	}

	public String getJdbcTypeString() {
		if (null == this.jdbcType) {
			return null;
		}
		return this.jdbcType.name();
	}

	public String getJavaTypeString() {
		if (null == this.javaType) {
			return null;
		}

		if (this.javaType.getName().startsWith("java.lang.")) {
			return this.javaType.getSimpleName();
		}
		return this.javaType.getName();
	}

	public void setPathType(String pathType) {
		this.pathType = pathType;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColumnName() {
		return this.columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public Class<?> getJavaType() {
		return this.javaType;
	}

	public void setJavaType(Class<?> javaType) {
		this.javaType = javaType;
	}

	public JdbcType getJdbcType() {
		return this.jdbcType;
	}

	public void setJdbcType(JdbcType jdbcType) {
		this.jdbcType = jdbcType;
	}

	public Domain getDomain() {
		return this.domain;
	}

}
