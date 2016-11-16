package org.springframework.data.mybatis.repository.query;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.Entity;

/**
 * Created by songjiawei on 2016/11/9.
 */
public class DefaultMybatisEntityMetadata<T> implements MybatisEntityMetadata<T> {

    private final Class<T> domainType;

    public DefaultMybatisEntityMetadata(Class<T> domainType) {
        Assert.notNull(domainType, "Domain type must not be null!");
        this.domainType = domainType;
    }

    @Override
    public String getEntityName() {
        Entity entity = AnnotatedElementUtils.findMergedAnnotation(domainType, Entity.class);
        boolean hasName = null != entity && StringUtils.hasText(entity.name());

        return hasName ? entity.name() : domainType.getSimpleName();
    }

    @Override
    public Class<T> getJavaType() {
        return domainType;
    }
}
