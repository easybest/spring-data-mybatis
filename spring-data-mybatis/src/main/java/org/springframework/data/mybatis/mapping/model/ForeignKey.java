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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.JoinColumns;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class ForeignKey implements Component {

	private static final long serialVersionUID = 2476229243396329609L;

	private final List<JoinColumn> columns = new LinkedList<>();

	private final Model model;

	private final ManyToOneAssociation assModel;

	public ForeignKey(Model model, ManyToOneAssociation assModel, MybatisPersistentProperty property) {
		this.model = model;
		this.assModel = assModel;

		Set<javax.persistence.JoinColumn> joinColumnAnns = new HashSet<>();
		JoinColumns joinColumnsAnn = property.findAnnotation(JoinColumns.class);
		if (null != joinColumnsAnn && joinColumnsAnn.value().length > 0) {
			joinColumnAnns.addAll(Arrays.asList(joinColumnsAnn.value()));
		}
		javax.persistence.JoinColumn joinColumnAnn = property.findAnnotation(javax.persistence.JoinColumn.class);
		if (null != joinColumnAnn) {
			joinColumnAnns.add(joinColumnAnn);
		}

		if (!joinColumnAnns.isEmpty()) {
			joinColumnAnns.stream().map(jc -> this.processJoinColumn(property, jc.name(), jc.referencedColumnName()))
					.forEach(jc -> this.columns.add(jc));
		}
		else {
			this.columns.add(this.processJoinColumn(property, null, null));
		}
	}

	private JoinColumn processJoinColumn(MybatisPersistentProperty property, String columnName,
			String referencedColumnName) {
		Model targetModel = this.model.getMappingContext().getRequiredModel(property.getActualType());
		PrimaryKey primaryKey = this.model.getPrimaryKey();
		if (StringUtils.isEmpty(referencedColumnName) && !(primaryKey instanceof SinglePrimaryKey)) {
			throw new MappingException("Could not find referenced column name for: " + property);
		}

		referencedColumnName = StringUtils.hasText(referencedColumnName) ? referencedColumnName
				: primaryKey.getColumns().iterator().next().getName().getCanonicalName();
		Column referencedColumn = targetModel.findColumn(referencedColumnName);
		if (null == referencedColumn) {
			throw new MappingException(
					"Could not find referenced column by " + referencedColumnName + " in " + property);
		}

		Column foreign = new Column(this.assModel, referencedColumn.getProperty());
		foreign.setName(referencedColumn.getName());
		foreign.setJavaType(referencedColumn.getJavaType());
		foreign.setJdbcType(referencedColumn.getJdbcType());
		foreign.setTypeHandler(referencedColumn.getTypeHandler());
		Column local = new Column(this.model, property);
		local.setName(Identifier.toIdentifier(
				StringUtils.hasText(columnName) ? columnName : property.getName() + '_' + foreign.getName().getText()));
		local.setJavaType(foreign.getJavaType());
		local.setJdbcType(foreign.getJdbcType());
		local.setTypeHandler(foreign.getTypeHandler());

		return new JoinColumn(local, foreign);
	}

	@Override
	public Model getModel() {
		return this.model;
	}

	public List<JoinColumn> getColumns() {
		return this.columns;
	}

}
