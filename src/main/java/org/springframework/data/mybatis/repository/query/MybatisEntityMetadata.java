package org.springframework.data.mybatis.repository.query;

import org.springframework.data.repository.core.EntityMetadata;

/**
 * Created by songjiawei on 2016/11/9.
 */
public interface MybatisEntityMetadata<T> extends EntityMetadata<T> {
    /**
     * Returns the name of the entity.
     *
     * @return
     */
    String getEntityName();
}
