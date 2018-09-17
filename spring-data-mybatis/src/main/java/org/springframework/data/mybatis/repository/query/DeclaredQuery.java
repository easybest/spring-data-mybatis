package org.springframework.data.mybatis.repository.query;

import org.springframework.data.mybatis.repository.annotation.Query;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Jarvis Song
 */
public interface DeclaredQuery {

	static DeclaredQuery of(@Nullable Query query) {
		if (null == query) {
			return EmptyDeclaredQuery.EMPTY_QUERY;
		}


	}

	static DeclaredQuery of(@Nullable String query) {
		return StringUtils.isEmpty(query) ? EmptyDeclaredQuery.EMPTY_QUERY : new StringQuery(query);
	}

	String namespace();

	String statement();

	String getQueryString();

	DeclaredQuery deriveCountQuery(@Nullable String countQuery, @Nullable String countQueryProjection);

}
