package org.springframework.data.mybatis.repository.support;

import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Persistable;
import org.springframework.data.repository.core.support.PersistableEntityInformation;
import org.springframework.lang.Nullable;

/**
 * Extension of {@link PersistableEntityInformation} that consideres methods of {@link Persistable} to lookup the id.
 * 
 * @author Jarvis Song
 */
public class MyBatisPersistableEntityInformation<T extends Persistable<ID>, ID>
		extends MyBatisEntityInformationSupport<T, ID> {

	private Class<ID> idClass;

	/**
	 * Creates a new {@link PersistableEntityInformation}.
	 *
	 * @param domainClass
	 */
	public MyBatisPersistableEntityInformation(Class<T> domainClass) {

		super(domainClass);

		Class<?> idClass = ResolvableType.forClass(Persistable.class, domainClass).resolveGeneric(0);

		if (idClass == null) {
			throw new IllegalArgumentException(String.format("Could not resolve identifier type for %s!", domainClass));
		}

		this.idClass = (Class<ID>) idClass;
	}

	@Override
	public boolean isNew(T entity) {
		return entity.isNew();
	}

	@Override
	@Nullable
	public ID getId(T entity) {
		return entity.getId();
	}

	@Override
	public Class<ID> getIdType() {
		return this.idClass;
	}
}
