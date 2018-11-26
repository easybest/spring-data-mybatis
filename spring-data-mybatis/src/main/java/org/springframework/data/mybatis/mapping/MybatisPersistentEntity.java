package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.FieldNamingStrategy;

public interface MybatisPersistentEntity<T>
		extends PersistentEntity<T, MybatisPersistentProperty> {

	String getTableName();

	FieldNamingStrategy getFieldNamingStrategy();

}
