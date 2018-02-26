package org.springframework.data.mybatis.mapping;

import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.ParsingUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.Comparator;

/**
 * @author Jarvis Song
 */
public class MyBatisPersistentEntityImpl<T> extends BasicPersistentEntity<T, MyBatisPersistentProperty>
		implements MyBatisPersistentEntity<T> {

	private final MyBatisMappingContext mappingContext;

	public MyBatisPersistentEntityImpl(MyBatisMappingContext context, TypeInformation<T> information) {
		this(context, information, null);
	}

	public MyBatisPersistentEntityImpl(MyBatisMappingContext context, TypeInformation<T> information,
			Comparator<MyBatisPersistentProperty> comparator) {
		super(information, comparator);

		mappingContext = context;
	}

	@Override
	public MyBatisMappingContext getMappingContext() {
		return mappingContext;
	}

	@Override
	public String getTableName() {
		String tableName;
		Table table = getType().getAnnotation(Table.class);

		if (null != table && StringUtils.hasText(table.name())) {
			tableName = table.name();
		} else {
			tableName = ParsingUtils.reconcatenateCamelCase(getType().getSimpleName(), "_");
		}

		if (null != table && StringUtils.hasText(table.schema())) {
			tableName = table.schema() + "." + tableName;
		}

		return tableName;
	}

	/**
	 * 1 将组件类注解为@Embeddable,并将组件的属性注解为@Id <br/>
	 * 2 将组件的属性注解为@EmbeddedId (方便) <br/>
	 * 3 将类注解为@IdClass,并将该实体中所有属于主键的属性都注解为@Id<br/>
	 *
	 * @return
	 */
	@Override
	public boolean hasCompositeIdProperty() {

		if (hasIdProperty() && getIdProperty().isCompositeId()) {
			return true;
		}

		if (isAnnotationPresent(IdClass.class)) {
			return true;
		}

		return false;
	}

	@Override
	public Class<?> getCompositeIdClass() {
		IdClass idClass = findAnnotation(IdClass.class);
		if (null != idClass) {
			return idClass.value();
		}

		if (!hasIdProperty()) {
			return null;
		}

		MyBatisPersistentProperty idProperty = getIdProperty();
		if (!idProperty.isCompositeId()) {
			return null;
		}
		if (idProperty.isAnnotationPresent(EmbeddedId.class)) {
			return idProperty.getActualType();
		}

		if (idProperty.isAssociation() && idProperty.getAssociation().getInverse().isAnnotationPresent(Embeddable.class)) {
			return idProperty.getActualType();
		}

		return null;
	}

	@Override
	public MyBatisPersistentEntity<?> getCompositeIdPersistentEntity() {
		Class<?> compositeIdClass = getCompositeIdClass();
		if (null == compositeIdClass) {
			return null;
		}
		return mappingContext.getPersistentEntity(compositeIdClass);
	}

	@Override
	public Class<?> getIdClass() {
		IdClass idClass = findAnnotation(IdClass.class);
		if (null != idClass) {
			return idClass.value();
		}

		MyBatisPersistentProperty idProperty = getIdProperty();
		if (null == idProperty) {
			return null;
		}

		return idProperty.getActualType();
	}

	@Override
	public String getEntityName() {
		Entity entity = findAnnotation(Entity.class);
		if (null != entity && StringUtils.hasText(entity.name())) {
			return entity.name();
		}
		return StringUtils.uncapitalize(getType().getSimpleName());
	}

	@Override
	protected MyBatisPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(
			MyBatisPersistentProperty property) {

		return property.isIdProperty() ? property : null;
	}

	@Override
	public void doWithIdProperties(SimplePropertyHandler handler) {
		doWithProperties((SimplePropertyHandler) pp -> {
			if (pp.isIdProperty()) {
				handler.doWithPersistentProperty(pp);
			}
		});
		doWithAssociations((SimpleAssociationHandler) as -> {
			if (as.getInverse().isIdProperty()) {
				handler.doWithPersistentProperty(as.getInverse());
			}
		});
	}
}
