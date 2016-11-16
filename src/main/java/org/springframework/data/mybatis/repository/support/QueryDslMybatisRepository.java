package org.springframework.data.mybatis.repository.support;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import java.io.Serializable;

/**
 * Created by songjiawei on 2016/11/9.
 */
public class QueryDslMybatisRepository<T, ID extends Serializable> extends SimpleMybatisRepository<T, ID> implements QueryDslPredicateExecutor<T> {


    public QueryDslMybatisRepository(MybatisEntityInformation<T, ID> entityInformation, SqlSessionTemplate sqlSessionTemplate) {
        super(entityInformation, sqlSessionTemplate);
        throw new UnsupportedOperationException("unsupported QueryDsl Repository...");
    }

    @Override
    public T findOne(Predicate predicate) {
        return null;
    }

    @Override
    public Iterable<T> findAll(Predicate predicate) {
        return null;
    }

    @Override
    public Iterable<T> findAll(Predicate predicate, Sort sort) {
        return null;
    }

    @Override
    public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
        return null;
    }

    @Override
    public Iterable<T> findAll(OrderSpecifier<?>... orders) {
        return null;
    }

    @Override
    public Page<T> findAll(Predicate predicate, Pageable pageable) {
        return null;
    }

    @Override
    public long count(Predicate predicate) {
        return 0;
    }

    @Override
    public boolean exists(Predicate predicate) {
        return false;
    }
}
