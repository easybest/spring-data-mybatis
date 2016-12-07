/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.springframework.data.mybatis.mapping;

import org.apache.ibatis.type.JdbcType;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mybatis.annotations.*;
import org.springframework.data.mybatis.annotations.Id.GenerationType;
import org.springframework.data.util.ParsingUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jarvis Song
 */
class MybatisPersistentPropertyImpl extends AnnotationBasedPersistentProperty<MybatisPersistentProperty>
        implements MybatisPersistentProperty {

    private static Map<Class<?>, JdbcType> javaTypesMappedToJdbcTypes = new HashMap<Class<?>, JdbcType>();

    static {
        javaTypesMappedToJdbcTypes.put(String.class, JdbcType.VARCHAR);
        javaTypesMappedToJdbcTypes.put(java.math.BigDecimal.class, JdbcType.NUMERIC);
        javaTypesMappedToJdbcTypes.put(boolean.class, JdbcType.BIT);
        javaTypesMappedToJdbcTypes.put(byte.class, JdbcType.TINYINT);
        javaTypesMappedToJdbcTypes.put(short.class, JdbcType.SMALLINT);
        javaTypesMappedToJdbcTypes.put(int.class, JdbcType.INTEGER);
        javaTypesMappedToJdbcTypes.put(long.class, JdbcType.BIGINT);
        javaTypesMappedToJdbcTypes.put(float.class, JdbcType.REAL);
        javaTypesMappedToJdbcTypes.put(double.class, JdbcType.DOUBLE);
        javaTypesMappedToJdbcTypes.put(byte[].class, JdbcType.VARBINARY);
        javaTypesMappedToJdbcTypes.put(java.util.Date.class, JdbcType.TIMESTAMP);
        javaTypesMappedToJdbcTypes.put(java.sql.Date.class, JdbcType.DATE);
        javaTypesMappedToJdbcTypes.put(java.sql.Time.class, JdbcType.TIME);
        javaTypesMappedToJdbcTypes.put(java.sql.Timestamp.class, JdbcType.TIMESTAMP);

        javaTypesMappedToJdbcTypes.put(Boolean.class, JdbcType.BIT);
        javaTypesMappedToJdbcTypes.put(Integer.class, JdbcType.INTEGER);
        javaTypesMappedToJdbcTypes.put(Long.class, JdbcType.BIGINT);
        javaTypesMappedToJdbcTypes.put(Float.class, JdbcType.REAL);
        javaTypesMappedToJdbcTypes.put(Double.class, JdbcType.DOUBLE);


    }


    /**
     * Creates a new {@link AnnotationBasedPersistentProperty}.
     *
     * @param field              must not be {@literal null}.
     * @param propertyDescriptor can be {@literal null}.
     * @param owner              must not be {@literal null}.
     * @param simpleTypeHolder
     */
    public MybatisPersistentPropertyImpl(Field field,
                                         PropertyDescriptor propertyDescriptor,
                                         PersistentEntity<?, MybatisPersistentProperty> owner,
                                         SimpleTypeHolder simpleTypeHolder) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);
    }

    @Override
    protected Association<MybatisPersistentProperty> createAssociation() {
        if (null != findAnnotation(Embedded.class)) {
            return new MybatisEmbeddedAssociation(this, null);
        }

        if (null != findAnnotation(ManyToOne.class)) {
            return new MybatisManyToOneAssociation(this, null);
        }
        if (null != findAnnotation(OneToOne.class)) {
            return new MybatisOneToOneAssociation(this, null);
        }

        return new MybatisAssociation(this, null);

    }


    /**
     * Java Type	JDBC type
     * String	VARCHAR or LONGVARCHAR
     * java.math.BigDecimal	NUMERIC
     * boolean	BIT
     * byte	TINYINT
     * short	SMALLINT
     * int	INTEGER
     * long	BIGINT
     * float	REAL
     * double	DOUBLE
     * byte[]	VARBINARY or LONGVARBINARY
     * java.sql.Date	DATE
     * java.sql.Time	TIME
     * java.sql.Timestamp	TIMESTAMP
     * ----------------------------------------
     * Java Object Type	JDBC Type
     * String	VARCHAR or LONGVARCHAR
     * java.math.BigDecimal	NUMERIC
     * Boolean	BIT
     * Integer	INTEGER
     * Long	BIGINT
     * Float	REAL
     * Double	DOUBLE
     * byte[]	VARBINARY or LONGVARBINARY
     * java.sql.Date	DATE
     * java.sql.Time	TIME
     * java.sql.Timestamp	TIMESTAMP
     *
     * @return
     */
    @Override
    public JdbcType getJdbcType() {

        org.springframework.data.mybatis.annotations.JdbcType jdbcType = findAnnotation(org.springframework.data.mybatis.annotations.JdbcType.class);
        if (null != jdbcType) {
            return jdbcType.value();
        }

        Class<?> type = getActualType();

        JdbcType t = javaTypesMappedToJdbcTypes.get(type);
        if (null != t) {
            return t;
        }

        return JdbcType.UNDEFINED;
    }

    @Override
    public String getColumnName() {

        Column column = findAnnotation(Column.class);
        if (null != column && StringUtils.hasText(column.name())) {
            return column.name();
        }

        return ParsingUtils.reconcatenateCamelCase(getName(), "_");
    }

    @Override
    public boolean isToOneAssociation() {
        if (!isAssociation()) {
            return false;
        }
        return (null != findAnnotation(Embedded.class) || null != findAnnotation(ManyToOne.class) || null != findAnnotation(OneToOne.class));
    }

    @Override
    public boolean isCompositeId() {
        return isIdProperty() && isEntity();
    }

    @Override
    public GenerationType getIdGenerationType() {
        if (!isIdProperty()) {
            return null;
        }
        Id id = findAnnotation(Id.class);
        if (null == id) {
            return null;
        }
        return id.strategy();
    }
}
