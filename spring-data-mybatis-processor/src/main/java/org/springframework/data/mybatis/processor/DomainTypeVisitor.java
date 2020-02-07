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
package org.springframework.data.mybatis.processor;

import java.util.HashMap;
import java.util.Map;

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

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class DomainTypeVisitor implements TypeVisitor<DomainTypeVisitor, ColumnMeta> {

	/**
	 * Packing types.
	 */
	public static Map<String, String> PACKING = new HashMap<>();
	static {
		// boolean, byte, short, int, long, char, float, and double
		PACKING.put("boolean", "Boolean");
		PACKING.put("byte", "Byte");
		PACKING.put("short", "Short");
		PACKING.put("int", "Integer");
		PACKING.put("long", "Long");
		PACKING.put("char", "Character");
		PACKING.put("float", "Float");
		PACKING.put("double", "Double");
		PACKING.put("String", "String");
		PACKING.put("java.util.Date", "java.util.Date");
	}

	@Override
	public DomainTypeVisitor visit(TypeMirror t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitPrimitive(PrimitiveType t, ColumnMeta columnMeta) {
		String packType = PACKING.get(t.toString());
		columnMeta.setType((null != packType) ? packType : t.toString());
		return this;
	}

	@Override
	public DomainTypeVisitor visitNull(NullType t, ColumnMeta columnMeta) {
		columnMeta.setType(t.toString());
		return this;
	}

	@Override
	public DomainTypeVisitor visitArray(ArrayType t, ColumnMeta columnMeta) {
		columnMeta.setType(t.toString());
		return this;
	}

	@Override
	public DomainTypeVisitor visitDeclared(DeclaredType t, ColumnMeta columnMeta) {
		columnMeta.setType(t.toString());
		return this;
	}

	@Override
	public DomainTypeVisitor visitError(ErrorType t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitTypeVariable(TypeVariable t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitWildcard(WildcardType t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitExecutable(ExecutableType t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitNoType(NoType t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitUnknown(TypeMirror t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitUnion(UnionType t, ColumnMeta columnMeta) {
		return this;
	}

	@Override
	public DomainTypeVisitor visitIntersection(IntersectionType t, ColumnMeta columnMeta) {
		return this;
	}

}
