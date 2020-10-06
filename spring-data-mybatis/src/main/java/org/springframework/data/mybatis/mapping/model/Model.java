/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.mapping.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public interface Model extends Serializable {

	MybatisMappingContext getMappingContext();

	String getAlias();

	Model getOwner();

	Table getTable();

	MybatisPersistentEntity<?> getMappingEntity();

	PrimaryKey getPrimaryKey();

	Collection<Component> getNormalColumns();

	Collection<Component> getVersionColumns();

	Collection<Embedding> getEmbeddings();

	Collection<Association> getOneToOneAssociations();

	Collection<Association> getManyToOneAssociations();

	Collection<Association> getOneToManyAssociations();

	Collection<Association> getManyToManyAssociations();

	Column findColumn(String columnName);

	Column findColumn(PropertyPath propertyPath);

	Column findColumnByPropertyName(String propertyName);

	List<Column> findColumnByTypeHandler(Class<?> typeHandlerClass);

}
