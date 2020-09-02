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

import java.util.Optional;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class ModelUtils {

	private final Elements elementUtils;

	private final Types typeUtils;

	public ModelUtils(Elements elementUtils, Types typeUtils) {
		this.elementUtils = elementUtils;
		this.typeUtils = typeUtils;
	}

	String resolveTypeName(TypeMirror type) {
		Object reference = this.resolveTypeReference(type);
		if (reference instanceof Class) {
			return ((Class) reference).getName();
		}
		return reference.toString();
	}

	Object resolveTypeReference(TypeMirror type) {
		Object result = Void.TYPE;
		if (type.getKind().isPrimitive()) {
			result = this.resolvePrimitiveTypeReference(type);
		}
		else if (type.getKind() == TypeKind.ARRAY) {
			ArrayType arrayType = (ArrayType) type;
			TypeMirror componentType = arrayType.getComponentType();
			if (componentType.getKind().isPrimitive()) {
				result = this.classOfPrimitiveArrayFor(this.resolvePrimitiveTypeReference(componentType).getName());
			}
			else {
				final TypeMirror erased = this.typeUtils.erasure(componentType);
				final Element e = this.typeUtils.asElement(erased);
				if (e instanceof TypeElement) {
					result = this.resolveTypeReferenceForTypeElement((TypeElement) e) + "[]";
				}
			}
		}
		else if (type.getKind() != TypeKind.VOID && type.getKind() != TypeKind.ERROR) {
			final TypeMirror erased = this.typeUtils.erasure(type);
			final Element element = this.typeUtils.asElement(erased);
			if (element instanceof TypeElement) {
				TypeElement te = (TypeElement) element;
				result = this.resolveTypeReferenceForTypeElement(te);
			}
		}
		return result;
	}

	String resolveTypeReferenceForTypeElement(TypeElement typeElement) {
		return JavaModelUtils.getClassName(typeElement);
	}

	private Class resolvePrimitiveTypeReference(TypeMirror type) {
		Class result;
		if (type instanceof DeclaredType) {
			DeclaredType dt = (DeclaredType) type;
			result = this.classOfPrimitiveFor(dt.asElement().getSimpleName().toString());
		}
		else {
			if (type instanceof PrimitiveType) {
				PrimitiveType pt = (PrimitiveType) type;
				TypeKind kind = pt.getKind();
				switch (kind) {
				case VOID:
					return void.class;
				case INT:
					return int.class;
				case BYTE:
					return byte.class;
				case CHAR:
					return char.class;
				case LONG:
					return long.class;
				case FLOAT:
					return float.class;
				case SHORT:
					return short.class;
				case DOUBLE:
					return double.class;
				case BOOLEAN:
					return boolean.class;
				default:
					result = this.classOfPrimitiveFor(type.toString());
				}
			}
			else {
				result = this.classOfPrimitiveFor(type.toString());
			}
		}
		return result;
	}

	public Optional<ElementKind> resolveKind(Element element, ElementKind expected) {
		final Optional<ElementKind> elementKind = this.resolveKind(element);
		if (elementKind.isPresent() && elementKind.get() == expected) {
			return elementKind;
		}
		return Optional.empty();
	}

	public Optional<ElementKind> resolveKind(Element element) {
		if (element != null) {
			try {
				final ElementKind kind = element.getKind();
				return Optional.of(kind);
			}
			catch (Exception ex) {
				// ignore and fall through to empty
			}
		}
		return Optional.empty();
	}

	Class<?> classOfPrimitiveFor(String primitiveType) {
		return ClassUtils.getPrimitiveType(primitiveType)
				.orElseThrow(() -> new IllegalArgumentException("Unknown primitive type: " + primitiveType));
	}

	Class<?> classOfPrimitiveArrayFor(String primitiveType) {
		return ClassUtils.arrayTypeForPrimitive(primitiveType)
				.orElseThrow(() -> new IllegalArgumentException("Unsupported primitive type " + primitiveType));
	}

}
