package org.springframework.data.mybatis.repository.query;

import org.springframework.data.repository.core.EntityMetadata;

public interface MybatisEntityMetadata<T> extends EntityMetadata<T> {

	String getEntityName();

}
