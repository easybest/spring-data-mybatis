package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.PersistentEntity;

/**
 * @author Jarvis Song
 */
public interface MyBatisPersistentEntity<T> extends PersistentEntity<T, MyBatisPersistentProperty> {

	String getTableName();

}
