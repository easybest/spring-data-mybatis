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
package org.springframework.data.mybatis.querydsl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.VariableElement;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.apache.ibatis.type.JdbcType;

import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Property implements Serializable {

	private static final long serialVersionUID = 4593515230257618786L;

	private Entity entity;

	private final VariableElement member;

	private String name;

	private String columnName;

	private String javaType;

	private JdbcType jdbcType;

	private String pathType;

	private Association association;

	public Property(Entity entity, VariableElement member) {
		this.entity = entity;
		this.member = member;
	}

	public boolean valid() {
		return StringUtils.hasText(this.name) && StringUtils.hasText(this.javaType);
	}

	public boolean isId() {
		if (null != this.member.getAnnotation(Id.class)) {
			return true;
		}
		return false;
	}

	public boolean isPath() {
		return null != this.pathType;
	}

	public boolean isEmbedded() {
		javax.persistence.Embedded embedded = this.member.getAnnotation(Embedded.class);
		if (null != embedded) {
			return true;
		}
		EmbeddedId embeddedId = this.member.getAnnotation(EmbeddedId.class);
		return null != embeddedId;
	}

	public boolean isToOne() {
		ManyToOne manyToOne = this.member.getAnnotation(ManyToOne.class);
		if (null != manyToOne) {
			return true;
		}
		OneToOne oneToOne = this.member.getAnnotation(OneToOne.class);
		return null != oneToOne;
	}

	public boolean isNeedPattern() {
		return !"StringPath".equals(this.pathType) && !"BooleanPath".equals(this.pathType);
	}

	public String getPathFactory() {
		if (null == this.pathType) {
			return null;
		}
		return this.pathType.replace("Path", "");
	}

	public String getQueryJavaType() {
		String javaTypeString = this.getJavaType();
		if (null == javaTypeString) {
			return null;
		}
		int pos = javaTypeString.lastIndexOf('.');
		if (pos > 0) {
			return javaTypeString.substring(0, pos + 1) + "Q" + javaTypeString.substring(pos + 1);
		}
		return "Q" + javaTypeString;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColumnName() {
		return this.columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public String getJavaType() {
		return this.javaType;
	}

	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}

	public JdbcType getJdbcType() {
		return this.jdbcType;
	}

	public void setJdbcType(JdbcType jdbcType) {
		this.jdbcType = jdbcType;
	}

	public Entity getEntity() {
		return this.entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public String getPathType() {
		return this.pathType;
	}

	public void setPathType(String pathType) {
		this.pathType = pathType;
	}

	public VariableElement getMember() {
		return this.member;
	}

	public Association getAssociation() {
		return this.association;
	}

	public void setAssociation(Association association) {
		this.association = association;
	}

	@Override
	public String toString() {
		return "Property{" + "member=" + this.member + ", name='" + this.name + '\'' + ", columnName='"
				+ this.columnName + '\'' + ", javaType='" + this.javaType + '\'' + ", jdbcType=" + this.jdbcType
				+ ", pathType='" + this.pathType + '\'' + '}';
	}

	static class Association {

		private final List<Property> locals = new ArrayList<>();

		private final List<Property> foreigns = new ArrayList<>();

		private Property property;

		public void add(Property property, Property local, Property foreign) {
			this.property = property;
			this.locals.add(local);
			this.foreigns.add(foreign);
		}

		public List<Property> getLocals() {
			return this.locals;
		}

		public List<Property> getForeigns() {
			return this.foreigns;
		}

		public Property getProperty() {
			return this.property;
		}

	}

}
