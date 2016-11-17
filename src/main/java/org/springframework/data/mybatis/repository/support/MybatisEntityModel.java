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

package org.springframework.data.mybatis.repository.support;


import org.apache.ibatis.type.JdbcType;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.data.annotations.ComplexSearch;
import org.springframework.data.annotations.Searchable;
import org.springframework.data.mybatis.repository.util.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * entity model.
 *
 * @author Jarvis Song
 */
public class MybatisEntityModel {

    private Class<?> clz;
    private String   name; // the name in java, maybe entity's name or property's name.
    private String   nameInDatabase; // the name in database, maybe table's name or column's name.
    private String   jdbcType;

    private MybatisEntityModel parent; // parent entity. if this model is column, it's parent will be entity.
    private Map<String, MybatisEntityModel> columns      = new LinkedHashMap<String, MybatisEntityModel>(); // property => column
    private Map<String, MybatisEntityModel> primaryKeys  = new LinkedHashMap<String, MybatisEntityModel>();
    private Map<String, MybatisEntityModel> oneToOnes    = new LinkedHashMap<String, MybatisEntityModel>();
    private Map<String, MybatisEntityModel> manyToOnes   = new LinkedHashMap<String, MybatisEntityModel>();
    private Map<String, MybatisEntityModel> oneToManys   = new LinkedHashMap<String, MybatisEntityModel>();
    private Map<String, MybatisEntityModel> manyToManys  = new LinkedHashMap<String, MybatisEntityModel>();
    private Map<String, MybatisEntityModel> joinColumns  = new LinkedHashMap<String, MybatisEntityModel>(); //BASIC use join columns
    private List<SearchModel>               searchModels = new ArrayList<SearchModel>();

    private MybatisEntityModel primaryKey;

    /* JOIN COLUMN INFO */
    private String joinColumnName;
    private String joinReferencedColumnName;
    private String joinReferencedName;

    private boolean generatedValue = false;
    private boolean compositeId    = false;

    public MybatisEntityModel(Class<?> domainClass) {
        this(null, domainClass, true);
    }

    /**
     * Create a column.
     *
     * @param parent
     * @param name
     * @param nameInDatabase
     */
    public MybatisEntityModel(MybatisEntityModel parent, String name, String nameInDatabase) {
        this.parent = parent;
        this.name = name;
        this.nameInDatabase = nameInDatabase;
    }

    protected static MybatisEntityModel createSimpleColumn(MybatisEntityModel entity, String propertyName, String columnName) {
        return new MybatisEntityModel(entity, propertyName, columnName);
    }

    public MybatisEntityModel(MybatisEntityModel parent, Class<?> domainClass, boolean includeRelation) {
        this.parent = parent;

        this.clz = domainClass;
        this.name = classToEntityName(domainClass);
        this.nameInDatabase = classToTableName(domainClass);


        // foreach property as column
        PropertyDescriptor[] properties = ReflectUtils.getBeanProperties(domainClass); // must readable and writable.
        if (null == properties || properties.length == 0) {
            return;
        }

        for (PropertyDescriptor propertyDescriptor : properties) {
            if (hasAnnotation(propertyDescriptor, Transient.class) || hasAnnotation(propertyDescriptor, org.springframework.data.annotation.Transient.class)) {
                continue; // transient
            }

            String pName = propertyDescriptor.getName();
            Method pMethod = propertyDescriptor.getReadMethod();
            Field pField = ReflectionUtils.findField(domainClass, pName);

            Class<?> type = getType(propertyDescriptor, pMethod, pField);
            String propertyName = getPropertyName(propertyDescriptor, pMethod, pField);
            String columnName;
            Column columnAnn = getAnnotation(pMethod, pField, Column.class);
            if (null == columnAnn || StringUtils.isEmpty(columnAnn.name())) {
                columnName = StringUtils.camelToUnderline(propertyName).toUpperCase();
            } else {
                columnName = columnAnn.name();
            }

            MybatisEntityModel column = createSimpleColumn(this, propertyName, columnName);
            column.setClz(type);

            // Search
            List<Searchable> searchables = new ArrayList<Searchable>();
            ComplexSearch complexSearch = getAnnotation(pMethod, pField, ComplexSearch.class);
            if (null != complexSearch && null != complexSearch.value() && complexSearch.value().length > 0) {
                for (Searchable searchable : complexSearch.value()) {
                    searchables.add(searchable);
                }
            }
            Searchable searchable = getAnnotation(pMethod, pField, Searchable.class);
            if (null != searchable) {
                searchables.add(searchable);
            }
            if (!searchables.isEmpty()) {
                for (Searchable search : searchables) {
                    SearchModel sm = new SearchModel();
                    sm.setPropertyName(StringUtils.isNotEmpty(search.properyName()) ? search.properyName() : propertyName);
                    sm.setColumnName(StringUtils.isNotEmpty(search.columnName()) ? search.columnName() : columnName);
                    sm.setOperate(search.operate().name());
                    sm.setOper(search.operate().getOper());
                    if (StringUtils.isNotEmpty(search.alias())) {
                        sm.setAlias(search.alias());
                    }
                    searchModels.add(sm);
                }
            }

            // ManyToOne
            if (hasAnnotation(propertyDescriptor, ManyToOne.class)) {
                MybatisEntityModel target = new MybatisEntityModel(this, type, false);

                processJoinColumnInNotIncludeRelation(pMethod, pField, propertyName, target);
                if (!includeRelation) {
                    continue;
                }
                resolveJoinColumn(pMethod, pField, target);

                manyToOnes.put(propertyName, target);
                continue;
            }

            // OneToOne
            if (hasAnnotation(propertyDescriptor, OneToOne.class)) {
                MybatisEntityModel target = new MybatisEntityModel(this, type, false);

                processJoinColumnInNotIncludeRelation(pMethod, pField, propertyName, target);

                if (!includeRelation) {
                    continue;
                }

                resolveJoinColumn(pMethod, pField, target);

                oneToOnes.put(propertyName, target);
                continue;
            }
            // OneToMany
            if (hasAnnotation(propertyDescriptor, OneToMany.class)) {
                if (!includeRelation) {
                    continue;
                }
                MybatisEntityModel target = new MybatisEntityModel(this, type, false);
                //TODO
                oneToManys.put(propertyName, target);
                continue;
            }
            // ManyToMany
            if (hasAnnotation(propertyDescriptor, ManyToMany.class)) {
                if (!includeRelation) {
                    continue;
                }

                if (Collection.class.isAssignableFrom(type)) {
                    // TODO
                }

                MybatisEntityModel target = new MybatisEntityModel(this, type, false);
                manyToManys.put(propertyName, target);
                continue;
            }

            org.springframework.data.annotations.JdbcType jdbcTypeAnnotation = getAnnotation(pMethod, pField, org.springframework.data.annotations.JdbcType.class);
            if (null != jdbcTypeAnnotation && null != jdbcTypeAnnotation.value()) {
                column.setJdbcType(jdbcTypeAnnotation.value().name());
            } else {
                column.setJdbcType(getJdbcType(column.getClz()));
            }

            /**
             * primary key
             */
            if (hasAnnotation(propertyDescriptor, Id.class) || hasAnnotation(propertyDescriptor, org.springframework.data.annotation.Id.class)) {
                if (hasAnnotation(propertyDescriptor, GeneratedValue.class)) {
                    column.setGeneratedValue(true);
                }
                primaryKeys.put(propertyName, column);
                primaryKey = column;
            } else if (hasAnnotation(propertyDescriptor, EmbeddedId.class)) {
                // Composite primary key
                compositeId = true;
                primaryKey = column;
                if (!CollectionUtils.isEmpty(primaryKey.getColumns())) {
                    primaryKeys.putAll(primaryKey.getColumns());
                }

            } else {
                columns.put(propertyName, column);
            }
        }
    }

    private String getJdbcType(Class<?> jt) {
        if (null == jt) {
            return JdbcType.UNDEFINED.name();
        }
        if (jt == String.class || jt.isEnum()) {
            return JdbcType.VARCHAR.name();
        }
        if (jt == Long.class || jt == long.class
                || jt == Integer.class || jt == int.class
                || jt == Double.class || jt == double.class
                || jt == Float.class || jt == float.class
                || Number.class.isAssignableFrom(jt)
                ) {
            return JdbcType.NUMERIC.name();
        }
        if (jt == Boolean.class || jt == boolean.class) {
            return JdbcType.BOOLEAN.name();
        }
        if (jt == Date.class || Date.class.isAssignableFrom(jt)) {
            return JdbcType.TIMESTAMP.name();
        }

        throw new RuntimeException("No supported JdbcType for field :" + jt + "," + getClz());
    }

    public MybatisEntityModel findColumnByColumnName(String columnName) {
        for (Map.Entry<String, MybatisEntityModel> entry : primaryKeys.entrySet()) {
            if (columnName.equals(entry.getValue().getNameInDatabase())) {
                return entry.getValue();
            }
        }
        for (Map.Entry<String, MybatisEntityModel> entry : columns.entrySet()) {
            if (columnName.equals(entry.getValue().getNameInDatabase())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public MybatisEntityModel findColumnByPropertyName(String segment) {

        MybatisEntityModel column = primaryKeys.get(segment);
        if (null != column) {
            return column;
        }

        return columns.get(segment);
    }

    private void processJoinColumnInNotIncludeRelation(Method method, Field field, String propertyName, MybatisEntityModel dm) {
        JoinColumn joinColumn = getAnnotation(method, field, JoinColumn.class);
        if ((null == joinColumn || StringUtils.isEmpty(joinColumn.name())) && dm.getPrimaryKeys().size() > 1) {
            throw new MybatisRepositoryCreationException("when target model has more then 1 primary key , you should use @JoinColumn to assigin the join column.");
        }

        if ((null == joinColumn || StringUtils.isEmpty(joinColumn.name())) && dm.getPrimaryKeys().isEmpty()) {
            throw new MybatisRepositoryCreationException("when target model has no primary key , you should use @JoinColumn to assigin the join column.");
        }

        String joinColumnName = StringUtils.camelToUnderline(dm.getName()).toUpperCase() + "_" + dm.getPrimaryKeys().values().iterator().next().getNameInDatabase();
        String joinReferencedColumnName;
        String joinReferencedName = dm.getPrimaryKeys().values().iterator().next().getName();
        if (null != joinColumn) {
            if (StringUtils.isNotEmpty(joinColumn.name())) {
                joinColumnName = joinColumn.name();
            }
            if (StringUtils.isNotEmpty(joinColumn.referencedColumnName())) {
                joinReferencedColumnName = joinColumn.referencedColumnName();
                MybatisEntityModel referenceModel = dm.findColumnByColumnName(joinReferencedColumnName);
                if (null != referenceModel) {
                    joinReferencedName = referenceModel.getName();
                }
            }
        }
        String name = propertyName + "." + joinReferencedName;
        MybatisEntityModel column = new MybatisEntityModel(dm, name, joinColumnName);
        if (null != joinColumn && StringUtils.isNotEmpty(joinColumn.referencedColumnName())) {
            joinReferencedColumnName = joinColumn.referencedColumnName();
            MybatisEntityModel referenceModel = dm.findColumnByColumnName(joinReferencedColumnName);
            if (null != referenceModel) {
                column.setClz(referenceModel.getClz());
                column.setJdbcType(referenceModel.getJdbcType());
            }
        }
        joinColumns.put(column.getName(), column);
    }

    private void resolveJoinColumn(Method method, Field field, MybatisEntityModel dm) {
        String joinColumnName = StringUtils.camelToUnderline(dm.getName()).toUpperCase() + "_" + dm.getPrimaryKeys().values().iterator().next().getNameInDatabase();
        String joinReferencedColumnName = dm.getPrimaryKeys().values().iterator().next().getNameInDatabase();
        String joinReferencedName = dm.getPrimaryKeys().values().iterator().next().getName();
        JoinColumn joinColumn = getAnnotation(method, field, JoinColumn.class);
        if (null != joinColumn) {
            if (StringUtils.isNotEmpty(joinColumn.name())) {
                joinColumnName = joinColumn.name();
            }
            if (StringUtils.isNotEmpty(joinColumn.referencedColumnName())) {
                joinReferencedColumnName = joinColumn.referencedColumnName();
                MybatisEntityModel referenceModel = dm.findColumnByColumnName(joinReferencedColumnName);
                if (null != referenceModel) {
                    joinReferencedName = referenceModel.getName();
                }
            }
        }
        dm.setJoinColumnName(joinColumnName);
        dm.setJoinReferencedColumnName(joinReferencedColumnName);
        dm.setJoinReferencedName(joinReferencedName);
    }


    protected static String classToEntityName(Class<?> entityClass) {
        String entityName = entityClass.getSimpleName();
        char[] chars = entityName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    protected static String classToTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (null != table && StringUtils.isNotEmpty(table.name())) {
            return table.name();
        }
        return StringUtils.camelToUnderline(entityClass.getSimpleName()).toUpperCase(); // guess from the domain's name
    }

    protected <X extends Annotation> X getAnnotation(Method method, Field field, Class<X> annotationClass) {
        X annotation = method.getAnnotation(annotationClass);
        if (null == annotation && null != field) {
            annotation = field.getAnnotation(annotationClass);
        }
        return annotation;
    }

    protected <X extends Annotation> boolean hasAnnotation(Method method, Field field, Class<X> annotationClass) {
        return null != getAnnotation(method, field, annotationClass);
    }

    protected <X extends Annotation> boolean hasAnnotation(PropertyDescriptor propertyDescriptor, Class<X> annotationClass) {
        Method method = propertyDescriptor.getReadMethod();
        String name = propertyDescriptor.getName();
        Field field = ReflectionUtils.findField(this.clz, name);
        return hasAnnotation(method, field, annotationClass);
    }

    private String getPropertyName(PropertyDescriptor propertyDescriptor, Method method, Field field) {
        String name = propertyDescriptor.getName();
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        if (null != field) {
            return field.getName();
        }
        if (null != method) {
            String propertyName = method.getName().replace("get", "");
            char[] chars = propertyName.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            return new String(chars);
        }
        return null;
    }

    protected Class<?> getType(PropertyDescriptor propertyDescriptor, Method method, Field field) {
        Class<?> type = propertyDescriptor.getPropertyType();
        if (null != type) {
            return type;
        }
        type = method.getReturnType();
        if (null != type) {
            return type;
        }
        if (null != field) {
            return field.getType();
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameInDatabase() {
        return nameInDatabase;
    }

    public void setNameInDatabase(String nameInDatabase) {
        this.nameInDatabase = nameInDatabase;
    }

    public String getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(String jdbcType) {
        this.jdbcType = jdbcType;
    }

    public Class<?> getClz() {
        return clz;
    }

    public void setClz(Class<?> clz) {
        this.clz = clz;
    }

    public MybatisEntityModel getParent() {
        return parent;
    }

    public void setParent(MybatisEntityModel parent) {
        this.parent = parent;
    }

    public Map<String, MybatisEntityModel> getColumns() {
        return columns;
    }

    public Map<String, MybatisEntityModel> getPrimaryKeys() {
        return primaryKeys;
    }

    public Map<String, MybatisEntityModel> getOneToOnes() {
        return oneToOnes;
    }

    public Map<String, MybatisEntityModel> getManyToOnes() {
        return manyToOnes;
    }

    public Map<String, MybatisEntityModel> getOneToManys() {
        return oneToManys;
    }

    public Map<String, MybatisEntityModel> getManyToManys() {
        return manyToManys;
    }

    public Map<String, MybatisEntityModel> getJoinColumns() {
        return joinColumns;
    }

    public List<SearchModel> getSearchModels() {
        return searchModels;
    }

    public String getJoinColumnName() {
        return joinColumnName;
    }

    public void setJoinColumnName(String joinColumnName) {
        this.joinColumnName = joinColumnName;
    }

    public String getJoinReferencedColumnName() {
        return joinReferencedColumnName;
    }

    public void setJoinReferencedColumnName(String joinReferencedColumnName) {
        this.joinReferencedColumnName = joinReferencedColumnName;
    }

    public String getJoinReferencedName() {
        return joinReferencedName;
    }

    public void setJoinReferencedName(String joinReferencedName) {
        this.joinReferencedName = joinReferencedName;
    }

    public boolean isGeneratedValue() {
        return generatedValue;
    }

    public void setGeneratedValue(boolean generatedValue) {
        this.generatedValue = generatedValue;
    }

    public MybatisEntityModel getPrimaryKey() {
        return primaryKey;
    }

    public boolean isCompositeId() {
        return compositeId;
    }


    /**
     * Searcher model.
     */
    public static class SearchModel {
        private String propertyName;
        private String columnName;
        private String operate;
        private String oper;
        private String alias;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public String getOper() {
            return oper;
        }

        public void setOper(String oper) {
            this.oper = oper;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getOperate() {
            return operate;
        }

        public void setOperate(String operate) {
            this.operate = operate;
        }
    }
}
