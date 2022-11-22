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

package io.easybest.mybatis.repository.query.criteria;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;

import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;

/**
 * .
 *
 * @author Jarvis Song
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParamValue implements Serializable {

	private String name;

	private Object value;

	private String javaType;

	private JdbcType jdbcType;

	private Class<?> typeHandler;

	private boolean getterOptional;

	public ParamValue(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	public static ParamValue of(String name, Object value) {
		return new ParamValue(name, value);
	}

	public static ParamValue of(Object value) {

		ParamValue pv = new ParamValue();
		pv.setValue(value);
		return pv;
	}

	public static ParamValue of(String name, Object value, String javaType, JdbcType jdbcType, Class<?> typeHandler) {
		return new ParamValue(name, value, javaType, jdbcType, typeHandler, false);
	}

	public static ParamValue of(String name, Object value, String javaType, JdbcType jdbcType, Class<?> typeHandler,
			boolean getterOptional) {
		return new ParamValue(name, value, javaType, jdbcType, typeHandler, getterOptional);
	}

	public static ParamValue of(String name, Object value, MybatisPersistentPropertyImpl leaf) {
		return new ParamValue(name, value, leaf.getJavaType(), leaf.getJdbcType(), leaf.getTypeHandler(),
				leaf.isGetterOptional());
	}

}
