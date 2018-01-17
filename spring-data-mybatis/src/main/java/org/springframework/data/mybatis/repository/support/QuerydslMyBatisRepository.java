package org.springframework.data.mybatis.repository.support;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.PathBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;

import java.io.Serializable;
import java.util.Optional;

/**
 * @author Jarvis Song
 */
public class QuerydslMyBatisRepository<T, ID extends Serializable> extends SimpleMyBatisRepository<T, ID>
		implements QuerydslPredicateExecutor<T> {

	private static final EntityPathResolver DEFAULT_ENTITY_PATH_RESOLVER = SimpleEntityPathResolver.INSTANCE;

	private final EntityPath<T> path;
	private final PathBuilder<T> builder;

	public QuerydslMyBatisRepository(MyBatisEntityInformation<T, ?> entityInformation,
			SqlSessionTemplate sqlSessionTemplate) {

		this(entityInformation, sqlSessionTemplate, DEFAULT_ENTITY_PATH_RESOLVER);
	}

	public QuerydslMyBatisRepository(MyBatisEntityInformation<T, ?> entityInformation,
			SqlSessionTemplate sqlSessionTemplate, EntityPathResolver resolver) {

		super(entityInformation, sqlSessionTemplate);

		this.path = resolver.createPath(entityInformation.getJavaType());
		this.builder = new PathBuilder<T>(path.getType(), path.getMetadata());
	}

	@Override
	public Optional<T> findOne(Predicate predicate) {
		return Optional.empty();
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
