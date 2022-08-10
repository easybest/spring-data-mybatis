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

package io.easybest.mybatis.mapping;

import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import org.springframework.data.mapping.PersistentProperty;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface MybatisPersistentProperty extends PersistentProperty<MybatisPersistentPropertyImpl> {

	boolean isEmbeddable();

	boolean isDatabaseDefaultValue();

	SqlIdentifier getColumnName();

	JdbcType getJdbcType();

	String getJavaType();

	Class<? extends TypeHandler<?>> getTypeHandler();

	@Override
	MybatisPersistentEntity<?> getOwner();

	default SqlIdentifier getRequiredColumnName() {

		SqlIdentifier columnName = this.getColumnName();
		if (null == columnName) {
			throw new IllegalArgumentException(
					String.format("No column name available for persistent property %s", this));
		}
		return columnName;
	}

}
