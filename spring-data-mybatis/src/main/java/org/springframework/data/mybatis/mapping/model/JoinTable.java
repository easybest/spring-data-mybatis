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

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class JoinTable implements Component {

	private final Model model;

	private final Model targetModel;

	private final ManyToManyAssociation assModel;

	private Table table;

	private final List<JoinColumn> joinColumns = new LinkedList<>();

	private final List<JoinColumn> inverseJoinColumns = new LinkedList<>();

	public JoinTable(Model model, ManyToManyAssociation assModel, MybatisPersistentProperty property) {
		this.model = model;
		this.assModel = assModel;
		this.targetModel = model.getMappingContext().getRequiredModel(property.getActualType());
		this.initialize(property);
	}

	private void initialize(MybatisPersistentProperty property) {
		String schema = null;
		String catalog = null;
		String name = null;
		if (property.isAnnotationPresent(javax.persistence.JoinTable.class)) {
			javax.persistence.JoinTable tableAnn = property.getRequiredAnnotation(javax.persistence.JoinTable.class);
			schema = tableAnn.schema();
			catalog = tableAnn.catalog();
			name = tableAnn.name();
		}
		if (StringUtils.isEmpty(name)) {
			name = this.model.getTable().getName().getText() + '_' + this.assModel.getTable().getName().getText();
		}
		this.table = new Table(schema, catalog, name);

		if (property.isAnnotationPresent(javax.persistence.JoinTable.class)) {
			javax.persistence.JoinTable tableAnn = property.getRequiredAnnotation(javax.persistence.JoinTable.class);
			if (null != tableAnn.joinColumns() && tableAnn.joinColumns().length > 0) {
				if (tableAnn.joinColumns().length > 0) {
					for (javax.persistence.JoinColumn joinColumn : tableAnn.joinColumns()) {
						this.joinColumns.add(this.processJoinColumn(this.model, joinColumn.name(),
								joinColumn.referencedColumnName()));
					}
				}
				if (null != tableAnn.inverseJoinColumns() && tableAnn.inverseJoinColumns().length > 0) {
					for (javax.persistence.JoinColumn inverseJoinColumn : tableAnn.inverseJoinColumns()) {
						this.inverseJoinColumns.add(this.processJoinColumn(this.assModel, inverseJoinColumn.name(),
								inverseJoinColumn.referencedColumnName()));
					}

				}
			}
		}

		if (this.joinColumns.isEmpty()) {
			this.joinColumns.add(this.processJoinColumn(this.model, null, null));
		}
		if (this.inverseJoinColumns.isEmpty()) {
			this.inverseJoinColumns.add(this.processJoinColumn(this.assModel, null, null));
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
			throw new MappingException("Could not find referenced column by " + referencedColumnName);
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

	@Override
	public Model getModel() {
		return this.model;
	}

	public Table getTable() {
		return this.table;
	}

	public List<JoinColumn> getJoinColumns() {
		return this.joinColumns;
	}

	public List<JoinColumn> getInverseJoinColumns() {
		return this.inverseJoinColumns;
	}

	public Model getTargetModel() {
		return this.targetModel;
	}

	public String getTableAlias() {
		return "ass_" + this.assModel.getTableAlias();
	}

}
