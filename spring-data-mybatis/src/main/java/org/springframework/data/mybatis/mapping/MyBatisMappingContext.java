package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author Jarvis Song
 */
public class MyBatisMappingContext
		extends AbstractMappingContext<MyBatisPersistentEntityImpl<?>, MyBatisPersistentProperty> {

	@Override
	protected <T> MyBatisPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new MyBatisPersistentEntityImpl<>(this, typeInformation, new ResultMapComparator());
	}

	@Override
	protected MyBatisPersistentProperty createPersistentProperty(Property property, MyBatisPersistentEntityImpl<?> owner,
			SimpleTypeHolder simpleTypeHolder) {
		return new MyBatisPersistentPropertyImpl(property, owner, simpleTypeHolder);
	}

	@Override
	public void initialize() {
		super.initialize();

		getPersistentEntities().forEach(entity -> entity.initTable());

	}

	private static final class ResultMapComparator implements Comparator<MyBatisPersistentProperty>, Serializable {

		@Override
		public int compare(MyBatisPersistentProperty o1, MyBatisPersistentProperty o2) {

			if (o1.isIdProperty() && o2.isIdProperty()) {
				return 0;
			}
			if (o1.isIdProperty() && !o2.isIdProperty()) {
				return -1;
			}
			if (!o1.isIdProperty() && o2.isIdProperty()) {
				return 1;
			}

			if (o1.isAssociation() && !o2.isAssociation()) {
				return 1;
			}
			if (!o1.isAssociation() && o2.isAssociation()) {
				return -1;
			}

			return o1.getName().compareTo(o2.getName());
		}
	}

}
