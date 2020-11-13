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

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class JoinTable extends Connector {

	private static final long serialVersionUID = 6291689921380485728L;

	private Table table;
	private List<JoinColumn> locals = new LinkedList<>();
	private List<JoinColumn> foreigns = new LinkedList<>();

	public void addLocal(JoinColumn joinColumn){
		this.locals.add(joinColumn);
	}
	public void addForeign(JoinColumn joinColumn){
		this.foreigns.add(joinColumn);
	}

	public Table getTable() {
		return this.table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public List<JoinColumn> getLocals() {
		return this.locals;
	}



	public List<JoinColumn> getForeigns() {
		return this.foreigns;
	}



	//	private final MybatisPersistentProperty property;
//
//	private final Domain localDomain;
//
//	private final Domain foreignDomain;
//
//	private final Table table;
//
//	private final List<JoinColumn> localJoinColumns = new LinkedList<>();
//
//	private final List<JoinColumn> foreignJoinColumns = new LinkedList<>();
//
//	public JoinTable(MybatisPersistentProperty property, Domain localDomain, Domain foreignDomain) {
//		this.property = property;
//		this.localDomain = localDomain;
//		this.foreignDomain = foreignDomain;
//
//		String schema = null;
//		String catalog = null;
//		String name = null;
//		if (property.isAnnotationPresent(javax.persistence.JoinTable.class)) {
//			javax.persistence.JoinTable joinTableAnn = property
//					.getRequiredAnnotation(javax.persistence.JoinTable.class);
//			schema = joinTableAnn.schema();
//			catalog = joinTableAnn.catalog();
//			name = joinTableAnn.name();
//
//			if (null != joinTableAnn.joinColumns() && joinTableAnn.joinColumns().length > 0) {
//				for (javax.persistence.JoinColumn joinColumn : joinTableAnn.joinColumns()) {
//					this.localJoinColumns
//							.add(this.processJoinColumn(false, joinColumn.name(), joinColumn.referencedColumnName()));
//				}
//			}
//			if (null != joinTableAnn.inverseJoinColumns() && joinTableAnn.inverseJoinColumns().length > 0) {
//				for (javax.persistence.JoinColumn joinColumn : joinTableAnn.inverseJoinColumns()) {
//					this.foreignJoinColumns
//							.add(this.processJoinColumn(true, joinColumn.name(), joinColumn.referencedColumnName()));
//				}
//			}
//		}
//
//		if (this.localJoinColumns.isEmpty()) {
//			this.localJoinColumns.add(this.processJoinColumn(false, null, null));
//		}
//
//		if (this.foreignJoinColumns.isEmpty()) {
//			this.foreignJoinColumns.add(this.processJoinColumn(true, null, null));
//		}
//
//		if (StringUtils.isEmpty(name)) {
//			name = localDomain.getTable().getName().getText() + '_' + foreignDomain.getTable().getName().getText();
//		}
//		this.table = new Table(schema, catalog, name);
//
//	}
//
//	public String getTableAlias() {
//		return this.table.getName().getText() + '_' + this.property.getName();
//	}
//
//	private JoinColumn processJoinColumn(boolean referenced, String localColumnName, String foreignColumnName) {
//		Domain domain = referenced ? this.foreignDomain : this.localDomain;
//		Column local;
//		Column foreign;
//		if (StringUtils.isEmpty(foreignColumnName)) {
//			PrimaryKey foreignPrimaryKey = domain.getPrimaryKey();
//			if (null == foreignPrimaryKey || foreignPrimaryKey.isComposited()) {
//				throw new MappingException(
//						"Could not find referenced column [" + foreignColumnName + "] in " + this.property);
//			}
//			foreign = foreignPrimaryKey.getColumns().values().stream().findFirst().get();
//		}
//		else {
//			foreign = domain.findColumn(Identifier.toIdentifier(foreignColumnName));
//		}
//
//		if (null == foreign) {
//			throw new MappingException("Count not find referenced column in " + this.property);
//		}
//
//		if (StringUtils.isEmpty(localColumnName)) {
//			localColumnName = domain.getTable().getName().getText() + '_' + foreign.getName().getText();
//		}
//		Identifier localIdentifier = Identifier.toIdentifier(localColumnName);
//
//		local = new Column(domain, foreign.getProperty());
//		local.setName(localIdentifier);
//		return new JoinColumn(local, foreign);
//	}
//
//	public MybatisPersistentProperty getProperty() {
//		return this.property;
//	}
//
//	public Domain getLocalDomain() {
//		return this.localDomain;
//	}
//
//	public Domain getForeignDomain() {
//		return this.foreignDomain;
//	}
//
//	public Table getTable() {
//		return this.table;
//	}
//
//	public List<JoinColumn> getLocalJoinColumns() {
//		return this.localJoinColumns;
//	}
//
//	public List<JoinColumn> getForeignJoinColumns() {
//		return this.foreignJoinColumns;
//	}

}
