package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.SimplePropertyHandler;

/**
 * PersistentEntity's Extension.
 * 
 * @author Jarvis Song
 */
public interface MyBatisPersistentEntity<T> extends PersistentEntity<T, MyBatisPersistentProperty> {
	/**
	 * Get mapping context.
	 * 
	 * @return
	 */
	MyBatisMappingContext getMappingContext();

	/**
	 * Get table's name.
	 * 
	 * @return
	 */
	String getTableName();

	/**
	 * If this entity has composite id.
	 * 
	 * @return
	 */
	boolean hasCompositeIdProperty();

	Class<?> getCompositeIdClass();

	MyBatisPersistentEntity<?> getCompositeIdPersistentEntity();

	Class<?> getIdClass();

	String getEntityName();

	void doWithIdProperties(SimplePropertyHandler handler);

}
