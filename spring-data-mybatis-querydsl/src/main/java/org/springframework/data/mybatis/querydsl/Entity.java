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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.lang.model.element.TypeElement;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Entity implements Serializable {

	private static final long serialVersionUID = 2505197848959082580L;

	private final TypeElement element;

	private String packageName;

	private String className;

	private String queryClassName;

	private String schema;

	private String catalog;

	private String tableName;

	private List<Property> properties = new LinkedList<>();

	private Map<String, Map<String, Object>> genericTypeArgumentInfo = new HashMap<>();

	public Entity(TypeElement element) {

		this.element = element;
	}

	public String getInstanceName() {
		return Character.toLowerCase(this.className.charAt(0)) + this.className.substring(1);
	}

	public boolean isHasSchema() {
		return null != this.schema;
	}

	public Long getSerialVersion() {
		return new Random().nextLong();
	}

	public String getPackageName() {
		return this.packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getClassName() {
		return this.className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getQueryClassName() {
		return this.queryClassName;
	}

	public void setQueryClassName(String queryClassName) {
		this.queryClassName = queryClassName;
	}

	public void addProperties(List<Property> properties) {
		this.properties.addAll(properties);
	}

	public String getSchema() {
		return this.schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getCatalog() {
		return this.catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getTableName() {
		return this.tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public List<Property> getProperties() {
		return this.properties;
	}

	public Map<String, Map<String, Object>> getGenericTypeArgumentInfo() {
		return this.genericTypeArgumentInfo;
	}

	public void setGenericTypeArgumentInfo(Map<String, Map<String, Object>> genericTypeArgumentInfo) {
		this.genericTypeArgumentInfo = genericTypeArgumentInfo;
	}

	public TypeElement getElement() {
		return this.element;
	}

	@Override
	public String toString() {
		return "Entity{" + "element=" + this.element + ", packageName='" + this.packageName + '\'' + ", className='"
				+ this.className + '\'' + ", queryClassName='" + this.queryClassName + '\'' + ", schema='" + this.schema
				+ '\'' + ", catalog='" + this.catalog + '\'' + ", tableName='" + this.tableName + '\'' + ", properties="
				+ this.properties + '}';
	}

}
