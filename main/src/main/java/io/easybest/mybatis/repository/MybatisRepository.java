/*
 * Copyright 2019-2023 the original author or authors.
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

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.QueryByExampleExecutor;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> entity type
 * @param <ID> primary key
 */
@NoRepositoryBean
public interface MybatisRepository<T, ID> extends ListCrudRepository<T, ID>, ListPagingAndSortingRepository<T, ID>,
		QueryByExampleExecutor<T>, QueryByCriteriaExecutor<T> {

	T getById(ID id);

	@Override
	List<T> findAll(Sort sort);

	@Override
	List<T> findAll();

	@Override
	List<T> findAllById(Iterable<ID> ids);

	@Override
	<S extends T> List<S> findAll(Example<S> example);

	@Override
	<S extends T> List<S> findAll(Example<S> example, Sort sort);

	@Override
	<S extends T> List<S> saveAll(Iterable<S> entities);

	<S extends T> List<S> saveSelectiveAll(Iterable<S> entities);

	<S extends T> List<S> saveCascadeAll(Iterable<S> entities);

	<S extends T> S saveCascade(S entity);

	<S extends T> S saveSelective(S entity);

	<S extends T> S insert(S entity);

	/**
	 * Experimental cascading.
	 * @param entity entity
	 * @return persist entity
	 * @param <S> type
	 */
	<S extends T> S insertCascade(S entity);

	<S extends T> S insertSelective(S entity);

	<S extends T> S update(S entity);

	<S extends T> S updateCascade(S entity);

	<S extends T> S updateSelective(S entity);

	<S extends T> S update(ID id, S entity);

	<S extends T> S updateSelective(ID id, S entity);

	void deleteAllInBatch(Iterable<T> entities);

	void deleteAllByIdInBatch(Iterable<ID> ids);

	void deleteAllInBatch();

}
