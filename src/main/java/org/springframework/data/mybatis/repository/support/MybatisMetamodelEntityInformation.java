package org.springframework.data.mybatis.repository.support;

import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.ReflectionUtils;

import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * Created by songjiawei on 2016/11/9.
 */
public class MybatisMetamodelEntityInformation<T, ID extends Serializable> extends MybatisEntityInformationSupport<T, ID> {


    /**
     * Creates a new {@link AbstractEntityInformation} from the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     */
    public MybatisMetamodelEntityInformation(Class<T> domainClass) {
        super(domainClass);
    }

    @Override
    public SingularAttribute<? super T, ?> getIdAttribute() {
        return null;
    }

    @Override
    public boolean hasCompositeId() {
        return model.getPrimaryKeys().size() > 1;
    }

    @Override
    public Iterable<String> getIdAttributeNames() {
        return null;
    }

    @Override
    public Object getCompositeIdAttributeValue(Serializable id, String idAttribute) {
        return null;
    }

    @Override
    public ID getId(T entity) {
        MybatisEntityModel primaryKey = model.getPrimaryKey();
        if (null == primaryKey) {
            return null;
        }

        Field field = ReflectionUtils.findField(entity.getClass(), primaryKey.getName());
        if (null == field) {
            return null;
        }

        return (ID) ReflectionUtils.getField(field, entity);

    }

    @Override
    public Class<ID> getIdType() {
        MybatisEntityModel primaryKey = model.getPrimaryKey();
        if (null == primaryKey) {
            return null;
        }
        return (Class<ID>) primaryKey.getClz();
    }
}
