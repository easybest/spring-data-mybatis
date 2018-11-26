package org.springframework.data.mybatis.repository.support;

import org.springframework.data.domain.Persistable;
import org.springframework.data.mybatis.repository.query.DefaultMybatisEntityMetadata;
import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

public abstract class MybatisEntityInformationSupport<T, ID> extends
		AbstractEntityInformation<T, ID> implements MybatisEntityInformation<T, ID> {

	private MybatisEntityMetadata<T> metadata;

	public MybatisEntityInformationSupport(Class<T> domainClass) {
		super(domainClass);

		this.metadata = new DefaultMybatisEntityMetadata<>(domainClass);
	}

	@Override
	public String getEntityName() {
		return metadata.getEntityName();
	}

	public static <T> MybatisEntityInformation<T, ?> getEntityInformation(
			Class<T> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		if (Persistable.class.isAssignableFrom(domainClass)) {
			return new MybatisPersistableEntityInformation(domainClass);
		}

		return new MybatisReflectionEntityInformation<>(domainClass);
	}

}
