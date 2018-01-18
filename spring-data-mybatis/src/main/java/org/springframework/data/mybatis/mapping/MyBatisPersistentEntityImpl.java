package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mybatis.annotation.Entity;
import org.springframework.data.util.ParsingUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

import java.util.Comparator;

class MyBatisPersistentEntityImpl<T> extends BasicPersistentEntity<T, MyBatisPersistentProperty>
		implements MyBatisPersistentEntity<T> {

	public MyBatisPersistentEntityImpl(TypeInformation<T> information) {
		super(information);
	}

	public MyBatisPersistentEntityImpl(TypeInformation<T> information, Comparator<MyBatisPersistentProperty> comparator) {
		super(information, comparator);
	}

	@Override
	public String getTableName() {
		String tableName;
		Entity entity = getType().getAnnotation(Entity.class);
		if (null != entity && StringUtils.hasText(entity.table())) {
			tableName = entity.table();
		} else {
			tableName = ParsingUtils.reconcatenateCamelCase(getType().getSimpleName(), "_");
		}
		if (null != entity && StringUtils.hasText(entity.schema())) {
			tableName = entity.schema() + "." + tableName;
		}

		return tableName;
	}
}
