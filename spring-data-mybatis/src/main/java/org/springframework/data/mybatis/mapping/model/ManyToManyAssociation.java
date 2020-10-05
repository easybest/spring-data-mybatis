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

import javax.persistence.FetchType;
import javax.persistence.ManyToMany;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.annotation.Fetch;
import org.springframework.data.mybatis.annotation.FetchMode;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class ManyToManyAssociation extends Domain implements Association {

	private static final long serialVersionUID = -4694636482266575894L;

	private final MybatisPersistentProperty property;

	protected FetchType fetchType;

	protected FetchMode fetchMode;

	protected final JoinTable joinTable;

	public ManyToManyAssociation(MybatisMappingContext mappingContext, Model owner, MybatisPersistentProperty property,
			String alias) {
		super(mappingContext, owner, property.getActualType(), alias);
		this.property = property;
		this.fetchType = (FetchType) AnnotationUtils.getValue(property.findAnnotation(ManyToMany.class), "fetch");
		Fetch fetch = property.findAnnotation(Fetch.class);
		if (null != fetch) {
			this.fetchMode = fetch.value();
		}
		else {
			this.fetchMode = FetchMode.SELECT; // default
		}
		this.joinTable = new JoinTable(owner, this, property);
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

	public JoinTable getJoinTable() {
		return this.joinTable;
	}

}
