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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.mysema.codegen.model.ClassType;
import com.mysema.codegen.model.TypeCategory;
import com.mysema.codegen.model.Types;
import com.querydsl.codegen.TypeFactory;
import com.querydsl.sql.types.Type;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.Compiler;
import com.samskivert.mustache.Template;

import org.springframework.data.mybatis.annotation.JdbcType;
import org.springframework.data.mybatis.querydsl.Property.Association;
import org.springframework.data.util.ParsingUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class MappingContext {

	/**
	 * Query class name prefix.
	 */
	public static final String QUERY_CLASS_PREFIX = "Q";

	private final ProcessingEnvironment processingEnv;

	private final Map<String, Entity> initialEntitySet = new HashMap<>();

	private final Map<String, EmbeddableEntity> embeddableEntitySet = new HashMap<>();

	private final Boolean mapUnderscoreToCamelCase;

	private final GenericUtils genericUtils;

	private final JavaTypeMapping javaTypeMapping = new JavaTypeMapping();

	public MappingContext(ProcessingEnvironment processingEnv) {

		this.processingEnv = processingEnv;
		this.genericUtils = new GenericUtils(processingEnv.getElementUtils(), processingEnv.getTypeUtils(),
				new ModelUtils(processingEnv.getElementUtils(), processingEnv.getTypeUtils()));
		this.mapUnderscoreToCamelCase = Boolean
				.valueOf(this.processingEnv.getOptions().getOrDefault("mapUnderscoreToCamelCase", "true"));

	}

	public void process() {
		this.initialEntitySet.values().stream().forEach(entity -> entity.getProperties().stream()
				.filter(p -> p.isToOne()).forEach(p -> this.processAssociationToOne(entity, p.getMember(), p)));

		Compiler mustache = Mustache.compiler().escapeHTML(false);
		Filer filer = this.processingEnv.getFiler();
		this.initialEntitySet.values().stream().forEach(entity -> this.render(mustache, filer, entity));
		this.embeddableEntitySet.values().stream().forEach(entity -> this.render(mustache, filer, entity));
	}

	private void render(Compiler mustache, Filer filer, Entity entity) {
		try {
			ClassLoader classLoader = MybatisAnnotationProcessor.class.getClassLoader();
			Map<String, Object> scopes = new HashMap<>();
			scopes.put("entity", entity);
			JavaFileObject javaFileObject = filer
					.createSourceFile(entity.getPackageName() + '.' + entity.getQueryClassName(), entity.getElement());
			InputStream is = classLoader.getResourceAsStream("template/Q.mustache");
			try (InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
					Writer writer = javaFileObject.openWriter()) {
				Template template = mustache.compile(in);
				template.execute(scopes, writer);
			}
		}
		catch (Exception ex) {
			this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
					ex.getMessage() + "\n" + Stream.of(ex.getStackTrace())
							.map(stackTraceElement -> stackTraceElement.toString()).collect(Collectors.joining("\n")));
		}
	}

	public void addEntityElements(Set<Element> elements) {
		if (CollectionUtils.isEmpty(elements)) {
			return;
		}

		this.initialEntitySet.putAll(elements.stream().map(element -> this.processEntity((TypeElement) element))
				.collect(Collectors.toMap(entity -> entity.getElement().toString(), entity -> entity)));

	}

	public void addEmbeddableEntityElements(Set<Element> elements) {
		if (CollectionUtils.isEmpty(elements)) {
			return;
		}
		this.embeddableEntitySet
				.putAll((Map) elements.stream().map(element -> this.processEntity((TypeElement) element))
						.collect(Collectors.toMap(entity -> entity.getElement().toString(), entity -> entity)));
	}

	private Entity processEntity(TypeElement element) {
		Entity entity;

		if (null != element.getAnnotation(Embeddable.class)) {
			entity = new EmbeddableEntity(element);
		}
		else {
			entity = new Entity(element);
		}

		this.processTableName(element, entity);

		entity.setPackageName(this.processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString());
		entity.setClassName(element.getSimpleName().toString());
		entity.setQueryClassName(QUERY_CLASS_PREFIX + entity.getClassName());
		entity.setGenericTypeArgumentInfo(this.genericUtils.buildGenericTypeArgumentInfo(element));

		this.processClass(element, entity);
		return entity;
	}

	private void processClass(TypeElement element, Entity entity) {
		this.processProperties(element, entity);

		TypeMirror superclass = element.getSuperclass();
		if (null == superclass || superclass.getKind() == TypeKind.NONE) {
			return;
		}

		this.processClass((TypeElement) this.processingEnv.getTypeUtils().asElement(superclass), entity);
	}

	private void processProperties(TypeElement element, Entity entity) {
		List<Property> properties = ElementFilter.fieldsIn(element.getEnclosedElements()).stream()
				.filter(member -> !member.getModifiers().contains(Modifier.STATIC)
						&& !member.getModifiers().contains(Modifier.TRANSIENT)
						&& null == member.getAnnotation(Transient.class)
						&& null == member.getAnnotation(org.springframework.data.annotation.Transient.class))
				.map(member -> this.processProperty(entity, element, member)).filter(Property::valid)
				.collect(Collectors.toList());
		entity.addProperties(properties);
	}

	private Property processProperty(Entity entity, TypeElement entityElement, VariableElement element) {
		Property property = new Property(entity, element);
		property.setName(element.getSimpleName().toString());
		this.processColumnName(element, property);
		this.processPropertyJavaType(entityElement, element, property);
		this.processPropertyJdbcType(element, property);
		this.processPropertyPathType(element, property);
		return property;
	}

	private void processAssociationToOne(Entity entity, VariableElement element, Property property) {

		if (null == element.getAnnotation(OneToOne.class) && null == element.getAnnotation(ManyToOne.class)) {
			return;
		}

		Entity target = this.initialEntitySet.get(property.getJavaType());
		if (null == target) {
			return;
		}

		String targetPk = target.getProperties().stream().filter(Property::isId).map(p -> p.getColumnName()).findFirst()
				.orElse("id");

		List<JoinColumn> joinColumns = new ArrayList<>();
		JoinColumn joinColumnAnn = element.getAnnotation(JoinColumn.class);
		if (null != joinColumnAnn) {
			joinColumns.add(joinColumnAnn);
		}
		JoinColumns joinColumnsAnn = element.getAnnotation(JoinColumns.class);
		if (null != joinColumnsAnn) {
			joinColumns.addAll(Stream.of(joinColumnsAnn.value()).collect(Collectors.toList()));
		}

		String[] locals;
		String[] foreigns;
		if (CollectionUtils.isEmpty(joinColumns)) {
			locals = new String[] { property.getName() + "_" + targetPk };
			foreigns = new String[] { targetPk };
		}
		else {
			locals = new String[joinColumns.size()];
			foreigns = new String[joinColumns.size()];
			for (int i = 0; i < joinColumns.size(); i++) {
				JoinColumn joinColumn = joinColumns.get(i);
				String local = property.getName() + "_" + targetPk;
				String foreign = targetPk;

				if (StringUtils.hasText(joinColumn.name())) {
					local = joinColumn.name();
				}
				if (StringUtils.hasText(joinColumn.referencedColumnName())) {
					foreign = joinColumn.referencedColumnName();
				}
				locals[i] = local;
				foreigns[i] = foreign;
			}

		}

		Association association = new Association();
		for (int i = 0; i < locals.length; i++) {
			String local = locals[i];
			String foreign = foreigns[i];
			target.getProperties().stream().filter(p -> foreign.equals(p.getColumnName())).findFirst()
					.ifPresent(targetProperty -> {
						Property localProperty = new Property(entity, targetProperty.getMember());
						localProperty.setName(local);
						localProperty.setColumnName(local);
						localProperty.setJdbcType(targetProperty.getJdbcType());
						localProperty.setJavaType(targetProperty.getJavaType());
						localProperty.setPathType(targetProperty.getPathType());
						localProperty.setEntity(targetProperty.getEntity());
						association.add(property, localProperty, targetProperty);
					});
		}
		property.setAssociation(association);
	}

	private void processPropertyPathType(VariableElement element, Property property) {
		if (null == property.getJavaType()) {
			return;
		}
		if (null != property.getPathType()) {
			return;
		}

		try {
			Class<?> clz = ClassUtils.forName(property.getJavaType(),
					MybatisAnnotationProcessor.class.getClassLoader());
			com.mysema.codegen.model.Type type = new TypeFactory(Arrays.asList(javax.persistence.Entity.class))
					.get(clz);
			TypeCategory category = type.getCategory();
			switch (category) {
			case COMPARABLE:
				property.setPathType("ComparablePath");
				break;
			case ENUM:
				property.setPathType("EnumPath");
				break;
			case DATE:
				property.setPathType("DatePath");
				break;
			case DATETIME:
				property.setPathType("DateTimePath");
				break;
			case TIME:
				property.setPathType("TimePath");
				break;
			case NUMERIC:
				property.setPathType("NumberPath");
				break;
			case STRING:
				property.setPathType("StringPath");
				break;
			case BOOLEAN:
				property.setPathType("BooleanPath");
			case SIMPLE:
				property.setPathType("SimplePath");
				break;
			// default:
			// property.setPathType("EntityPathBase");
			}
		}
		catch (ClassNotFoundException ex) {
		}
	}

	private void processPropertyJdbcType(VariableElement element, Property property) {
		if (null != property.getJdbcType()) {
			return;
		}
		JdbcType jdbcTypeAnn = element.getAnnotation(JdbcType.class);
		if (null != jdbcTypeAnn && StringUtils.hasText(jdbcTypeAnn.value())) {
			property.setJdbcType(org.apache.ibatis.type.JdbcType.valueOf(jdbcTypeAnn.value()));
			return;
		}
		Lob lobAnn = element.getAnnotation(Lob.class);
		if (null != lobAnn) {
			property.setJdbcType(org.apache.ibatis.type.JdbcType.BLOB);
			return;
		}
		Enumerated enumeratedAnn = element.getAnnotation(Enumerated.class);
		if (null != enumeratedAnn) {
			EnumType enumType = enumeratedAnn.value();
			if (enumType == EnumType.ORDINAL) {
				property.setJdbcType(org.apache.ibatis.type.JdbcType.INTEGER);
			}
			else {
				property.setJdbcType(org.apache.ibatis.type.JdbcType.VARCHAR);
			}
			return;
		}

		if (null == property.getJavaType()) {
			return;
		}
		try {
			Class<?> clz = ClassUtils.forName(property.getJavaType(),
					MybatisAnnotationProcessor.class.getClassLoader());
			Type<?> types = this.javaTypeMapping.getType(clz);
			if (null != types) {
				property.setJdbcType(org.apache.ibatis.type.JdbcType.forCode(types.getSQLTypes()[0]));
			}
		}
		catch (ClassNotFoundException ex) {
		}

	}

	private void processPropertyJavaType(TypeElement entityElement, VariableElement element, Property property) {
		Lob lobAnn = element.getAnnotation(Lob.class);
		if (null != lobAnn) {
			property.setJavaType(Blob.class.getCanonicalName());
			return;
		}
		element.asType().accept(new PropertyTypeVisitor(entityElement), property);
	}

	private void processColumnName(VariableElement element, Property property) {
		Column columnAnn = element.getAnnotation(Column.class);
		if (null != columnAnn && StringUtils.hasText(columnAnn.name())) {
			property.setColumnName(columnAnn.name());
			return;
		}
		OrderColumn orderColumnAnn = element.getAnnotation(OrderColumn.class);
		if (null != orderColumnAnn && StringUtils.hasText(orderColumnAnn.name())) {
			property.setColumnName(orderColumnAnn.name());
			return;
		}

		property.setColumnName(this.mapUnderscoreToCamelCase
				? String.join("_", ParsingUtils.splitCamelCaseToLower(element.getSimpleName().toString()))
				: element.getSimpleName().toString());
	}

	private void processTableName(TypeElement element, Entity entity) {
		Table tableAnn = element.getAnnotation(Table.class);
		if (null != tableAnn) {
			if (StringUtils.hasText(tableAnn.schema())) {
				entity.setSchema(tableAnn.schema());
			}
			if (StringUtils.hasText(tableAnn.name())) {
				entity.setTableName(tableAnn.name());
				return;
			}
		}
		javax.persistence.Entity entityAnn = element.getAnnotation(javax.persistence.Entity.class);
		if (null != entityAnn && StringUtils.hasText(entityAnn.name())) {
			entity.setTableName(entityAnn.name());
			return;
		}

		entity.setTableName(element.getSimpleName().toString());
	}

	public static class PropertyTypeVisitor implements TypeVisitor<PropertyTypeVisitor, Property> {

		private final TypeElement entityElement;

		PropertyTypeVisitor(TypeElement entityElement) {

			this.entityElement = entityElement;
		}

		private void fillJavaType(String t, Property property) {
			property.setJavaType(t);
		}

		@Override
		public PropertyTypeVisitor visit(TypeMirror t, Property property) {
			return this;
		}

		@Override
		public PropertyTypeVisitor visitPrimitive(PrimitiveType t, Property property) {

			ClassType types = null;
			switch (t.getKind()) {
			case BOOLEAN:
				types = Types.BOOLEAN;
				break;
			case BYTE:
				types = Types.BYTE;
				break;
			case SHORT:
				types = Types.SHORT;
				break;
			case INT:
				types = Types.INTEGER;
				break;
			case LONG:
				types = Types.LONG;
				break;
			case CHAR:
				types = Types.CHARACTER;
				break;
			case FLOAT:
				types = Types.FLOAT;
				break;
			case DOUBLE:
				types = Types.DOUBLE;
				break;
			}

			if (null == types) {
				return this;
			}

			property.setJavaType(types.getJavaClass().getSimpleName());

			return this;
		}

		@Override
		public PropertyTypeVisitor visitNull(NullType t, Property property) {
			return this;
		}

		@Override
		public PropertyTypeVisitor visitArray(ArrayType t, Property property) {
			this.fillJavaType(t.toString(), property);
			return this;
		}

		@Override
		public PropertyTypeVisitor visitDeclared(DeclaredType t, Property property) {
			if (t.asElement().getKind() == ElementKind.ENUM) {
				property.setPathType("EnumPath");
			}
			this.fillJavaType(t.toString(), property);
			return this;
		}

		@Override
		public PropertyTypeVisitor visitError(ErrorType t, Property property) {
			return this;
		}

		@Override
		public PropertyTypeVisitor visitTypeVariable(TypeVariable t, Property property) {
			Map<String, Map<String, Object>> genericTypeArgumentInfo = property.getEntity()
					.getGenericTypeArgumentInfo();

			Map<String, Object> info = genericTypeArgumentInfo.get(this.entityElement.getQualifiedName().toString());
			if (null != info) {
				Object type = info.get(t.toString());
				this.fillJavaType(String.valueOf(type), property);
			}
			return this;
		}

		@Override
		public PropertyTypeVisitor visitWildcard(WildcardType t, Property property) {
			return this;
		}

		@Override
		public PropertyTypeVisitor visitExecutable(ExecutableType t, Property property) {
			return this;
		}

		@Override
		public PropertyTypeVisitor visitNoType(NoType t, Property property) {
			return null;
		}

		@Override
		public PropertyTypeVisitor visitUnknown(TypeMirror t, Property property) {
			return null;
		}

		@Override
		public PropertyTypeVisitor visitUnion(UnionType t, Property property) {
			return null;
		}

		@Override
		public PropertyTypeVisitor visitIntersection(IntersectionType t, Property property) {
			return null;
		}

		@Override
		public PropertyTypeVisitor visit(TypeMirror t) {
			return this;
		}

	}

}
