package org.springframework.data.mybatis.mapping;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Jarvis Song
 */
public interface MyBatisPersistentProperty extends PersistentProperty<MyBatisPersistentProperty> {

	/**
	 * Mapping column's name.
	 * 
	 * @return
	 */
	String getColumnName();

	/**
	 * This id property weather is composite.
	 * 
	 * @return
	 */
	boolean isCompositeId();

	/**
	 * only with @Id and basic types.
	 * 
	 * @return
	 */
	boolean isClearlyId();

	boolean isSingleId();

	/**
	 * Java type mapping to jdbc type.
	 * 
	 * @return
	 */
	JdbcType getJdbcType();

	Class<? extends TypeHandler> getSpecifiedTypeHandler();

	MyBatisPersistentEntity<?> getOwnerEntity();

}
