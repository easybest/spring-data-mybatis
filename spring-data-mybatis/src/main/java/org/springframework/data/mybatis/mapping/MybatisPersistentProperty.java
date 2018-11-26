package org.springframework.data.mybatis.mapping;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.data.mapping.PersistentProperty;

public interface MybatisPersistentProperty
		extends PersistentProperty<MybatisPersistentProperty> {

	/**
	 * Return whether the property is considered embeddable.
	 * @return
	 */
	boolean isEmbeddable();

	String getColumnName();

	JdbcType getJdbcType();

	Class<? extends TypeHandler> getSpecifiedTypeHandler();

}
