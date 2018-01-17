package org.springframework.data.mybatis.repository.query;

import org.springframework.data.repository.core.EntityMetadata;

/**
 * @author Jarvis Song
 */
public interface MyBatisEntityMetadata<T> extends EntityMetadata<T> {

	/**
	 * Returns the name of the entity.
	 *
	 * @return
	 */
	String getEntityName();

}
