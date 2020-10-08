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

import java.util.LinkedList;
import java.util.List;

import javax.persistence.FetchType;
import javax.persistence.JoinColumns;
import javax.persistence.OneToMany;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.annotation.Fetch;
import org.springframework.data.mybatis.annotation.FetchMode;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class OneToManyAssociation extends Domain implements Association {

	private static final long serialVersionUID = -5958192304583654899L;

	private final MybatisPersistentProperty property;

	protected FetchType fetchType;

	protected FetchMode fetchMode;

	protected final List<JoinColumn> joinColumns = new LinkedList<>();

	protected final Model targetModel;

	public OneToManyAssociation(MybatisMappingContext mappingContext, Model owner, MybatisPersistentProperty property,
			String alias) {
		super(mappingContext, owner, property.getActualType(), alias);
		this.property = property;
		this.fetchType = (FetchType) AnnotationUtils.getValue(property.findAnnotation(OneToMany.class), "fetch");
		Fetch fetch = property.findAnnotation(Fetch.class);
		if (null != fetch) {
			this.fetchMode = fetch.value();
		}
		else {
			this.fetchMode = FetchMode.SELECT; // default
		}

		this.targetModel = mappingContext.getRequiredModel(property.getActualType());

		JoinColumns joinColumnsAnn = property.findAnnotation(JoinColumns.class);
		if (null != joinColumnsAnn && joinColumnsAnn.value().length > 0) {
			for (javax.persistence.JoinColumn joinColumnAnn : joinColumnsAnn.value()) {
				this.joinColumns.add(this.processJoinColumn(this.targetModel, joinColumnAnn.name(),
						joinColumnAnn.referencedColumnName()));
			}
		}
		javax.persistence.JoinColumn joinColumnAnn = property.findAnnotation(javax.persistence.JoinColumn.class);
		if (null != joinColumnAnn) {
			this.joinColumns.add(this.processJoinColumn(this.targetModel, joinColumnAnn.name(),
					joinColumnAnn.referencedColumnName()));
		}

		if (this.joinColumns.isEmpty()) {
			this.joinColumns.add(this.processJoinColumn(this.targetModel, null, null));
		}

	}

	private JoinColumn processJoinColumn(Model targetModel, String name, String referencedColumnName) {
		PrimaryKey primaryKey = targetModel.getPrimaryKey();
		if (StringUtils.isEmpty(referencedColumnName) && !(primaryKey instanceof SinglePrimaryKey)) {
			throw new MappingException("Could not find referenced column name for: " + targetModel.getAlias());
		}
		referencedColumnName = StringUtils.hasText(referencedColumnName) ? referencedColumnName
				: primaryKey.getColumns().iterator().next().getName().getCanonicalName();
		Column referencedColumn = targetModel.findColumn(referencedColumnName);
		if (null == referencedColumn) {
			throw new MappingException(
					"Could not find referenced column by " + referencedColumnName + " in " + targetModel);
		}

		Column foreign = new Column(targetModel, referencedColumn.getProperty());
		foreign.setName(referencedColumn.getName());
		foreign.setJavaType(referencedColumn.getJavaType());
		foreign.setJdbcType(referencedColumn.getJdbcType());
		foreign.setTypeHandler(referencedColumn.getTypeHandler());

		Column local = new Column(targetModel, referencedColumn.getProperty());
		local.setName(Identifier.toIdentifier(StringUtils.hasText(name) ? name
				: targetModel.getTable().getName().getText() + '_' + foreign.getName().getText()));
		local.setJavaType(foreign.getJavaType());
		local.setJdbcType(foreign.getJdbcType());
		local.setTypeHandler(foreign.getTypeHandler());
		return new JoinColumn(local, foreign);
	}

	public MybatisPersistentProperty getProperty() {
		return this.property;
	}

	public String getFetchType() {
		if (null == this.fetchType) {
			return null;
		}
		return this.fetchType.name().toLowerCase();
	}

	public FetchMode getFetchMode() {
		return this.fetchMode;
	}

	public List<JoinColumn> getJoinColumns() {
		return this.joinColumns;
	}

	public Model getTargetModel() {
		return this.targetModel;
	}

}
