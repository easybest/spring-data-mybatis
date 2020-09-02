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
import javax.lang.model.element.Name;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public final class JavaModelUtils {

	private JavaModelUtils() {
	}

	public static Optional<ElementKind> resolveKind(Element element) {
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

	public static Optional<ElementKind> resolveKind(Element element, ElementKind expected) {
		final Optional<ElementKind> elementKind = resolveKind(element);
		if (elementKind.isPresent() && elementKind.get() == expected) {
			return elementKind;
		}
		return Optional.empty();
	}

	public static boolean isInterface(Element element) {
		return resolveKind(element, ElementKind.INTERFACE).isPresent();
	}

	public static boolean isClass(Element element) {
		return resolveKind(element, ElementKind.CLASS).isPresent();
	}

	public static boolean isEnum(Element element) {
		return resolveKind(element, ElementKind.ENUM).isPresent();
	}

	public static boolean isClassOrInterface(Element element) {
		return isInterface(element) || isClass(element);
	}

	public static String getClassName(TypeElement typeElement) {
		Name qualifiedName = typeElement.getQualifiedName();
		NestingKind nestingKind;
		try {
			nestingKind = typeElement.getNestingKind();
			if (nestingKind == NestingKind.MEMBER) {
				TypeElement enclosingElement = typeElement;
				StringBuilder builder = new StringBuilder();
				while (nestingKind == NestingKind.MEMBER) {
					builder.insert(0, '$').insert(1, enclosingElement.getSimpleName());
					Element enclosing = enclosingElement.getEnclosingElement();

					if (enclosing instanceof TypeElement) {
						enclosingElement = (TypeElement) enclosing;
						nestingKind = enclosingElement.getNestingKind();
					}
					else {
						break;
					}
				}
				Name enclosingName = enclosingElement.getQualifiedName();
				return enclosingName.toString() + builder;
			}
			else {
				return qualifiedName.toString();
			}
		}
		catch (RuntimeException ex) {
			return qualifiedName.toString();
		}
	}

	public static String getClassArrayName(TypeElement typeElement) {
		return "[L" + getClassName(typeElement) + ";";
	}

}
