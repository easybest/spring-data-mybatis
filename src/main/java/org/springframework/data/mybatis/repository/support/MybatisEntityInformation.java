package org.springframework.data.mybatis.repository.support;

import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.EntityInformation;

import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;

/**
 * Created by songjiawei on 2016/11/9.
 */
public interface MybatisEntityInformation<T, ID extends Serializable> extends EntityInformation<T, ID>, MybatisEntityMetadata<T> {

    /**
     * Returns the id attribute of the entity.
     *
     * @return
     */
    SingularAttribute<? super T, ?> getIdAttribute();

    /**
     * Returns {@literal true} if the entity has a composite id.
     *
     * @return
     */
    boolean hasCompositeId();

    /**
     * Returns the attribute names of the id attributes. If the entity has a composite id, then all id attribute names are
     * returned. If the entity has a single id attribute then this single attribute name is returned.
     *
     * @return
     */
    Iterable<String> getIdAttributeNames();

    /**
     * Extracts the value for the given id attribute from a composite id
     *
     * @param id
     * @param idAttribute
     * @return
     */
    Object getCompositeIdAttributeValue(Serializable id, String idAttribute);

}
