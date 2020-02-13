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
package org.springframework.data.mybatis.mapping.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * Column model.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Getter
@Setter
public class Column {

	private Identifier name;

	private JdbcType jdbcType;

	private Class<?> javaType;

	private Class<? extends TypeHandler<?>> typeHandler;

	private boolean primaryKey;

	public Column(String name) {
		this.name = Identifier.toIdentifier(name);
	}

	public Column(String name, JdbcType jdbcType) {
		this.name = Identifier.toIdentifier(name);
		this.jdbcType = jdbcType;
	}

	public boolean isString() {
		return this.javaType == String.class;
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
		if (this.javaType == byte[].class) {
			return "_byte[]";
		}
		return this.javaType.getName();
	}

	public String getTypeHandlerString() {
		if (null == this.typeHandler) {
			return null;
		}
		return this.typeHandler.getName();
	}

}
