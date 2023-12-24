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

package io.easybest.mybatis.repository.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author Jarvis Song
 * @param <S> domain type
 * @param <R> result type
 */
class FetchableFluentQueryByExample<S, R> extends FluentQuerySupport<S, R> implements FetchableFluentQuery<R> {

	private final QueryByExampleExecutor<S> repository;

	private final Example<S> example;

	public FetchableFluentQueryByExample(QueryByExampleExecutor<S> repository, Example<S> example, Class<S> entityType,
			Class<R> resultType, Sort sort, Collection<String> properties) {

		super(entityType, resultType, sort, properties);

		this.repository = repository;
		this.example = example;
	}

	@Override
	public FetchableFluentQuery<R> sortBy(Sort sort) {

		Assert.notNull(sort, "Sort must not be null!");

		return new FetchableFluentQueryByExample<>(this.repository, this.example, this.entityType, this.resultType,
				this.sort.and(sort), this.properties);
	}

	@Override
	public <R1> FetchableFluentQuery<R1> as(Class<R1> resultType) {

		Assert.notNull(resultType, "Projection target type must not be null!");
		if (!resultType.isInterface()) {
			throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
		}

		return new FetchableFluentQueryByExample<>(this.repository, this.example, this.entityType, resultType,
				this.sort, this.properties);
	}

	@Override
	public FetchableFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryByExample<>(this.repository, this.example, this.entityType, this.resultType,
				this.sort, this.mergeProperties(properties));
	}

	@Override
	public R oneValue() {

		S entity = this.repository.findOne(this.example).orElse(null);
		return null == entity ? null : this.getConversionFunction().apply(entity);
	}

	@Override
	public R firstValue() {

		Page<S> page = this.repository.findAll(this.example, PageRequest.ofSize(1).withSort(this.sort));

		List<S> results = page.getContent();

		return results.isEmpty() ? null : this.getConversionFunction().apply(results.get(0));
	}

	@Override
	public List<R> all() {

		List<S> resultList = (List<S>) this.repository.findAll(this.example, this.sort);

		return this.convert(resultList);
	}

	@Override
	public Page<R> page(Pageable pageable) {

		if (this.sort.isSorted()) {
			pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
					pageable.getSort().and(this.sort));
		}

		Page<S> page = this.repository.findAll(this.example, pageable);

		List<S> content = page.getContent();
		List<R> results = this.convert(content);
		return new PageImpl<>(results, pageable, page.getTotalElements());
	}

	@Override
	public Stream<R> stream() {

		return this.all().stream();
	}

	@Override
	public long count() {

		return this.repository.count(this.example);
	}

	@Override
	public boolean exists() {

		return this.repository.exists(this.example);
	}

	private List<R> convert(List<S> resultList) {

		Function<Object, R> conversionFunction = this.getConversionFunction();
		List<R> mapped = new ArrayList<>(resultList.size());

		for (S s : resultList) {
			mapped.add(conversionFunction.apply(s));
		}
		return mapped;
	}

	private Function<Object, R> getConversionFunction() {
		return this.getConversionFunction(this.example.getProbeType(), this.resultType);
	}

}
