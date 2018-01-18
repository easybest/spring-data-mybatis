package org.springframework.data.mybatis.mapping;

import org.apache.ibatis.type.JdbcType;
import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Jarvis Song
 */
public interface MyBatisPersistentProperty extends PersistentProperty<MyBatisPersistentProperty> {

	String getColumnName();

	boolean isCompositeId();

	JdbcType getJdbcType();

}
