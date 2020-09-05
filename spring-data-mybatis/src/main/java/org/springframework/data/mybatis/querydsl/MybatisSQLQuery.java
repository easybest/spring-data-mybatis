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

import java.sql.Connection;

import javax.inject.Provider;

import com.querydsl.core.DefaultQueryMetadata;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.sql.AbstractSQLQuery;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLSerializer;
import com.querydsl.sql.SQLTemplates;

/**
 * .
 *
 * @param <T> type
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class MybatisSQLQuery<T> extends AbstractSQLQuery<T, MybatisSQLQuery<T>> {

	static final Configuration DEFAULT = new Configuration(SQLTemplates.DEFAULT);

	public MybatisSQLQuery() {
		super((Connection) null, DEFAULT, new DefaultQueryMetadata());
	}

	public MybatisSQLQuery(SQLTemplates templates) {
		super((Connection) null, new Configuration(templates), new DefaultQueryMetadata());
	}

	public MybatisSQLQuery(Connection conn, SQLTemplates templates) {
		super(conn, new Configuration(templates), new DefaultQueryMetadata());
	}

	public MybatisSQLQuery(Connection conn, SQLTemplates templates, QueryMetadata metadata) {
		super(conn, new Configuration(templates), metadata);
	}

	public MybatisSQLQuery(Configuration configuration) {
		this((Connection) null, configuration);
	}

	public MybatisSQLQuery(Connection conn, Configuration configuration) {
		super(conn, configuration, new DefaultQueryMetadata());
	}

	public MybatisSQLQuery(Connection conn, Configuration configuration, QueryMetadata metadata) {
		super(conn, configuration, metadata);
	}

	public MybatisSQLQuery(Provider<Connection> connProvider, Configuration configuration) {
		super(connProvider, configuration, new DefaultQueryMetadata());
	}

	public MybatisSQLQuery(Provider<Connection> connProvider, Configuration configuration, QueryMetadata metadata) {
		super(connProvider, configuration, metadata);
	}

	@Override
	protected SQLSerializer createSerializer() {
		MybatisSQLSerializer serializer = new MybatisSQLSerializer(this.configuration);
		serializer.setUseLiterals(this.useLiterals);
		return serializer;
	}

	@Override
	public MybatisSQLQuery<T> clone(Connection conn) {
		MybatisSQLQuery<T> q = new MybatisSQLQuery<>(conn, this.getConfiguration(), this.getMetadata().clone());
		q.clone(this);
		return q;
	}

	@Override
	public <U> MybatisSQLQuery<U> select(Expression<U> expr) {
		this.queryMixin.setProjection(expr);
		MybatisSQLQuery<U> newType = (MybatisSQLQuery<U>) this;
		return newType;
	}

	@Override
	public MybatisSQLQuery<Tuple> select(Expression<?>... exprs) {
		this.queryMixin.setProjection(exprs);
		MybatisSQLQuery<Tuple> newType = (MybatisSQLQuery<Tuple>) this;
		return newType;
	}

}
