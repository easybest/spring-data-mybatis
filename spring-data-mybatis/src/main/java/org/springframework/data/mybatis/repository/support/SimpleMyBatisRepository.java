package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.MyBatisRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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

	private static final String STATEMENT_INSERT = "_insert";
	private static final String STATEMENT_UPDATE = "_update";
	private static final String STATEMENT_UPDATE_IGNORE_NULL = "_updateIgnoreNull";
	private static final String STATEMENT_GET_BY_ID = "_getById";
	private static final String STATEMENT_DELETE_BY_ID = "_deleteById";

	private final MyBatisEntityInformation entityInformation;

	public SimpleMyBatisRepository(MyBatisEntityInformation<T, ?> entityInformation,
			SqlSessionTemplate sqlSessionTemplate) {

		super(sqlSessionTemplate);

		Assert.notNull(entityInformation, "MyBatisEntityInformation must not be null!");

		this.entityInformation = entityInformation;
	}

	@Override
	protected String getNamespace() {
		return entityInformation.getJavaType().getName();
	}

	@Override
	public List<T> findAll() {
		return null;
	}

	@Override
	public List<T> findAll(Sort sort) {
		return null;
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return null;
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		return null;
	}

	@Override
	public long count() {
		return 0;
	}

	@Override
	public void deleteById(ID id) {

	}

	@Override
	public void delete(T entity) {

	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {

	}

	@Override
	public void deleteAll() {

	}

	@Override
	public <S extends T> S save(S entity) {

		if (entityInformation.isNew(entity)) {
			return insert(entity);
		}
		return update(entity);
	}

	@Override
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		return null;
	}

	@Override
	public Optional<T> findById(ID id) {
		return Optional.empty();
	}

	@Override
	public boolean existsById(ID id) {
		return false;
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
	public <S extends T> S insert(S entity) {
		// TODO audit
		// TODO version
		insert(STATEMENT_INSERT, entity);
		return entity;
	}

	@Override
	public <S extends T> S update(S entity) {
		return null;
	}

	@Override
	public <S extends T> S updateIgnoreNull(S entity) {
		return null;
	}

	@Override
	public <S extends T> S saveIgnoreNull(S entity) {

		if (entityInformation.isNew(entity)) {
			return insert(entity);
		}
		return updateIgnoreNull(entity);
	}

	@Override
	public void deleteInBatch(Iterable<T> entities) {

	}

	@Override
	public void deleteAllInBatch() {

	}
}
