package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.MyBatisRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of the {@link org.springframework.data.repository.CrudRepository} interface.
 * 
 * @author Jarvis Song
 * @param <T> the type of the entity to handle
 * @param <ID> the type of the entity's identifier
 */
@Repository
@Transactional(readOnly = true, rollbackFor = { Throwable.class })
public class SimpleMyBatisRepository<T, ID> extends SqlSessionRepositorySupport implements MyBatisRepository<T, ID> {

	private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";

	private final MyBatisEntityInformation<T, ID> entityInformation;
	private final String namespace;

	public SimpleMyBatisRepository(RepositoryInformation repositoryInformation,
			MyBatisEntityInformation<T, ID> entityInformation, SqlSessionTemplate sqlSessionTemplate) {

		super(sqlSessionTemplate);

		Assert.notNull(entityInformation, "MyBatisEntityInformation must not be null!");

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

		return selectOne("_getById", id);
	}

	@Override
	public List<T> findAll() {
		return findAll((T) null);
	}

	@Override
	public List<T> findAll(Sort sort) {
		return selectList("_findAll", new HashMap<String, Object>(1) {
			{
				put("_sorts", sort);
			}
		});
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return findAll(pageable, null);
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		Assert.notNull(ids, "The given Iterable of Id's must not be null!");

		return selectList("_findAll", new HashMap<String, Object>(1) {
			{
				put("_ids", ids);
			}
		});
	}

	@Override
	public <X extends T> T getOne(X condition) {
		return selectOne("_findAll", new HashMap<String, Object>() {
			{
				put("_condition", condition);
			}
		});
	}

	@Override
	public <X extends T> Optional<T> findOne(X condition) {
		return Optional.ofNullable(getOne(condition));
	}

	@Override
	public <X extends T> List<T> findAll(X condition) {
		return selectList("_findAll", new HashMap<String, Object>() {
			{
				put("_condition", condition);
			}
		});
	}

	@Override
	public <X extends T> List<T> findAll(Sort sort, X condition) {
		return selectList("_findAll", new HashMap<String, Object>() {
			{
				put("_sort", sort);
				put("_condition", condition);
			}
		});
	}

	@Override
	public <X extends T> Page<T> findAll(Pageable pageable, X condition) {
		if (isUnpaged(pageable)) {
			return new PageImpl<>(findAll());
		}
		return findByPager(pageable, "_findByPager", "_countByCondition", condition);
	}

	@Override
	public <X extends T> long countAll(X condition) {
		return selectOne("_countByCondition", new HashMap<String, Object>() {
			{
				put("_condition", condition);
			}
		});
	}

	@Override
	public long count() {
		return selectOne("_count");
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public void deleteById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		super.delete("_deleteById", id);
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public void delete(T entity) {
		Assert.notNull(entity, "The entity must not be null!");
		deleteById(entityInformation.getId(entity));
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public void deleteAll(Iterable<? extends T> entities) {
		Assert.notNull(entities, "The given Iterable of entities not be null!");
		entities.forEach(entity -> {
			delete(entity);
		});
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public void deleteAll() {
		super.delete("_deleteAll");
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public <S extends T> S save(S entity) {

		if (entityInformation.isNew(entity)) {
			return insert(entity);
		}
		return update(entity);
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public <S extends T> List<S> saveAll(Iterable<S> entities) {

		Assert.notNull(entities, "The given Iterable of entities not be null!");
		entities.forEach(entity -> {
			save(entity);
		});
		return (List<S>) entities;
	}

	@Override
	public Optional<T> findById(ID id) {

		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return Optional.ofNullable(getById(id));
	}

	@Override
	public boolean existsById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return findBasicById(id).isPresent();
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		return Optional.empty();
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		return null;
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		return null;
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		return null;
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		return 0;
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		return false;
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public <S extends T> S insert(S entity) {
		// TODO audit
		// TODO version
		insert("_insert", entity);
		return entity;
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public <S extends T> S update(S entity) {

		// TODO modify date and modifier process

		int row = super.update("_update", entity);
		// TODO @Version control

		return entity;
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public <S extends T> S updateIgnoreNull(S entity) {
		// TODO modify date and modifier process

		int row = super.update("_updateIgnoreNull", entity);
		// TODO @Version control

		return entity;
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public <S extends T> S saveIgnoreNull(S entity) {

		if (entityInformation.isNew(entity)) {
			return insert(entity);
		}
		return updateIgnoreNull(entity);
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public void deleteInBatch(Iterable<T> entities) {
		Assert.notNull(entities, "The given Iterable of entities not be null!");
		entities.forEach(entity -> {
			delete(entity);
		});
	}

	@Override
	@Transactional(rollbackFor = { Throwable.class })
	public void deleteAllInBatch() {
		super.delete("_deleteAll");
	}

	@Override
	public Optional<T> findBasicById(ID id) {
		return Optional.ofNullable(getBasicById(id));
	}

	@Override
	public T getBasicById(ID id) {
		Assert.notNull(id, ID_MUST_NOT_BE_NULL);
		return selectOne("_getBasicById", id);
	}

	@Override
	public List<T> findBasicAll() {
		return findBasicAll((T) null);
	}

	@Override
	public List<T> findBasicAll(Sort sort) {
		return selectList("_findBasicAll", new HashMap<String, Object>(1) {
			{
				put("_sorts", sort);
			}
		});
	}

	@Override
	public List<T> findBasicAllById(Iterable<ID> ids) {
		return selectList("_findBasicAll", new HashMap<String, Object>(1) {
			{
				put("_ids", ids);
			}
		});
	}

	@Override
	public <X extends T> T getBasicOne(X condition) {
		return selectOne("_findBasicAll", new HashMap<String, Object>() {
			{
				put("_condition", condition);
			}
		});
	}

	@Override
	public <X extends T> Optional<T> findBasicOne(X condition) {
		return Optional.ofNullable(getBasicOne(condition));
	}

	@Override
	public <X extends T> List<T> findBasicAll(X condition) {
		return selectList("_findBasicAll", new HashMap<String, Object>() {
			{
				put("_condition", condition);
			}
		});
	}

	@Override
	public <X extends T> List<T> findBasicAll(Sort sort, X condition) {
		return selectList("_findBasicAll", new HashMap<String, Object>() {
			{
				put("_sort", sort);
				put("_condition", condition);
			}
		});
	}

	@Override
	public <X extends T> Page<T> findBasicAll(Pageable pageable, X condition) {
		if (isUnpaged(pageable)) {
			return new PageImpl<>(findBasicAll());
		}
		return findByPager(pageable, "_findBasicByPager", "_countBasicByCondition", condition);
	}

	@Override
	public <X extends T> long countBasicAll(X condition) {
		return selectOne("_countBasicByCondition", new HashMap<String, Object>() {
			{
				put("_condition", condition);
			}
		});
	}

	private static boolean isUnpaged(Pageable pageable) {
		return pageable.isUnpaged();
	}
}
