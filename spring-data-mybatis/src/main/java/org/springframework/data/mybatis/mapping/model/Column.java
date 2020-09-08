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

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;

/**
 * Column model.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Getter
@Setter
@ToString
public class Column {

	private static Map<Class<?>, String> TYPE_ALIAS = new HashMap<>();
	static {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		Map<String, Class<?>> typeAliases = typeAliasRegistry.getTypeAliases();
		typeAliases.entrySet().stream().forEach(entry -> TYPE_ALIAS.put(entry.getValue(), entry.getKey()));
	}

	private Identifier name;

	private JdbcType jdbcType;

	private Class<?> javaType;

	private Class<?> typeHandler;

	private boolean primaryKey;

	private boolean version;

	public Column(String name) {
		this.name = Identifier.toIdentifier(name);
	}

	public Column(String name, JdbcType jdbcType) {
		this.name = Identifier.toIdentifier(name);
		this.jdbcType = jdbcType;
	}

	public Column(Identifier name, JdbcType jdbcType) {
		this.name = name;
		this.jdbcType = jdbcType;
	}

	public boolean isString() {
		return this.javaType == String.class
				|| (null != this.javaType && CharSequence.class.isAssignableFrom(this.javaType));
	}

	public boolean hasJdbcType() {
		return null != this.jdbcType;
	}

	public boolean hasJavaType() {
		return null != this.jdbcType;
	}

	public boolean hasTypeHandler() {
		return null != this.typeHandler;
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

		String type = TYPE_ALIAS.get(this.javaType);
		if (null != type) {
			return type;
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
