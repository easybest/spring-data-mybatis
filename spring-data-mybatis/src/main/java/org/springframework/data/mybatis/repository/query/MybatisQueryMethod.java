package org.springframework.data.mybatis.repository.query;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

import java.lang.reflect.Method;

/**
 * The mybatis implementation of {@link QueryMethod}.
 *
 * @author Jarvis Song
 */
public class MybatisQueryMethod extends QueryMethod {
	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct query to use for following
	 * invocations of the method given.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	public MybatisQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
	}
}
