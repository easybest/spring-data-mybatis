package org.springframework.data.mybatis.repository;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis specific extension of {@link org.springframework.data.repository.Repository}.
 * 
 * @author Jarvis Song
 */
@NoRepositoryBean
public interface MyBatisRepository<T, ID> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {
	/**
	 * Returns a reference to the entity with the given identifier.
	 *
	 * @param id must not be {@literal null}.
	 * @return a reference to the entity with the given identifier.
	 */
	T getById(ID id);

	@Override
	List<T> findAll();

	@Override
	List<T> findAll(Sort sort);

	@Override
	List<T> findAllById(Iterable<ID> ids);

	<X extends T> T getOne(X condition);

	<X extends T> Optional<T> findOne(X condition);

	<X extends T> List<T> findAll(X condition);

	<X extends T> List<T> findAll(Sort sort, X condition);

	<X extends T> Page<T> findAll(Pageable pageable, X condition);

	<X extends T> long countAll(X condition);

	@Override
	<S extends T> List<S> saveAll(Iterable<S> entities);

	@Override
	<S extends T> List<S> findAll(Example<S> example);

	@Override
	<S extends T> List<S> findAll(Example<S> example, Sort sort);

	/*** Extends ***/

	/**
	 * insert entity.
	 */
	<S extends T> S insert(S entity);

	/**
	 * force update entity.
	 */
	<S extends T> S update(S entity);

	/**
	 * force update entity ignore null properties.
	 */
	<S extends T> S updateIgnoreNull(S entity);

	/**
	 * save entity ignore null properties.
	 */
	<S extends T> S saveIgnoreNull(S entity);

	/**
	 * batch delete entities.
	 * 
	 * @param entities
	 */
	void deleteInBatch(Iterable<T> entities);

	/**
	 * batch delete all entities.
	 */
	void deleteAllInBatch();

}
