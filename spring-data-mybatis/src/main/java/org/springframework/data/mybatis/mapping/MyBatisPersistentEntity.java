package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.PersistentEntity;

/**
 * PersistentEntity's Extension.
 * 
 * @author Jarvis Song
 */
public interface MyBatisPersistentEntity<T> extends PersistentEntity<T, MyBatisPersistentProperty> {

	Table getTable();

}
