package org.springframework.data.mybatis.mapping;

import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Lob;
import javax.persistence.Temporal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.ibatis.type.JdbcType.*;

/**
 * @author Jarvis Song
 */
@Getter
@Setter
public class Column implements Cloneable {

	private static Map<Class<?>, JdbcType> javaTypesMappedToJdbcTypes = new HashMap<>();

	static {
		javaTypesMappedToJdbcTypes.put(String.class, VARCHAR);
		javaTypesMappedToJdbcTypes.put(java.math.BigDecimal.class, NUMERIC);
		javaTypesMappedToJdbcTypes.put(boolean.class, BIT);
		javaTypesMappedToJdbcTypes.put(byte.class, TINYINT);
		javaTypesMappedToJdbcTypes.put(short.class, SMALLINT);
		javaTypesMappedToJdbcTypes.put(int.class, INTEGER);
		javaTypesMappedToJdbcTypes.put(long.class, BIGINT);
		javaTypesMappedToJdbcTypes.put(float.class, REAL);
		javaTypesMappedToJdbcTypes.put(double.class, DOUBLE);
		javaTypesMappedToJdbcTypes.put(byte[].class, VARBINARY);
		javaTypesMappedToJdbcTypes.put(java.util.Date.class, TIMESTAMP);
		javaTypesMappedToJdbcTypes.put(java.sql.Date.class, DATE);
		javaTypesMappedToJdbcTypes.put(java.sql.Time.class, TIME);
		javaTypesMappedToJdbcTypes.put(java.sql.Timestamp.class, TIMESTAMP);

		javaTypesMappedToJdbcTypes.put(Boolean.class, BIT);
		javaTypesMappedToJdbcTypes.put(Integer.class, INTEGER);
		javaTypesMappedToJdbcTypes.put(Long.class, BIGINT);
		javaTypesMappedToJdbcTypes.put(Float.class, REAL);
		javaTypesMappedToJdbcTypes.put(Double.class, DOUBLE);

	}

	public static final int DEFAULT_LENGTH = 255;
	public static final int DEFAULT_PRECISION = 19;
	public static final int DEFAULT_SCALE = 2;

	protected int length = DEFAULT_LENGTH;
	protected int precision = DEFAULT_PRECISION;
	protected int scale = DEFAULT_SCALE;
	protected boolean nullable = true;
	protected boolean unique;
	protected String comment;
	protected String defaultValue;

	protected Table table;

	/**
	 * mapping property.
	 */
	protected MyBatisPersistentProperty property;
	/**
	 * column's name in database.
	 */
	protected String name;
	/**
	 * full column path.
	 */
	protected String prefix;
	/**
	 * mapping to property.
	 */
	protected String alias;

	protected javax.persistence.Column columnAnnotation;

	public Column(Table table) {
		this.table = table;
	}

	public JdbcType getJdbcType() {
		org.springframework.data.mybatis.annotation.JdbcType jdbcType = property
				.findAnnotation(org.springframework.data.mybatis.annotation.JdbcType.class);
		if (null != jdbcType) {
			return JdbcType.valueOf(jdbcType.value());
		}

		Temporal temporal = property.findAnnotation(Temporal.class);
		if (null != temporal) {
			switch (temporal.value()) {
				case DATE:
					return DATE;
				case TIME:
					return TIME;
				case TIMESTAMP:
					return TIMESTAMP;
			}
		}

		Class<?> type = property.getActualType();

		Lob lob = property.findAnnotation(Lob.class);
		if (null != lob) {
			if (type == String.class) {
				return CLOB;
			}
			return BLOB;
		}

		JdbcType t = javaTypesMappedToJdbcTypes.get(type);
		if (null != t) {
			return t;
		}

		return UNDEFINED;
	}

	public Class<? extends TypeHandler> getSpecifiedTypeHandler() {
		org.springframework.data.mybatis.annotation.TypeHandler typeHandler = property
				.findAnnotation(org.springframework.data.mybatis.annotation.TypeHandler.class);
		if (null != typeHandler && StringUtils.hasText(typeHandler.value())) {
			try {
				return (Class<? extends TypeHandler>) ClassUtils.forName(typeHandler.value(),
						ClassUtils.getDefaultClassLoader());
			} catch (ClassNotFoundException e) {
				throw new MappingException("@TypeHandler with class " + typeHandler.value() + " not found.", e);
			}

		}

		return null;
	}

	public String getActualName(Dialect d) {
		if (StringUtils.hasText(name) && Dialect.QUOTE.indexOf(name.charAt(0)) > -1) {
			return d.openQuote() + name.substring(1, name.length() - 1) + d.closeQuote();
		}
		return name;
	}

	public String getQuotedPrefix(Dialect d) {
		return d.openQuote() + getPrefix() + d.closeQuote();
	}

	public String getQuotedAlias(Dialect d) {
		return d.openQuote() + getAlias() + d.closeQuote();
	}

	public String getQueryColumnName(boolean complex, Dialect dialect) {
		if (complex) {
			return getQuotedPrefix(dialect) + '.' + getActualName(dialect);
		}
		return getActualName(dialect);
	}

	public void setColumnAnnotation(javax.persistence.Column ca) {
		if (null != ca) {
			length = ca.length();
			if (ca.precision() > 0) {
				precision = ca.precision();
			}
			if (ca.scale() > 0) {
				scale = ca.scale();
			}
			nullable = ca.nullable();
			unique = ca.unique();
		}

		this.columnAnnotation = ca;
	}

	public Class<?> getType() {
		if (this instanceof CompositeIdColumn) {
			return ((CompositeIdColumn) this).getIdClass();
		}

		if (null != property) {
			return property.getActualType();
		}

		return null;
	}

	@Override
	public Column clone() {
		Column copy = new Column(this.table);
		BeanUtils.copyProperties(this, copy);
		return copy;
	}

	@Override
	public String toString() {
		return "Column{" + "name='" + name + '\'' + ", prefix='" + prefix + '\'' + ", alias='" + alias + '\'' + '}';
	}
}
