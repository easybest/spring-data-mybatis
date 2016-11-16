package org.springframework.data.mybatis.repository;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songjiawei on 2016/11/9.
 */
@NoRepositoryBean
public interface MybatisRepository<T, ID extends Serializable>
        extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {

    @Override
    List<T> findAll();

    @Override
    List<T> findAll(Sort sort);

    @Override
    List<T> findAll(Iterable<ID> ids);

    @Override
    <S extends T> List<S> findAll(Example<S> example);

    @Override
    <S extends T> List<S> findAll(Example<S> example, Sort sort);


    /***  Query with association ***/
    T findBasicOne(ID id, String... columns);

    <X extends T> T findOne(X condition, String... columns);

    <X extends T> List<T> findAll(X condition, String... columns);

    <X extends T> List<T> findAll(Sort sort, X condition, String... columns);

    <X extends T> Page<T> findAll(Pageable pageable, X condition, String... columns);

    <X extends T> Long countAll(X condition);

    /*** Query with non association ***/

    <X extends T> T findBasicOne(X condition, String... columns);

    <X extends T> List<T> findBasicAll(X condition, String... columns);

    <X extends T> List<T> findBasicAll(Sort sort, X condition, String... columns);

    <X extends T> Page<T> findBasicAll(Pageable pageable, X condition, String... columns);

    <X extends T> Long countBasicAll(X condition);


    <X extends T> int deleteByCondition(X condition);
}
