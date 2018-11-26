package org.springframework.data.mybatis.repository.support;

import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Persistable;

public class MybatisPersistableEntityInformation<T extends Persistable<ID>, ID>
		extends MybatisEntityInformationSupport<T, ID> {

	private Class<ID> idClass;

	public MybatisPersistableEntityInformation(Class<T> domainClass) {
		super(domainClass);

		Class<?> idClass = ResolvableType.forClass(Persistable.class, domainClass)
				.resolveGeneric(0);

		if (null == idClass) {
			throw new IllegalArgumentException(String
					.format("Could not resolve identifier type for %s!", domainClass));
		}

		this.idClass = (Class<ID>) idClass;
	}

	@Override
	public ID getId(T entity) {
		return entity.getId();
	}

	@Override
	public Class<ID> getIdType() {
		return idClass;
	}

	@Override
	public boolean isNew(T entity) {
		return entity.isNew();
	}

}
