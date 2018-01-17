package org.springframework.data.mybatis.repository.support;

import org.springframework.data.domain.Persistable;
import org.springframework.data.mybatis.repository.query.DefaultMyBatisEntityMetadata;
import org.springframework.data.mybatis.repository.query.MyBatisEntityMetadata;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

/**
 * @author Jarvis Song
 */
public abstract class MyBatisEntityInformationSupport<T, ID> extends AbstractEntityInformation<T, ID>
		implements MyBatisEntityInformation<T, ID> {

	private MyBatisEntityMetadata<T> metadata;

	public MyBatisEntityInformationSupport(Class<T> domainClass) {
		super(domainClass);
		this.metadata = new DefaultMyBatisEntityMetadata<>(domainClass);
	}

	@Override
	public String getEntityName() {
		return metadata.getEntityName();
	}

	public static <T> MyBatisEntityInformation<T, ?> getEntityInformation(Class<T> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		if (Persistable.class.isAssignableFrom(domainClass)) {
			return new MyBatisPersistableEntityInformation(domainClass);
		}

		return new MyBatisReflectionEntityInformation<>(domainClass);

	}
}
