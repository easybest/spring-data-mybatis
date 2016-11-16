package org.springframework.data.mybatis.repository.support;

import org.springframework.data.domain.Persistable;
import org.springframework.data.mybatis.repository.query.DefaultMybatisEntityMetadata;
import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by songjiawei on 2016/11/9.
 */
public abstract class MybatisEntityInformationSupport<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> implements MybatisEntityInformation<T, ID> {

    private   MybatisEntityMetadata<T> metadata;
    protected MybatisEntityModel       model;

    private static Map<Class<?>, MybatisEntityInformationSupport> cache = new HashMap<Class<?>, MybatisEntityInformationSupport>();

    /**
     * Creates a new {@link AbstractEntityInformation} from the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     */
    public MybatisEntityInformationSupport(Class<T> domainClass) {
        super(domainClass);
        this.metadata = new DefaultMybatisEntityMetadata<T>(domainClass);
        this.model = new MybatisEntityModel(domainClass);
    }

    public MybatisEntityModel getModel() {
        return model;
    }

    @Override
    public String getEntityName() {
        return metadata.getEntityName();
    }

    public static <T> MybatisEntityInformationSupport<T, ?> getEntityInformation(Class<T> domainClass) {
        Assert.notNull(domainClass);

        MybatisEntityInformationSupport information = cache.get(domainClass);
        if (null == information) {
            if (Persistable.class.isAssignableFrom(domainClass)) {
                information = new MybatisPersistableEntityInformation(domainClass);
            } else {
                information = new MybatisMetamodelEntityInformation(domainClass);
            }

            cache.put(domainClass, information);
        }
        return information;


    }
}
