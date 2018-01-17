package org.springframework.data.mybatis.repository.support;

import org.springframework.data.mybatis.repository.query.MyBatisEntityMetadata;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Extension of {@link EntityInformation} to capture additional MyBatis specific information about entities.
 * 
 * @author Jarvis Song
 */
public interface MyBatisEntityInformation<T, ID> extends EntityInformation<T, ID>, MyBatisEntityMetadata<T> {

}
