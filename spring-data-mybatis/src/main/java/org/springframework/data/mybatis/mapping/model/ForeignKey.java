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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
public class ForeignKey implements Serializable {

	private static final long serialVersionUID = -2443525149024712909L;

	private final MybatisPersistentProperty property;

	private final Domain local;

	private final Domain foreign;

	private final List<JoinColumn> joinColumns = new LinkedList<>();

	public ForeignKey(MybatisPersistentProperty property, Domain local, Domain foreign) {
		this.property = property;
		this.local = local;
		this.foreign = foreign;

		Set<javax.persistence.JoinColumn> joinColumnAnnotations = new HashSet<>();
		JoinColumns joinColumnsAnn = property.findAnnotation(JoinColumns.class);
		if (null != joinColumnsAnn && joinColumnsAnn.value().length > 0) {
			joinColumnAnnotations.addAll(Arrays.asList(joinColumnsAnn.value()));
		}
		javax.persistence.JoinColumn joinColumnAnn = property.findAnnotation(javax.persistence.JoinColumn.class);
		if (null != joinColumnAnn) {
			joinColumnAnnotations.add(joinColumnAnn);
		}

		if (!joinColumnAnnotations.isEmpty()) {
			this.joinColumns.addAll(joinColumnAnnotations.stream()
					.map(joinColumn -> this.processJoinColumn(joinColumn.name(), joinColumn.referencedColumnName()))
					.collect(Collectors.toList()));
		}
		else {
			this.joinColumns.add(this.processJoinColumn(null, null));
		}
	}

	private JoinColumn processJoinColumn(String localColumnName, String foreignColumnName) {
		Column local;
		Column foreign;
		if (StringUtils.isEmpty(foreignColumnName)) {
			PrimaryKey foreignPrimaryKey = this.foreign.getPrimaryKey();
			if (null == foreignPrimaryKey || foreignPrimaryKey.isComposited()) {
				throw new MappingException("Could not find referenced column in " + this.property);
			}
			foreign = foreignPrimaryKey.getColumns().values().stream().findFirst().get();
		}
		else {
			foreign = this.foreign.findColumn(Identifier.toIdentifier(foreignColumnName));
		}

		if (null == foreign) {
			throw new MappingException("Count not find referenced column in " + this.property);
		}

		if (StringUtils.isEmpty(localColumnName)) {
			localColumnName = this.property.getName() + '_' + foreign.getName().getText();
		}
		Identifier localIdentifier = Identifier.toIdentifier(localColumnName);
		local = this.local.findColumn(localIdentifier);
		if (null == local) {
			local = new Column(this.local, foreign.getProperty(),
					this.property.getName() + '.' + foreign.getProperty().getName());
			local.setName(localIdentifier);
		}

		return new JoinColumn(local, foreign);
	}

	public List<JoinColumn> getJoinColumns() {
		return this.joinColumns;
	}

	public MybatisPersistentProperty getProperty() {
		return this.property;
	}

	public Domain getLocal() {
		return this.local;
	}

	public Domain getForeign() {
		return this.foreign;
	}

}
