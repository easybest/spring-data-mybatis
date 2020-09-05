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
package org.springframework.data.mybatis.repository.support;

import java.util.List;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.OrderSpecifier.NullHandling;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.sql.AbstractSQLQuery;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLServer2005Templates;
import com.querydsl.sql.SQLServer2008Templates;
import com.querydsl.sql.SQLServer2012Templates;
import com.querydsl.sql.SQLTemplates;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.H2Dialect;
import org.springframework.data.mybatis.dialect.HSQLDialect;
import org.springframework.data.mybatis.dialect.PostgreSQLDialect;
import org.springframework.data.mybatis.dialect.SQLServer2005Dialect;
import org.springframework.data.mybatis.dialect.SQLServer2012Dialect;
import org.springframework.data.mybatis.dialect.SQLServerDialect;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.handler.DateUnixTimestampTypeHandler;
import org.springframework.data.mybatis.mapping.handler.UnixTimestampDateTypeHandler;
import org.springframework.data.mybatis.mapping.model.Column;
import org.springframework.data.mybatis.querydsl.MybatisSQLQuery;
import org.springframework.data.mybatis.querydsl.type.DateAsLongType;
import org.springframework.data.mybatis.querydsl.type.LongAsDateType;
import org.springframework.data.querydsl.QSort;
import org.springframework.util.Assert;

/**
 * .
 *
 * @param <Q> query type
 * @param <T> domain type
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Querydsl<Q, T> {

	private final SqlSessionTemplate sqlSessionTemplate;

	private final PathBuilder<Q> builder;

	private final Configuration configuration;

	public Querydsl(SqlSessionTemplate sqlSessionTemplate, MybatisMappingContext mappingContext, Dialect dialect,
			MybatisEntityInformation<T, ?> entityInformation, PathBuilder<Q> builder) {

		this.sqlSessionTemplate = sqlSessionTemplate;
		this.builder = builder;
		SQLTemplates sqlTemplates = null;
		if (dialect.getClass().getName().startsWith("MySQL")) {
			sqlTemplates = MySQLTemplates.builder().build();
		}
		else if (dialect.getClass().getName().startsWith("Oracle")) {
			sqlTemplates = OracleTemplates.builder().build();
		}
		else if (dialect.getClass() == H2Dialect.class) {
			sqlTemplates = H2Templates.builder().build();
		}
		else if (dialect.getClass() == HSQLDialect.class) {
			sqlTemplates = HSQLDBTemplates.builder().build();
		}
		else if (dialect.getClass() == PostgreSQLDialect.class) {
			sqlTemplates = PostgreSQLTemplates.builder().build();
		}
		else if (dialect.getClass() == SQLServer2005Dialect.class) {
			sqlTemplates = SQLServer2005Templates.builder().build();
		}
		else if (dialect.getClass() == SQLServerDialect.class) {
			sqlTemplates = SQLServer2008Templates.builder().build();
		}
		else if (dialect.getClass() == SQLServer2012Dialect.class) {
			sqlTemplates = SQLServer2012Templates.builder().build();
		}
		this.configuration = new Configuration(sqlTemplates);

		MybatisPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(entityInformation.getJavaType());
		entity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) property -> {
			Column column = property.getColumn();
			if (null == column.getTypeHandler()) {
				return;
			}
			if (column.getTypeHandler() == DateUnixTimestampTypeHandler.class) {
				this.configuration.register(entity.getTable().getName().getText(), column.getName().getText(),
						new DateAsLongType());
			}
			if (column.getTypeHandler() == UnixTimestampDateTypeHandler.class) {
				this.configuration.register(entity.getTable().getName().getText(), column.getName().getText(),
						new LongAsDateType());
			}
		});
	}

	public AbstractSQLQuery<T, MybatisSQLQuery<T>> createQuery() {
		return new MybatisSQLQuery<>(this.sqlSessionTemplate.getConnection(), this.configuration);
	}

	public AbstractSQLQuery<T, MybatisSQLQuery<T>> createQuery(EntityPath<?>... paths) {

		Assert.notNull(paths, "Paths must not be null!");
		return this.createQuery().from(paths);
	}

	public MybatisSQLQuery<T> applySorting(Sort sort, MybatisSQLQuery<T> query) {

		Assert.notNull(sort, "Sort must not be null!");
		Assert.notNull(query, "Query must not be null!");

		if (sort.isUnsorted()) {
			return query;
		}

		if (sort instanceof QSort) {
			return this.addOrderByFrom((QSort) sort, query);
		}

		return this.addOrderByFrom(sort, query);
	}

	private <T> MybatisSQLQuery<T> addOrderByFrom(QSort qsort, MybatisSQLQuery<T> query) {

		List<OrderSpecifier<?>> orderSpecifiers = qsort.getOrderSpecifiers();

		return query.orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]));
	}

	private <T> MybatisSQLQuery<T> addOrderByFrom(Sort sort, MybatisSQLQuery<T> query) {

		Assert.notNull(sort, "Sort must not be null!");
		Assert.notNull(query, "Query must not be null!");

		for (Order order : sort) {
			query.orderBy(this.toOrderSpecifier(order));
		}

		return query;
	}

	public MybatisSQLQuery<T> applyPagination(Pageable pageable, MybatisSQLQuery<T> query) {

		Assert.notNull(pageable, "Pageable must not be null!");
		Assert.notNull(query, "SQLQuery must not be null!");

		if (pageable.isUnpaged()) {
			return query;
		}

		query.offset(pageable.getOffset());
		query.limit(pageable.getPageSize());

		return this.applySorting(pageable.getSort(), query);
	}

	private OrderSpecifier<?> toOrderSpecifier(Order order) {

		return new OrderSpecifier(
				order.isAscending() ? com.querydsl.core.types.Order.ASC : com.querydsl.core.types.Order.DESC,
				this.buildOrderPropertyPathFrom(order), this.toQueryDslNullHandling(order.getNullHandling()));
	}

	private NullHandling toQueryDslNullHandling(org.springframework.data.domain.Sort.NullHandling nullHandling) {

		Assert.notNull(nullHandling, "NullHandling must not be null!");

		switch (nullHandling) {

		case NULLS_FIRST:
			return NullHandling.NullsFirst;

		case NULLS_LAST:
			return NullHandling.NullsLast;

		case NATIVE:
		default:
			return NullHandling.Default;
		}
	}

	private Expression<?> buildOrderPropertyPathFrom(Order order) {

		Assert.notNull(order, "Order must not be null!");

		PropertyPath path = PropertyPath.from(order.getProperty(), this.builder.getType());
		Expression<?> sortPropertyExpression = this.builder;

		while (path != null) {

			sortPropertyExpression = (!path.hasNext() && order.isIgnoreCase())
					? Expressions.stringPath((Path<?>) sortPropertyExpression, path.getSegment()).lower()
					: Expressions.path(path.getType(), (Path<?>) sortPropertyExpression, path.getSegment());

			path = path.next();
		}

		return sortPropertyExpression;
	}

}
