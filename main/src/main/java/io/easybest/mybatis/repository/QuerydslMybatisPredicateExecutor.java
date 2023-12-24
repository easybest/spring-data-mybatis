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

package io.easybest.mybatis.repository;

import java.util.Optional;
import java.util.function.Function;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import io.easybest.mybatis.mapping.EntityManager;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain
 */
public class QuerydslMybatisPredicateExecutor<T> implements QuerydslPredicateExecutor<T> {

	private final EntityInformation<T, ?> entityInformation;

	private final EntityPath<T> path;

	private final EntityManager entityManager;

	public QuerydslMybatisPredicateExecutor(EntityInformation<T, ?> entityInformation, EntityManager entityManager,
			EntityPathResolver resolver) {

		this.entityInformation = entityInformation;
		this.path = resolver.createPath(entityInformation.getJavaType());
		this.entityManager = entityManager;
	}

	@Override
	public Optional<T> findOne(Predicate predicate) {
		return Optional.empty();
	}

	@Override
	public Iterable<T> findAll(Predicate predicate) {
		return null;
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, Sort sort) {
		return null;
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
		return null;
	}

	@Override
	public Iterable<T> findAll(OrderSpecifier<?>... orders) {
		return null;
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {
		return null;
	}

	@Override
	public long count(Predicate predicate) {
		return 0;
	}

	@Override
	public boolean exists(Predicate predicate) {
		return false;
	}

	@Override
	public <S extends T, R> R findBy(Predicate predicate, Function<FetchableFluentQuery<S>, R> queryFunction) {
		return null;
	}

}
