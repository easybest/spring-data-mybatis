package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.PersistentProperty;

/**
 * @author Jarvis Song
 */
public interface MyBatisPersistentProperty extends PersistentProperty<MyBatisPersistentProperty> {

	Column getMappedColumn();

	Association getMappedAssociation();

}
