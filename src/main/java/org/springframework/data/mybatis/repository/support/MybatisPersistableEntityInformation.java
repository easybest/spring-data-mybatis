package org.springframework.data.mybatis.repository.support;

import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

import java.io.Serializable;

/**
 * Created by songjiawei on 2016/11/9.
 */
public class MybatisPersistableEntityInformation<T extends Persistable<ID>, ID extends Serializable> extends MybatisMetamodelEntityInformation<T, ID> {
    /**
     * Creates a new {@link AbstractEntityInformation} from the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     */
    public MybatisPersistableEntityInformation(Class<T> domainClass) {
        super(domainClass);
    }


    @Override
    public boolean isNew(T entity) {
        return entity.isNew();
    }

    @Override
    public ID getId(T entity) {
        return entity.getId();
    }
}
