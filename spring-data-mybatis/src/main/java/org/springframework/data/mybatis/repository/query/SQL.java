package org.springframework.data.mybatis.repository.query;

import org.apache.ibatis.jdbc.AbstractSQL;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MyBatisPersistentEntityImpl;
import org.springframework.data.mybatis.mapping.MyBatisPersistentProperty;
import org.springframework.util.StringUtils;

import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jarvis Song
 */
public class SQL extends AbstractSQL<SQL> {

	protected final MyBatisMappingContext context;
	protected final Dialect dialect;
	protected final MyBatisPersistentEntity<?> entity;

	public SQL(Dialect dialect, MyBatisMappingContext context, MyBatisPersistentEntity<?> entity) {
		this.context = context;
		this.dialect = dialect;
		this.entity = entity;
	}

	@Override
	public SQL getSelf() {
		return this;
	}

	public String[] toStrings() {
		return new String[] { toString() };
	}

	public String VAR(MyBatisPersistentProperty property) {
		return VAR(property.getName(), property.getJdbcType(), property.getSpecifiedTypeHandler());
	}

	public String VAR(String name) {

		return VAR(name, null, null);
	}

	public String VAR(String name, JdbcType jdbcType) {

		return VAR(name, jdbcType, null);
	}

	public String VAR(String name, JdbcType jdbcType, Class<? extends TypeHandler> typeHandler) {

		StringBuilder builder = new StringBuilder("#{");

		builder.append(name);
		if (null != jdbcType) {
			builder.append(",jdbcType=").append(jdbcType);
		}
		if (null != typeHandler) {
			builder.append(",typeHandler=").append(typeHandler.getName());
		}
		builder.append("}");

		return builder.toString();

	}

	public String TABLE_NAME() {
		return dialect.quote(entity.getTableName());
	}

	public String ALIAS() {
		return dialect.quote(entity.getEntityName());
	}

	public String COLUMN(MyBatisPersistentProperty property) {
		return dialect.quote(property.getColumnName());
	}

	public String COLUMN(String name) {
		return dialect.quote(name);
	}

	public String[] COLUMNS(boolean complex) {
		Set<String> columns = new HashSet<>();
		entity.doWithProperties((SimplePropertyHandler) pp -> {
			MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
			columns.add(ALIAS() + '.' + COLUMN(property) + " as " + dialect.quote(property.getName()));
		});
		entity.doWithAssociations((SimpleAssociationHandler) association -> {
			MyBatisPersistentProperty inverseProperty = (MyBatisPersistentProperty) association.getInverse();
			if (inverseProperty.isIdProperty()) {
				if (inverseProperty.isAnnotationPresent(ManyToOne.class)
						|| inverseProperty.isAnnotationPresent(OneToOne.class)) {
					// TODO
				} else {
					MyBatisPersistentEntityImpl<?> assEntity = context.getPersistentEntity(inverseProperty.getActualType());
					entity.doWithProperties((SimplePropertyHandler) pp -> {
						MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;
						columns.add(dialect.quote(entity.getEntityName()) + "." + COLUMN(property) + " as "
								+ dialect.quote(entity.getEntityName() + "." + assEntity.getEntityName() + "." + property.getName()));
					});
				}
			}
			// TODO REAL ASSOCIATION
			if (complex) {} else {}
		});
		return columns.toArray(new String[columns.size()]);
	}

	public SQL SELECT_WITH_COLUMNS(boolean complex) {

		SELECT(COLUMNS(complex));

		return getSelf();
	}

	public SQL FROM_WITH_LEFT_OUTER_JOIN(boolean complex) {

		FROM(entity.getTableName() + " " + dialect.quote(entity.getEntityName()));

		if (complex) {
			// TODO LEFT OUTER JOIN
		}
		return getSelf();
	}

	public SQL ID_CALUSE() {
		return ID_CALUSE(true);
	}

	public SQL ID_CALUSE(boolean alias) {
		MyBatisPersistentProperty idProperty = entity.getIdProperty();
		// process id cause
		if (entity.hasCompositeIdProperty()) {
			if (entity.isAnnotationPresent(IdClass.class)) {
				entity.doWithIdProperties(pp -> {

					MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;

					WHERE((alias ? (ALIAS() + ".") : "") + COLUMN(idProperty) + "=" + VAR(
							context.getPersistentEntity(entity.getCompositeIdClass()).getEntityName() + '.' + property.getName()));
				});
			} else {
				MyBatisPersistentEntityImpl<?> idEntity = context.getPersistentEntity(idProperty.getActualType());
				idEntity.doWithProperties((SimplePropertyHandler) pp -> {

					MyBatisPersistentProperty property = (MyBatisPersistentProperty) pp;

					WHERE((alias ? (ALIAS() + ".") : "") + COLUMN(idProperty) + "="
							+ VAR(idProperty.getName() + '.' + property.getName()));
				});
			}

		} else {
			WHERE((alias ? (ALIAS() + ".") : "") + COLUMN(idProperty) + "=" + VAR(idProperty.getName()));
		}
		return getSelf();
	}

	public SQL ORDER_BY(boolean complex, Sort sort) {
		if (null != sort && sort.isSorted()) {
			String[] columns = COLUMNS(complex);
			Map<String, String> map = Stream.of(columns).filter(c -> StringUtils.hasText(c)).collect(Collectors.toMap(c -> {
				String[] ss = c.split(" as ");
				String key = ss[ss.length - 1];
				key = key.replace(String.valueOf(dialect.openQuote()), "").replace(String.valueOf(dialect.closeQuote()), "");
				return key;
			}, c -> c.split(" as ")[0]));

			sort.forEach(order -> {
				String p = map.get(order.getProperty());
				ORDER_BY((StringUtils.isEmpty(p) ? order.getProperty() : p) + " " + order.getDirection().name());
			});
		}
		return getSelf();
	}

	public String SORT_SQL(boolean complex) {
		final StringBuilder builder = new StringBuilder();

		builder.append("<if test=\"_sorts != null\">");
		builder.append("<bind name=\"_columnsMap\" value='#{");
		String[] arr = COLUMNS(complex);
		Arrays.stream(arr).filter(c -> StringUtils.hasText(c)).forEach(s -> {
			String[] ss = s.split(" as ");
			String key = ss[ss.length - 1];
			String val = ss[0];
			key = key.replace(String.valueOf(dialect.openQuote()), "").replace(String.valueOf(dialect.closeQuote()), "");
			val = val.replace("\"", "\\\"");
			builder.append(String.format("\"%s\":\"%s\",", key, val));
		});

		if (builder.length() > 0 && builder.charAt(builder.length() - 1) == ',') {
			builder.deleteCharAt(builder.length() - 1);
		}
		builder.append("}' />");
		builder.append(" order by ");
		builder.append("<foreach item=\"item\" index=\"idx\" collection=\"_sorts\" open=\"\" separator=\",\" close=\"\">");
		builder.append("<if test=\"item.ignoreCase\">" + dialect.getLowercaseFunction() + "(</if>")
				.append("${_columnsMap[item.property]}").append("<if test=\"item.ignoreCase\">)</if>")
				.append(" ${item.direction}");
		builder.append("</foreach>");
		builder.append("</if>");

		return builder.toString();
	}

}
