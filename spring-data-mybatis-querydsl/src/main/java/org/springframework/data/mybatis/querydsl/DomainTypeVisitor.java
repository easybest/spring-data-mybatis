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

import java.util.Arrays;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.persistence.Entity;

import com.mysema.codegen.model.ClassType;
import com.mysema.codegen.model.TypeCategory;
import com.mysema.codegen.model.Types;
import com.querydsl.codegen.TypeFactory;
import com.querydsl.sql.types.Type;
import org.apache.ibatis.type.JdbcType;

import org.springframework.lang.Nullable;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class DomainTypeVisitor implements TypeVisitor<DomainTypeVisitor, Property> {

	private final TypeElement typeElement;

	private final JavaTypeMapping javaTypeMapping;

	public DomainTypeVisitor(TypeElement element) {

		this.typeElement = element;
		this.javaTypeMapping = new JavaTypeMapping();
	}

	@Override
	public DomainTypeVisitor visit(TypeMirror t, Property property) {

		return this;
	}

	@Override
	public DomainTypeVisitor visit(TypeMirror t) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitPrimitive(PrimitiveType t, Property property) {

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

		property.setJavaType(types.getJavaClass());
		this.setPathType(types.getCategory(), property);
		Type<?> tt = this.javaTypeMapping.getType(types.getJavaClass());
		if (null == property.getJdbcType()) {
			property.setJdbcType(JdbcType.forCode(tt.getSQLTypes()[0]));
		}

		return this;
	}

	private void setPathType(TypeCategory category, Property property) {

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
			break;
		// default:
		// property.setPathType("EntityPathBase");
		}
	}

	@Override
	public DomainTypeVisitor visitNull(NullType t, Property property) {
		// property.setJavaType(t.toString());

		return this;
	}

	@Override
	public DomainTypeVisitor visitArray(ArrayType t, Property property) {
		// property.setJavaType(t.toString());

		return this;
	}

	@Override
	public DomainTypeVisitor visitDeclared(DeclaredType t, Property property) {
		// property.setJavaType(t.toString());
		this.fillTypes(t.toString(), property);
		return this;
	}

	@Nullable
	private void fillTypes(String t, Property property) {
		try {
			Class<?> type = Class.forName(t);
			property.setJavaType(type);

			Type<?> types = this.javaTypeMapping.getType(type);
			if (null == property.getJdbcType()) {
				property.setJdbcType(JdbcType.forCode(types.getSQLTypes()[0]));
			}

			com.mysema.codegen.model.Type tt = new TypeFactory(Arrays.asList(Entity.class)).get(type);
			this.setPathType(tt.getCategory(), property);
		}
		catch (ClassNotFoundException ex) {
		}
	}

	@Override
	public DomainTypeVisitor visitError(ErrorType t, Property property) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitTypeVariable(TypeVariable t, Property property) {

		Map<String, Object> map = property.getDomain().getGenericInfo()
				.get(this.typeElement.getQualifiedName().toString());
		if (null != map) {
			Object type = map.get(t.toString());
			if (null != type) {
				this.fillTypes(String.valueOf(type), property);
			}
		}

		return this;
	}

	@Override
	public DomainTypeVisitor visitWildcard(WildcardType t, Property property) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitExecutable(ExecutableType t, Property property) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitNoType(NoType t, Property property) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitUnknown(TypeMirror t, Property property) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitUnion(UnionType t, Property property) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitIntersection(IntersectionType t, Property property) {
		return this;
	}

}
