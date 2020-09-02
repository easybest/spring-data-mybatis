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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.data.mybatis.annotation.JdbcType;
import org.springframework.data.util.ParsingUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Domain implements Serializable {

	private static final long serialVersionUID = -5860693531341982730L;

	private final boolean mapUnderscoreToCamelCase;

	private String packageName;

	private String className;

	private String queryClassName;

	private String schema;

	private String catalog;

	private String tableName;

	private List<Property> properties = new ArrayList<>();

	private final Map<String, Map<String, Object>> genericInfo = new HashMap<>();

	public Domain(ProcessingEnvironment processingEnv, boolean mapUnderscoreToCamelCase, TypeElement element) {
		this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
		Elements utils = processingEnv.getElementUtils();
		PackageElement packageElement = utils.getPackageOf(element);
		this.packageName = packageElement.getQualifiedName().toString();

		this.className = element.getSimpleName().toString();
		this.queryClassName = 'Q' + this.className;

		this.tableName(element);

		GenericUtils genericUtils = new GenericUtils(processingEnv.getElementUtils(), processingEnv.getTypeUtils(),
				new ModelUtils(processingEnv.getElementUtils(), processingEnv.getTypeUtils()));
		Map<String, Map<String, Object>> info = genericUtils.buildGenericTypeArgumentInfo(element);
		this.genericInfo.putAll(info);
		this.processClass(processingEnv, element);

	}

	private void processClass(ProcessingEnvironment processingEnv, TypeElement element) {

		this.properties(element);

		TypeMirror superclass = element.getSuperclass();
		if (null == superclass || superclass.getKind() == TypeKind.NONE) {
			return;
		}

		this.processClass(processingEnv, (TypeElement) processingEnv.getTypeUtils().asElement(superclass));
	}

	private void properties(TypeElement element) {
		this.properties.addAll(ElementFilter.fieldsIn(element.getEnclosedElements()).stream()
				.filter(member -> !member.getModifiers().contains(Modifier.STATIC)
						&& !member.getModifiers().contains(Modifier.TRANSIENT)
						&& null == member.getAnnotation(Transient.class)
						&& null == member.getAnnotation(org.springframework.data.annotation.Transient.class))
				.map(member -> {
					Property property = new Property(this);
					property.setName(member.getSimpleName().toString());
					property.setColumnName(this.columnName(member));
					this.processType(element, member, property);
					return property;
				}).filter(p -> null != p.getPathType()).collect(Collectors.toList()));
	}

	private void processType(TypeElement element, VariableElement member, Property property) {
		JdbcType jdbcTypeAnn = member.getAnnotation(JdbcType.class);
		if (null != jdbcTypeAnn && StringUtils.hasText(jdbcTypeAnn.value())) {
			property.setJdbcType(org.apache.ibatis.type.JdbcType.valueOf(jdbcTypeAnn.value()));
		}
		member.asType().accept(new DomainTypeVisitor(element), property);
	}

	private String columnName(Element member) {

		Column columnAnn = member.getAnnotation(Column.class);
		if (null != columnAnn && columnAnn.name().trim().length() > 0) {
			return columnAnn.name().trim();
		}
		OrderColumn orderColumnAnn = member.getAnnotation(OrderColumn.class);
		if (null != orderColumnAnn && orderColumnAnn.name().trim().length() > 0) {
			return orderColumnAnn.name().trim();
		}

		if (this.mapUnderscoreToCamelCase) {
			return String.join("_", ParsingUtils.splitCamelCaseToLower(member.getSimpleName().toString()));
		}

		return member.getSimpleName().toString();
	}

	private void tableName(TypeElement element) {
		Table tableAnn = element.getAnnotation(Table.class);
		if (null != tableAnn) {
			if (tableAnn.schema().trim().length() > 0) {
				this.schema = tableAnn.schema().trim();
			}
			if (tableAnn.catalog().trim().length() > 0) {
				this.catalog = tableAnn.catalog().trim();
			}
			if (tableAnn.name().trim().length() > 0) {
				this.tableName = tableAnn.name().trim();
				return;
			}
		}

		Entity entityAnn = element.getAnnotation(Entity.class);
		if (null != entityAnn && entityAnn.name().trim().length() > 0) {
			this.tableName = entityAnn.name().trim();
			return;
		}

		this.tableName = element.getSimpleName().toString();

	}

	public void addProperty(Property property) {
		this.properties.add(property);
	}

	public String getInstanceName() {
		return Character.toLowerCase(this.className.charAt(0)) + this.className.substring(1);
	}

	public Long getSerialVersion() {
		return new Random().nextLong();
	}

	public boolean isHasSchema() {
		return null != this.schema;
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

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	public Map<String, Map<String, Object>> getGenericInfo() {
		return this.genericInfo;
	}

	@Override
	public String toString() {
		return "Domain{" + "packageName='" + this.packageName + '\'' + ", className='" + this.className + '\''
				+ ", queryClassName='" + this.queryClassName + '\'' + ", schema='" + this.schema + '\'' + ", catalog='"
				+ this.catalog + '\'' + ", tableName='" + this.tableName + '\'' + ", properties=" + this.properties
				+ '}';
	}

}
