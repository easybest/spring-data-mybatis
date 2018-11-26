package org.springframework.data.mybatis.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

@NoRepositoryBean
public interface MybatisRepository<T, ID>
		extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {

	/**
	 * Returns a reference to the entity with the given identifier.
	 * @param id must not be {@literal null}.
	 * @return a reference to the entity with the given identifier. If the reference is
	 * not exist, will return null.
	 */
	T getById(ID id);

	@Override
	List<T> findAll();

	@Override
	List<T> findAll(Sort sort);

	@Override
	List<T> findAllById(Iterable<ID> ids);

	@Override
	<S extends T> List<S> findAll(Example<S> example);

	@Override
	<S extends T> List<S> findAll(Example<S> example, Sort sort);

	@Override
	<S extends T> List<S> saveAll(Iterable<S> entities);

	/************* find by condition *************/

	<X extends T> T getOne(X condition);

	<X extends T> Optional<T> findOne(X condition);

	<X extends T> List<T> findAll(X condition);

	<X extends T> List<T> findAll(Sort sort, X condition);

	<X extends T> Page<T> findAll(Pageable pageable, X condition);

	<X extends T> long countAll(X condition);

	/************* extends *************/

	/**
	 * Just execute a insert sql without checking id.
	 * @param entity
	 * @param <S>
	 * @return the inserted entity.
	 */
	<S extends T> S insert(S entity);

	/**
	 * Just execute a update sql without any check.
	 * @param entity
	 * @param <S>
	 * @return
	 */
	<S extends T> S update(S entity);

	<S extends T> S updateIgnoreNull(S entity);

	<S extends T> S saveIgnoreNull(S entity);

	void deleteInBatch(Iterable<T> entities);

	void deleteAllInBatch();

}
