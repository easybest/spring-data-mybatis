package org.springframework.data.mybatis.repository.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link CrudRepository} and
 * {@link PagingAndSortingRepository}.
 *
 * @param <T> Domain Class
 * @param <ID> Primary key type
 * @author JARVIS SONG
 */
@Repository
@Transactional(readOnly = true)
public class SimpleMybatisRepository<T, ID> extends SqlSessionRepositorySupport
		implements MybatisRepository<T, ID> {

	private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";

	private final String namespace;

	private final MybatisEntityInformation<T, ID> entityInformation;

	public SimpleMybatisRepository(SqlSessionTemplate sqlSessionTemplate,
			RepositoryInformation repositoryInformation,
			MybatisEntityInformation<T, ID> entityInformation) {

		super(sqlSessionTemplate);

		Assert.notNull(entityInformation, "EntityInformation must not be null!");

		this.namespace = repositoryInformation.getRepositoryInterface().getName();
		this.entityInformation = entityInformation;
	}

	@Override
	protected String getNamespace() {
		return namespace;
	}

	@Override
	public T getById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return selectOne("__get_by_id", id);
	}

	@Override
	public List<T> findAll() {
		return selectList("__find");
	}

	@Override
	public List<T> findAll(Sort sort) {
		if (null == sort || sort.isUnsorted()) {
			return findAll();
		}
		return selectList("__find", Collections.singletonMap("__sort", sort));
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		Assert.notNull(ids, "The given Iterable of Id's must not be null!");
		List<ID> idsCopy = new ArrayList<>();
		ids.iterator().forEachRemaining(idsCopy::add);
		if (idsCopy.isEmpty()) {
			return Collections.emptyList();
		}
		return selectList("__find", Collections.singletonMap("__ids", idsCopy));
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		Assert.notNull(entities, "The given iterable of entities not be null!");
		entities.forEach(this::save);
		List<S> result = new ArrayList<>();
		entities.iterator().forEachRemaining(result::add);
		return result;
	}

	@Override
	public <X extends T> T getOne(X condition) {

		return selectOne("__find", Collections.singletonMap("__condition", condition));
	}

	@Override
	public <X extends T> Optional<T> findOne(X condition) {
		return Optional.ofNullable(getOne(condition));
	}

	@Override
	public <X extends T> List<T> findAll(X condition) {
		return selectList("__find", Collections.singletonMap("__condition", condition));
	}

	@Override
	public <X extends T> List<T> findAll(Sort sort, X condition) {
		if (null == sort || sort.isUnsorted()) {
			return findAll(condition);
		}
		return selectList("__find", new HashMap<String, Object>() {
			{
				put("__sort", sort);
				put("__condition", condition);
			}
		});
	}

	@Override
	public <X extends T> Page<T> findAll(Pageable pageable, X condition) {
		if (null == pageable || pageable.isUnpaged()) {
			// FIXME USE A DEFAULT PAGEABLE?
			return new PageImpl<>(findAll(condition));
		}
		return findByPager(pageable, "__find_by_pager", "__count", condition);
	}

	@Override
	public <X extends T> long countAll(X condition) {
		return selectOne("__count", Collections.singletonMap("__condition", condition));
	}

	@Override
	@Transactional
	public <S extends T> S insert(S entity) {
		insert("__insert", entity);
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S update(S entity) {
		update("__update", entity);
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S updateIgnoreNull(S entity) {
		update("__update_ignore_null", entity);
		return entity;
	}

	@Override
	@Transactional
	public <S extends T> S saveIgnoreNull(S entity) {

		return entityInformation.isNew(entity) ? insert(entity)
				: updateIgnoreNull(entity);
	}

	@Override
	@Transactional
	public void deleteInBatch(Iterable<T> entities) {
		Assert.notNull(entities, "The given Iterable of entities not be null!");
		entities.forEach(this::delete);
	}

	@Override
	@Transactional
	public void deleteAllInBatch() {
		delete("__delete_all");
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return findAll(pageable, null);
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {
		return entityInformation.isNew(entity) ? insert(entity) : update(entity);
	}

	@Override
	public Optional<T> findById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);

		return Optional.ofNullable(getById(id));
	}

	@Override
	public boolean existsById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);

		return findById(id).isPresent();
	}

	@Override
	public long count() {
		return selectOne("__count_all");
	}

	@Override
	@Transactional
	public void deleteById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		delete("__delete_by_id", id);
	}

	@Override
	@Transactional
	public void delete(T entity) {
		Assert.notNull(entity, "The entity must not be null!");
		deleteById(entityInformation.getId(entity));
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {
		Assert.notNull(entities, "The given Iterable of entities not be null!");
		entities.forEach(this::delete);
	}

	@Override
	@Transactional
	public void deleteAll() {
		delete("__delete_all");
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {

		// TODO
		return null;
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		// TODO
		return null;
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		// TODO
		return Optional.empty();
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		// TODO
		return null;
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		// TODO
		return 0;
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		// TODO
		return false;
	}

}
