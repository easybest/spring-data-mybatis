package org.springframework.data.mybatis.repository.query;

import org.springframework.util.Assert;

public class EmptyDeclaredQuery implements DeclaredQuery {

	static final DeclaredQuery EMPTY_QUERY = new EmptyDeclaredQuery();

	@Override
	public String namespace() {
		return null;
	}

	@Override
	public String statement() {
		return null;
	}

	@Override
	public String getQueryString() {
		return "";
	}

	@Override
	public DeclaredQuery deriveCountQuery(String countQuery, String countQueryProjection) {
		Assert.hasText(countQuery, "CountQuery must not be empty!");
		return DeclaredQuery.of(countQuery);
	}
}
