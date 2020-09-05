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

import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.RelationalPathBase;

/**
 * .
 *
 * @param <T> type
 * @param <P> domain typ
 * @author JARVIS SONG
 * @since 2.0.2
 */
public abstract class MybatisRelationalPathBase<T, P> extends RelationalPathBase<T> {

	public MybatisRelationalPathBase(Class<? extends T> type, String variable, String schema, String table) {
		super(type, variable, schema, table);
	}

	public MybatisRelationalPathBase(Class<? extends T> type, PathMetadata metadata, String schema, String table) {
		super(type, metadata, schema, table);
	}

	public FactoryExpression<P> projections() {
		return Projections.bean(this.domainClass(), this.all());
	}

	protected abstract Class<P> domainClass();

}
