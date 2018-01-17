package org.springframework.data.mybatis.repository;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.util.List;

/**
 * MyBatis specific extension of {@link org.springframework.data.repository.Repository}.
 * 
 * @author Jarvis Song
 */
@NoRepositoryBean
public interface MyBatisRepository<T, ID> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {

	@Override
	List<T> findAll();

	@Override
	List<T> findAll(Sort sort);

	@Override
	List<T> findAllById(Iterable<ID> ids);

	@Override
	<S extends T> List<S> saveAll(Iterable<S> entities);

	@Override
	<S extends T> List<S> findAll(Example<S> example);

	@Override
	<S extends T> List<S> findAll(Example<S> example, Sort sort);

	/*** Extends ***/

	/**
	 * insert entity.
	 * 
	 * @param entity
	 * @return
	 */
	<S extends T> S insert(S entity);

	/**
	 * force update entity.
	 * 
	 * @param entity
	 * @return
	 */
	<S extends T> S update(S entity);

	/**
	 * force update entity ignore null properties.
	 * 
	 * @param entity
	 * @return
	 */
	<S extends T> S updateIgnoreNull(S entity);

	/**
	 * save entity ignore null properties.
	 * 
	 * @param entity
	 * @return
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
