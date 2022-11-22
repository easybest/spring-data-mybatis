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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;
import io.easybest.mybatis.mapping.MybatisPersistentEntity;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.repository.MybatisRepository;
import io.easybest.mybatis.repository.query.criteria.CriteriaQuery;
import io.easybest.mybatis.repository.query.criteria.impl.CriteriaQueryImpl;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.util.Streamable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import static io.easybest.mybatis.repository.support.ResidentStatementName.COUNT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.COUNT_ALL;
import static io.easybest.mybatis.repository.support.ResidentStatementName.COUNT_QUERY_BY_EXAMPLE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_ALL;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_ENTITIES;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_ENTITY;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_IDS;
import static io.easybest.mybatis.repository.support.ResidentStatementName.EXISTS_BY_EXAMPLE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.EXISTS_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_ALL;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_ALL_WITH_SORT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_CRITERIA;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_IDS;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_PAGE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.INSERT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.INSERT_SELECTIVE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE_WITH_PAGE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE_WITH_SORT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UPDATE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UPDATE_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UPDATE_SELECTIVE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UPDATE_SELECTIVE_BY_ID;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <ID> primary key
 */
@Transactional(readOnly = true)
public class SimpleMybatisRepository<T, ID> extends SqlSessionRepositorySupport implements MybatisRepository<T, ID> {

	private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";

	private static final String ENTITY_MUST_NOT_BE_NULL = "The entity must not be null!";

	private final EntityManager entityManager;

	private final MybatisPersistentEntity<T> persistentEntity;

	private final boolean basic;

	public SimpleMybatisRepository(EntityManager entityManager, EntityCallbacks entityCallbacks,
			MybatisPersistentEntity<T> entity) {

		super(entityManager.getSqlSessionTemplate(), entity.getType().getName());

		Assert.notNull(entityManager, "EntityManager must not be null.");
		Assert.notNull(entity, "Entity must not be null.");

		this.entityManager = entityManager;
		this.persistentEntity = entity;
		this.basic = entity.isBasic();
	}

	@Override
	@Transactional
	public <S extends T> S insert(S entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		// FIXME fill version property
		if (this.persistentEntity.hasVersionProperty()) {
			this.persistentEntity.getPropertyAccessor(entity)
					.setProperty(this.persistentEntity.getRequiredVersionProperty(), 0);
		}

		this.insert(INSERT, new MybatisContext<>(null, entity, this.persistentEntity.getType(), this.basic));

		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S insertCascade(S entity) {

		S inserted = this.insert(entity);
		// TODO

		if (this.basic) {
			return inserted;
		}

		PersistentPropertyAccessor<S> accessor = this.persistentEntity.getPropertyAccessor(entity);
		this.persistentEntity.doWithAssociations((AssociationHandler<MybatisPersistentPropertyImpl>) association -> {

			MybatisPersistentPropertyImpl inverse = association.getInverse();
			Object value = accessor.getProperty(inverse);
			System.out.println(value);
		});

		return inserted;
	}

	@Override
	@Transactional
	public <S extends T> S insertSelective(S entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		// FIXME fill version property
		if (this.persistentEntity.hasVersionProperty()) {
			this.persistentEntity.getPropertyAccessor(entity)
					.setProperty(this.persistentEntity.getRequiredVersionProperty(), 0);
		}

		this.insert(INSERT_SELECTIVE, new MybatisContext<>(null, entity, this.persistentEntity.getType(), this.basic));

		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S update(S entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		this.update(UPDATE, new MybatisContext<>(null, entity, this.persistentEntity.getType(), this.basic));

		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S updateSelective(S entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		this.update(UPDATE_SELECTIVE, new MybatisContext<>(null, entity, this.persistentEntity.getType(), this.basic));
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S update(ID id, S entity) {

		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		this.update(UPDATE_BY_ID, new MybatisContext<>(id, entity, this.persistentEntity.getType(), this.basic));
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S updateSelective(ID id, S entity) {

		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		this.update(UPDATE_SELECTIVE_BY_ID,
				new MybatisContext<>(id, entity, this.persistentEntity.getType(), this.basic));
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S updateCascade(S entity) {

		S updated = this.update(entity);

		PersistentPropertyAccessor<S> accessor = this.persistentEntity.getPropertyAccessor(updated);
		this.persistentEntity.doWithAssociations((AssociationHandler<MybatisPersistentPropertyImpl>) ass -> {

			MybatisAssociation association = (MybatisAssociation) ass;
			MybatisPersistentPropertyImpl inverse = association.getInverse();
			Object value = accessor.getProperty(inverse);
			if (null == value) {
				// FIXME
			}
		});

		return updated;
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		// FIXME sometimes, the entity is not newly, but without id, so how to judge it?
		if (this.persistentEntity.isNew(entity)) {
			return this.insert(entity);
		}

		// update

		return this.update(entity);
	}

	@Override
	@Transactional
	public <S extends T> S saveCascade(S entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		// FIXME sometimes, the entity is not newly, but without id, so how to judge it?
		if (this.persistentEntity.isNew(entity)) {
			return this.insertCascade(entity);
		}

		// TODO update

		return this.updateCascade(entity);
	}

	@Override
	public <S extends T> S saveSelective(S entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		// FIXME sometimes, the entity is not newly, but without id, so how to judge it?
		if (this.persistentEntity.isNew(entity)) {
			return this.insertSelective(entity);
		}

		// update selective

		return this.updateSelective(entity);
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveAll(Iterable<S> entities) {

		Assert.notNull(entities, "Entities must not be null!");

		return Streamable.of(entities).map(this::save).toList();
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveSelectiveAll(Iterable<S> entities) {
		Assert.notNull(entities, "Entities must not be null!");

		return Streamable.of(entities).map(this::saveSelective).toList();
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveCascadeAll(Iterable<S> entities) {

		Assert.notNull(entities, "Entities must not be null!");

		return Streamable.of(entities).map(this::saveCascade).toList();
	}

	@Override
	@Transactional
	public void deleteById(ID id) {

		Assert.notNull(id, ID_MUST_NOT_BE_NULL);

		if (this.persistentEntity.hasVersionProperty()) {
			this.findById(id).ifPresent(this::delete);
			return;
		}

		int affectRows;
		if (this.persistentEntity.getLogicDeleteColumn().isPresent()) {
			affectRows = this.update(DELETE_BY_ID,
					new MybatisContext<>(id, null, this.persistentEntity.getType(), this.basic));
		}
		else {
			affectRows = this.delete(DELETE_BY_ID,
					new MybatisContext<>(id, null, this.persistentEntity.getType(), this.basic));
		}
		if (affectRows == 0) {
			throw new EmptyResultDataAccessException(
					String.format("No %s entity with id %s exists!", this.persistentEntity.getType(), id), 1);
		}
	}

	@Override
	@Transactional
	public void delete(T entity) {

		Assert.notNull(entity, ENTITY_MUST_NOT_BE_NULL);

		if (this.persistentEntity.isNew(entity)) {
			return;
		}

		int affectRows;
		if (this.persistentEntity.getLogicDeleteColumn().isPresent()) {
			affectRows = this.update(DELETE_BY_ENTITY,
					new MybatisContext<>(null, entity, this.persistentEntity.getType(), this.basic));
		}
		else {
			affectRows = this.delete(DELETE_BY_ENTITY,
					new MybatisContext<>(null, entity, this.persistentEntity.getType(), this.basic));
		}
		if (affectRows == 0) {
			throw new EmptyResultDataAccessException(
					String.format("No %s entity with id %s exists!", this.persistentEntity.getType(),
							this.persistentEntity.getIdentifierAccessor(entity).getIdentifier()),
					1);
		}
	}

	@Override
	@Transactional
	public void deleteAllById(Iterable<? extends ID> ids) {

		Assert.notNull(ids, "The given iterable ids must not be null!");

		ids.forEach(this::deleteById);
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {

		Assert.notNull(entities, "Entities must not be null!");

		entities.forEach(this::delete);
	}

	@Override
	@Transactional
	public void deleteAll() {

		this.findAll().forEach(this::delete);
	}

	@Override
	@Transactional
	public void deleteAllInBatch(Iterable<T> entities) {

		Assert.notNull(entities, "Entities must not be null!");

		if (!entities.iterator().hasNext()) {
			return;
		}

		if (this.persistentEntity.getLogicDeleteColumn().isPresent()) {
			this.update(DELETE_BY_ENTITIES,
					new MybatisContext<>(null, entities, this.persistentEntity.getType(), this.basic));
			return;
		}

		this.delete(DELETE_BY_ENTITIES,
				new MybatisContext<>(null, entities, this.persistentEntity.getType(), this.basic));
	}

	@Override
	@Transactional
	public void deleteAllByIdInBatch(Iterable<ID> ids) {

		Assert.notNull(ids, "The given iterable ids must not be null!");

		if (!ids.iterator().hasNext()) {
			return;
		}

		if (this.persistentEntity.getLogicDeleteColumn().isPresent()) {
			this.update(DELETE_BY_IDS, new MybatisContext<>(ids, null, this.persistentEntity.getType(), this.basic));
			return;
		}

		this.delete(DELETE_BY_IDS, new MybatisContext<>(ids, null, this.persistentEntity.getType(), this.basic));
	}

	@Override
	@Transactional
	public void deleteAllInBatch() {

		if (this.persistentEntity.getLogicDeleteColumn().isPresent()) {
			this.update(DELETE_ALL);
			return;
		}

		this.delete(DELETE_ALL);
	}

	@Override
	public List<T> findAll() {

		return this.selectList(FIND_ALL);
	}

	@Override
	public List<T> findAll(Sort sort) {

		if (sort.isUnsorted()) {
			return this.findAll();
		}

		return this.selectList(FIND_ALL_WITH_SORT, new MybatisContext<>(null, null, this.persistentEntity.getType(),
				sort, this.entityManager, this.basic));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		if (pageable.isUnpaged()) {
			List<T> content = this.findAll(pageable.getSort());
			return new PageImpl<>(content, pageable, content.size());
		}

		MybatisContext<T, ID> context = new MybatisContext<>(null, null, this.persistentEntity.getType(),
				io.easybest.mybatis.repository.support.Pageable.of(pageable), pageable.getSort(), this.entityManager,
				this.basic);

		List<T> content = this.selectList(FIND_BY_PAGE, context);

		return PageableExecutionUtils.getPage(content, pageable, () -> this.selectOne(COUNT, content));
	}

	@Override
	public T getById(ID id) {

		Assert.notNull(id, ID_MUST_NOT_BE_NULL);

		return this.selectOne(FIND_BY_ID, new MybatisContext<>(id, null, this.persistentEntity.getType(), this.basic));
	}

	@Override
	public Optional<T> findById(ID id) {

		return Optional.ofNullable(this.getById(id));
	}

	@Override
	public boolean existsById(ID id) {

		Assert.notNull(id, ID_MUST_NOT_BE_NULL);

		return this.selectOne(EXISTS_BY_ID,
				new MybatisContext<>(id, null, this.persistentEntity.getType(), this.basic));
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {

		Assert.notNull(ids, "The given iterable ids must not be null.");

		if (!ids.iterator().hasNext()) {
			return Collections.emptyList();
		}

		return this.selectList(FIND_BY_IDS,
				new MybatisContext<>(ids, null, this.persistentEntity.getType(), this.basic));
	}

	@Override
	public long count() {

		return this.selectOne(COUNT_ALL);
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {

		Assert.notNull(example, "Example must not be null.");

		S result = this.selectOne(QUERY_BY_EXAMPLE, new MybatisContext<>(null, example.getProbe(),
				this.persistentEntity.getType(), example, this.entityManager, this.basic));

		return Optional.ofNullable(result);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {

		Assert.notNull(example, "Example must not be null.");

		return this.selectList(QUERY_BY_EXAMPLE, new MybatisContext<>(null, example.getProbe(),
				this.persistentEntity.getType(), example, this.entityManager, this.basic));
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {

		Assert.notNull(example, "Example must not be null.");

		if (sort.isUnsorted()) {
			return this.findAll(example);
		}

		return this.selectList(QUERY_BY_EXAMPLE_WITH_SORT, new MybatisContext<>(null, example.getProbe(),
				this.persistentEntity.getType(), null, sort, example, this.entityManager, this.basic));
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {

		Assert.notNull(example, "Example must not be null.");

		if (pageable.isUnpaged()) {
			return new PageImpl<>(this.findAll(example, pageable.getSort()));
		}

		MybatisContext<S, ID> context = new MybatisContext<>(null, example.getProbe(), this.persistentEntity.getType(),
				io.easybest.mybatis.repository.support.Pageable.of(pageable), pageable.getSort(), example,
				this.entityManager, this.basic);

		List<S> content = this.selectList(QUERY_BY_EXAMPLE_WITH_PAGE, context);

		return PageableExecutionUtils.getPage(content, pageable, () -> this.selectOne(COUNT_QUERY_BY_EXAMPLE, context));
	}

	@Override
	public <S extends T> long count(Example<S> example) {

		Assert.notNull(example, "Example must not be null.");

		return this.selectOne(COUNT_QUERY_BY_EXAMPLE, new MybatisContext<>(null, example.getProbe(),
				this.persistentEntity.getType(), example, this.entityManager, this.basic));
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {

		Assert.notNull(example, "Example must not be null.");

		return this.selectOne(EXISTS_BY_EXAMPLE, new MybatisContext<>(null, example.getProbe(),
				this.persistentEntity.getType(), example, this.entityManager, this.basic));

	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <S extends T, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {

		Assert.notNull(example, "Sample must not be null!");
		Assert.notNull(queryFunction, "Query function must not be null!");

		FetchableFluentQuery<S> fluentQuery = new FetchableFluentQueryByExample<>((QueryByExampleExecutor) this,
				example, example.getProbeType(), example.getProbeType(), Sort.unsorted(), Collections.emptySet());
		return queryFunction.apply(fluentQuery);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> Optional<S> findOne(CriteriaQuery<T, ?, ?, ?> criteria) {

		Assert.notNull(criteria, "Criteria must not be null!");

		Class<T> type = this.persistentEntity.getType();
		if (criteria instanceof CriteriaQueryImpl) {
			Class<T> domainClass = ((CriteriaQueryImpl<T, ?, ?, ?>) criteria).getDomainClass();
			if (null != domainClass) {
				type = domainClass;
			}
		}

		return Optional.ofNullable(this.selectOne(FIND_BY_CRITERIA,
				new MybatisContext<>(null, type, Collections.emptyMap(), this.basic, this.entityManager, criteria)));
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <S extends T> List<S> findAll(CriteriaQuery<T, ?, ?, ?> criteria) {

		Assert.notNull(criteria, "Criteria must not be null!");

		Class<T> type = this.persistentEntity.getType();
		if (criteria instanceof CriteriaQueryImpl) {
			Class<T> domainClass = ((CriteriaQueryImpl<T, ?, ?, Object>) criteria).getDomainClass();
			if (null != domainClass) {
				type = domainClass;
			}
		}

		return this.selectList(FIND_BY_CRITERIA,
				new MybatisContext<>(null, type, Collections.emptyMap(), this.basic, this.entityManager, criteria));
	}

}
