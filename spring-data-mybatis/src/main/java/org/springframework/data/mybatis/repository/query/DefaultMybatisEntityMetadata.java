package org.springframework.data.mybatis.repository.query;

import javax.persistence.Entity;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class DefaultMybatisEntityMetadata<T> implements MybatisEntityMetadata<T> {

	private final Class<T> domainType;

	public DefaultMybatisEntityMetadata(Class<T> domainType) {

		Assert.notNull(domainType, "Domain type must not be null!");

		this.domainType = domainType;
	}

	@Override
	public String getEntityName() {
		Entity entity = AnnotatedElementUtils.findMergedAnnotation(domainType,
				Entity.class);
		return null != entity && StringUtils.hasText(entity.name()) ? entity.name()
				: domainType.getSimpleName();
	}

	@Override
	public Class<T> getJavaType() {
		return domainType;
	}

}
