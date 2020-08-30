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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.query.EscapeCharacter;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * The simple implementation of {@link MybatisRepository}.
 *
 * @param <T> domain's type
 * @param <ID> domain primary key's type
 * @author JARVIS SONG
 * @since 1.0.0
 */
@Repository
@Transactional(readOnly = true)
public class SimpleMybatisRepository<T, ID> extends SqlSessionRepositorySupport
		implements MybatisRepositoryImplementation<T, ID> {

	private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";

	private static final String ENTITY_MUST_NOT_BE_NULL = "The entity must not be null!";

	private final MybatisEntityInformation<T, ID> entityInformation;

	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

	private AuditingHandler auditingHandler;

	private final String namespace;

	private static <T> Collection<T> toCollection(Iterable<T> ts) {
		if (ts instanceof Collection) {
			return (Collection<T>) ts;
		}

		List<T> tCollection = new ArrayList<>();
		for (T t : ts) {
			tCollection.add(t);
		}
		return tCollection;
	}

	public SimpleMybatisRepository(MybatisEntityInformation<T, ID> entityInformation,
			RepositoryInformation repositoryInformation, SqlSessionTemplate sqlSessionTemplate,
			AuditingHandler auditingHandler) {
		super(sqlSessionTemplate);
		Assert.notNull(entityInformation, "MybatisEntityInformation must not be null.");
		this.entityInformation = entityInformation;
		this.namespace = repositoryInformation.getRepositoryInterface().getName();
		this.auditingHandler = auditingHandler;
	}

	@Override
	protected String getNamespace() {
		return this.namespace;
	}

	protected Class<T> getDomainClass() {
		return this.entityInformation.getJavaType();
	}

	@Override
	public List<T> findAll() {
		return this.selectList(ResidentStatementName.FIND);
	}

	@Override
	public List<T> findAll(Sort sort) {
		if (null == sort || sort.isUnsorted()) {
			return this.findAll();
		}
		return this.selectList(ResidentStatementName.FIND, Collections.singletonMap(ResidentParameterName.SORT, sort));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return this.findAll(pageable, null);
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		Assert.notNull(ids, "The given iterable of ids must not be null.");
		if (!ids.iterator().hasNext()) {
			return Collections.emptyList();
		}

		return this.selectList(ResidentStatementName.FIND,
				Collections.singletonMap(ResidentParameterName.IDS, toCollection(ids)));
	}

	@Override
	public long count() {
		return this.selectOne(ResidentStatementName.COUNT_ALL);
	}

	@Override
	@Transactional
	public void deleteById(ID id) {
		this.removeById(id);
	}

	@Override
	@Transactional
	public void delete(T entity) {
		this.remove(entity);
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Entities must not be null.");
		entities.forEach(this::delete);
	}

	@Override
	@Transactional
	public void deleteAll() {
		this.deleteAll(this.findAll());
	}

	@Override
	@Transactional
	public int removeById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return this.delete(ResidentStatementName.DELETE_BY_ID, id);
	}

	@Override
	@Transactional
	public int remove(T entity) {
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
		// if (this.entityInformation.isNew(entity)) {
		// return 0;
		// }
		return this.removeById(this.entityInformation.getId(entity));
	}

	@Override
	@Transactional
	public int removeAll(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Entities must not be null.");
		AtomicInteger count = new AtomicInteger();
		entities.forEach(entity -> {
			this.remove(entity);
			count.getAndIncrement();
		});
		return count.get();
	}

	@Override
	@Transactional
	public int removeAll() {
		List<T> all = this.findAll();
		this.removeAll(all);
		return all.size();
	}

	@Override
	@Transactional
	public int deleteInBatch(Iterable<T> entities) {
		Assert.notNull(entities, "Entities must not be null.");
		if (!entities.iterator().hasNext()) {
			return 0;
		}

		Set<ID> ids = Streamable.of(entities).stream().map(this.entityInformation::getRequiredId)
				.collect(Collectors.toSet());

		return this.deleteInBatchById(ids);
	}

	@Override
	@Transactional
	public int deleteInBatchById(Iterable<ID> ids) {
		Assert.notNull(ids, "The given iterable of ids must not be null.");
		if (!ids.iterator().hasNext()) {
			return 0;
		}

		return this.delete(ResidentStatementName.DELETE_BY_IDS,
				Collections.singletonMap(ResidentParameterName.IDS, toCollection(ids)));
	}

	@Override
	@Transactional
	public int deleteAllInBatch() {
		return this.delete(ResidentStatementName.DELETE_ALL);
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {
		if (this.entityInformation.hasCompositeId()) {
			T target = this.getById(this.entityInformation.getId(entity));
			if (null == target) {
				return this.insert(entity);
			}
			return this.update(entity);

		}
		return this.entityInformation.isNew(entity) ? this.insert(entity) : this.update(entity);
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		Assert.notNull(entities, "Entities must not be null.");

		return Streamable.of(entities).stream().map(this::save).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveSelectiveAll(Iterable<S> entities) {
		Assert.notNull(entities, "Entities must not be null.");

		return Streamable.of(entities).stream().map(this::saveSelective).collect(Collectors.toList());
	}

	@Override
	public Optional<T> findById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);

		return Optional.ofNullable(this.getById(id));
	}

	@Override
	public boolean existsById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return this.findById(id).isPresent();
	}

	@Override
	public T getById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return this.selectOne(ResidentStatementName.GET_BY_ID, id);
	}

	@Override
	public <X extends T> T getOne(X condition) {
		return this.selectOne(ResidentStatementName.FIND,
				Collections.singletonMap(ResidentParameterName.CONDITION, condition));
	}

	@Override
	public <X extends T> Optional<T> findOne(X condition) {
		return Optional.ofNullable(this.getOne(condition));
	}

	@Override
	public <X extends T> List<T> findAll(X condition) {
		return this.selectList(ResidentStatementName.FIND,
				Collections.singletonMap(ResidentParameterName.CONDITION, condition));
	}

	@Override
	public <X extends T> List<T> findAll(Sort sort, X condition) {
		if (null == sort || sort.isUnsorted()) {
			return this.findAll(condition);
		}

		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.SORT, sort);
		params.put(ResidentParameterName.CONDITION, condition);
		return this.selectList(ResidentStatementName.FIND, params);
	}

	@Override
	public <X extends T> long countAll(X condition) {
		return this.selectOne(ResidentStatementName.COUNT,
				Collections.singletonMap(ResidentParameterName.CONDITION, condition));
	}

	@Override
	public <X extends T> Page<T> findAll(Pageable pageable, X condition) {
		if (null == pageable || pageable.isUnpaged()) {
			// FIXME use a default pageable?
			List<T> content = this.findAll(condition);
			return new PageImpl<>(content, pageable, content.size());
		}
		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.OFFSET, pageable.getOffset());
		params.put(ResidentParameterName.PAGE_SIZE, pageable.getPageSize());
		params.put(ResidentParameterName.OFFSET_END, pageable.getOffset() + pageable.getPageSize());
		if (null != pageable.getSort() && pageable.getSort().isSorted()) {
			params.put(ResidentParameterName.SORT, pageable.getSort());
		}
		List<T> content = this.selectList(ResidentStatementName.FIND_BY_PAGER, params);
		return PageableExecutionUtils.getPage(content, pageable,
				() -> this.selectOne(ResidentStatementName.COUNT, params));
	}

	@Override
	@Transactional
	public <S extends T> S saveSelective(S entity) {
		if (this.entityInformation.hasCompositeId()) {
			T target = this.getById(this.entityInformation.getId(entity));
			if (null == target) {
				return this.insertSelective(entity);
			}
			return this.updateSelective(entity);

		}
		return this.entityInformation.isNew(entity) ? this.insertSelective(entity) : this.updateSelective(entity);
	}

	@Override
	@Transactional
	public <S extends T> S insert(S entity) {
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		this.entityInformation.initVersion(entity);

		if (null != this.auditingHandler) {
			this.auditingHandler.markCreated(entity);
		}

		this.insert(ResidentStatementName.INSERT, entity);
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S insertSelective(S entity) {
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
		this.entityInformation.initVersion(entity);
		if (null != this.auditingHandler) {
			this.auditingHandler.markCreated(entity);
		}
		this.insert(ResidentStatementName.INSERT_SELECTIVE, entity);
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S update(S entity) {
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
		if (null != this.auditingHandler) {
			this.auditingHandler.markModified(entity);
		}
		this.update(ResidentStatementName.UPDATE, Collections.singletonMap(ResidentParameterName.ENTITY, entity));
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S update(ID id, S entity) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
		if (null != this.auditingHandler) {
			this.auditingHandler.markModified(entity);
		}
		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.ID, id);
		params.put(ResidentParameterName.ENTITY, entity);
		this.update(ResidentStatementName.UPDATE_BY_ID, params);
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S updateSelective(S entity) {
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
		if (null != this.auditingHandler) {
			this.auditingHandler.markModified(entity);
		}
		this.update(ResidentStatementName.UPDATE_SELECTIVE,
				Collections.singletonMap(ResidentParameterName.ENTITY, entity));
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S updateSelective(ID id, S entity) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);
		if (null != this.auditingHandler) {
			this.auditingHandler.markModified(entity);
		}
		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.ID, id);
		params.put(ResidentParameterName.ENTITY, entity);
		this.update(ResidentStatementName.UPDATE_SELECTIVE_BY_ID, params);
		return entity;
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		Assert.notNull(example, "Example must not be null.");
		ExampleMatcher matcher = example.getMatcher();
		ExampleMatcherAccessor accessor = new ExampleMatcherAccessor(matcher);
		S entity = example.getProbe();
		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.MATCHER, matcher);
		params.put(ResidentParameterName.ACCESSOR, accessor);
		params.put(ResidentParameterName.ENTITY, entity);

		S result = this.selectOne(ResidentStatementName.QUERY_BY_EXAMPLE, params);

		return Optional.ofNullable(result);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {

		return this.findAll(example, (Sort) null);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		Assert.notNull(example, "Example must not be null.");
		ExampleMatcher matcher = example.getMatcher();
		ExampleMatcherAccessor accessor = new ExampleMatcherAccessor(matcher);
		S entity = example.getProbe();
		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.MATCHER, matcher);
		params.put(ResidentParameterName.ACCESSOR, accessor);
		params.put(ResidentParameterName.ENTITY, entity);
		if (null != sort && sort.isSorted()) {
			params.put(ResidentParameterName.SORT, sort);
		}
		return this.selectList(ResidentStatementName.QUERY_BY_EXAMPLE, params);
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		Assert.notNull(example, "Example must not be null.");

		if (null == pageable || pageable.isUnpaged()) {
			return new PageImpl<>(this.findAll(example, (null != pageable) ? pageable.getSort() : null));
		}

		ExampleMatcher matcher = example.getMatcher();
		ExampleMatcherAccessor accessor = new ExampleMatcherAccessor(matcher);
		S entity = example.getProbe();
		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.MATCHER, matcher);
		params.put(ResidentParameterName.ACCESSOR, accessor);
		params.put(ResidentParameterName.ENTITY, entity);
		params.put(ResidentParameterName.OFFSET, pageable.getOffset());
		params.put(ResidentParameterName.PAGE_SIZE, pageable.getPageSize());
		params.put(ResidentParameterName.OFFSET_END, pageable.getOffset() + pageable.getPageSize());
		if (null != pageable.getSort() && pageable.getSort().isSorted()) {
			params.put(ResidentParameterName.SORT, pageable.getSort());
		}
		List<S> content = this.selectList(ResidentStatementName.QUERY_BY_EXAMPLE_FOR_PAGE, params);
		return PageableExecutionUtils.getPage(content, pageable,
				() -> this.selectOne(ResidentStatementName.COUNT_QUERY_BY_EXAMPLE, params));

	}

	@Override
	public <S extends T> long count(Example<S> example) {
		Assert.notNull(example, "Example must not be null.");
		ExampleMatcher matcher = example.getMatcher();
		ExampleMatcherAccessor accessor = new ExampleMatcherAccessor(matcher);
		S entity = example.getProbe();
		Map<String, Object> params = new HashMap<>();
		params.put(ResidentParameterName.MATCHER, matcher);
		params.put(ResidentParameterName.ACCESSOR, accessor);
		params.put(ResidentParameterName.ENTITY, entity);

		return this.selectOne(ResidentStatementName.COUNT_QUERY_BY_EXAMPLE, params);
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		return this.count(example) > 0;
	}

	@Override
	public void setEscapeCharacter(EscapeCharacter escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

}
