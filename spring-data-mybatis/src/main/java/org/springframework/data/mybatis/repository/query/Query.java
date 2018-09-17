package org.springframework.data.mybatis.repository.query;

import org.springframework.lang.Nullable;

import java.util.List;

public interface Query<R> {

	List<R> getResultList(@Nullable Object[] values);

	@Nullable
	R getSingleResult(@Nullable Object[] values);

	int executeUpdate(@Nullable Object[] values);

}
