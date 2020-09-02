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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class GenericUtils {

	private final Elements elementUtils;

	private final Types typeUtils;

	private final ModelUtils modelUtils;

	public GenericUtils(Elements elementUtils, Types typeUtils, ModelUtils modelUtils) {
		this.elementUtils = elementUtils;
		this.typeUtils = typeUtils;
		this.modelUtils = modelUtils;
	}

	Map<String, Map<String, Object>> buildGenericTypeArgumentInfo(DeclaredType dt) {
		Element element = dt.asElement();
		return this.buildGenericTypeArgumentInfo(element, dt);
	}

	public Map<String, Map<String, Object>> buildGenericTypeArgumentInfo(@NonNull Element element) {
		return this.buildGenericTypeArgumentInfo(element, null);
	}

	public Map<String, Map<String, TypeMirror>> buildGenericTypeArgumentElementInfo(@NonNull Element element) {
		Map<String, Map<String, Object>> data = this.buildGenericTypeArgumentInfo(element, null);
		Map<String, Map<String, TypeMirror>> elements = new HashMap<>(data.size());
		for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
			Map<String, Object> value = entry.getValue();
			HashMap<String, TypeMirror> submap = new HashMap<>(value.size());
			for (Map.Entry<String, Object> genericEntry : value.entrySet()) {
				TypeElement te = this.elementUtils.getTypeElement(genericEntry.getValue().toString());
				if (te != null) {
					submap.put(genericEntry.getKey(), te.asType());
				}
			}
			elements.put(entry.getKey(), submap);
		}
		return elements;
	}

	private Map<String, Map<String, Object>> buildGenericTypeArgumentInfo(@NonNull Element element,
			@Nullable DeclaredType dt) {

		Map<String, Map<String, Object>> beanTypeArguments = new HashMap<>();
		if (dt != null) {

			List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
			if (!CollectionUtils.isEmpty(typeArguments)) {
				TypeElement typeElement = (TypeElement) element;

				Map<String, Object> directTypeArguments = this.resolveBoundTypes(dt);
				if (!CollectionUtils.isEmpty(directTypeArguments)) {
					beanTypeArguments.put(typeElement.getQualifiedName().toString(), directTypeArguments);
				}
			}
		}

		if (element instanceof TypeElement) {
			TypeElement typeElement = (TypeElement) element;
			this.populateTypeArguments(typeElement, beanTypeArguments);
		}
		return beanTypeArguments;
	}

	protected TypeMirror interfaceGenericTypeFor(TypeElement element, Class interfaceType) {
		return this.interfaceGenericTypeFor(element, interfaceType.getName());
	}

	protected TypeMirror interfaceGenericTypeFor(TypeElement element, String interfaceName) {
		List<? extends TypeMirror> typeMirrors = this.interfaceGenericTypesFor(element, interfaceName);
		return typeMirrors.isEmpty() ? null : typeMirrors.get(0);
	}

	public List<? extends TypeMirror> interfaceGenericTypesFor(TypeElement element, String interfaceName) {
		for (TypeMirror tm : element.getInterfaces()) {
			DeclaredType declaredType = (DeclaredType) tm;
			Element declaredElement = declaredType.asElement();
			if (declaredElement instanceof TypeElement) {
				TypeElement te = (TypeElement) declaredElement;
				if (interfaceName.equals(te.getQualifiedName().toString())) {
					return declaredType.getTypeArguments();
				}
			}
		}
		return Collections.emptyList();
	}

	protected Optional<TypeMirror> getFirstTypeArgument(TypeMirror type) {
		TypeMirror typeMirror = null;

		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
			if (!CollectionUtils.isEmpty(typeArguments)) {
				typeMirror = typeArguments.get(0);
			}
		}
		return Optional.ofNullable(typeMirror);
	}

	protected Map<String, Object> resolveGenericTypes(TypeMirror type, Map<String, Object> boundTypes) {
		if (type.getKind().isPrimitive() || type.getKind() == TypeKind.VOID || type.getKind() == TypeKind.ARRAY) {
			return Collections.emptyMap();
		}
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			return this.resolveGenericTypes(declaredType, (TypeElement) declaredType.asElement(), boundTypes);
		}
		else if (type instanceof TypeVariable) {
			TypeVariable var = (TypeVariable) type;
			TypeMirror upperBound = var.getUpperBound();
			if (upperBound instanceof DeclaredType) {
				return this.resolveGenericTypes(upperBound, boundTypes);
			}
		}
		return Collections.emptyMap();
	}

	protected Map<String, Object> resolveGenericTypes(DeclaredType type, TypeElement typeElement,
			Map<String, Object> boundTypes) {
		List<? extends TypeMirror> typeArguments = type.getTypeArguments();
		Map<String, Object> resolvedParameters = new LinkedHashMap<>();
		List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();
		if (typeArguments.size() == typeParameters.size()) {
			Iterator<? extends TypeMirror> i = typeArguments.iterator();
			for (TypeParameterElement typeParameter : typeParameters) {
				String parameterName = typeParameter.toString();
				TypeMirror mirror = i.next();

				TypeKind kind = mirror.getKind();
				switch (kind) {
				case TYPEVAR:
					TypeVariable tv = (TypeVariable) mirror;
					if (boundTypes.containsKey(tv.toString())) {
						resolvedParameters.put(parameterName, boundTypes.get(tv.toString()));
					}
					else {
						TypeMirror upperBound = tv.getUpperBound();
						TypeMirror lowerBound = tv.getLowerBound();
						if (upperBound.getKind() != TypeKind.NULL) {
							resolvedParameters.put(parameterName, this.resolveTypeReference(upperBound, boundTypes));
						}
						else if (lowerBound.getKind() != TypeKind.NULL) {
							resolvedParameters.put(parameterName, this.resolveTypeReference(lowerBound, boundTypes));
						}
					}
					continue;
				case ARRAY:
				case BOOLEAN:
				case BYTE:
				case CHAR:
				case DOUBLE:
				case FLOAT:
				case INT:
				case LONG:
				case SHORT:
					this.resolveGenericTypeParameterForPrimitiveOrArray(resolvedParameters, parameterName, mirror,
							boundTypes);
					continue;
				case DECLARED:
					this.resolveGenericTypeParameter(resolvedParameters, parameterName, mirror, boundTypes);
					continue;
				case WILDCARD:
					WildcardType wcType = (WildcardType) mirror;
					TypeMirror extendsBound = wcType.getExtendsBound();
					TypeMirror superBound = wcType.getSuperBound();
					if (extendsBound != null) {
						this.resolveGenericTypeParameter(resolvedParameters, parameterName, extendsBound, boundTypes);
					}
					else if (superBound != null) {
						if (superBound instanceof TypeVariable) {
							TypeVariable superTypeVar = (TypeVariable) superBound;
							final TypeMirror upperBound = superTypeVar.getUpperBound();
							if (upperBound != null && !type.equals(upperBound)) {
								this.resolveGenericTypeParameter(resolvedParameters, parameterName, superBound,
										boundTypes);
							}
						}
						else {
							this.resolveGenericTypeParameter(resolvedParameters, parameterName, superBound, boundTypes);
						}
					}
					else {
						resolvedParameters.put(parameterName, Object.class);
					}
				default:
					// no-op
				}
			}
		}
		return resolvedParameters;
	}

	protected Object resolveTypeReference(TypeMirror mirror) {
		return this.resolveTypeReference(mirror, Collections.emptyMap());
	}

	protected Object resolveTypeReference(TypeMirror mirror, Map<String, Object> boundTypes) {
		TypeKind kind = mirror.getKind();
		switch (kind) {
		case TYPEVAR:
			TypeVariable tv = (TypeVariable) mirror;
			String name = tv.toString();
			if (boundTypes.containsKey(name)) {
				return boundTypes.get(name);
			}
			else {
				return this.resolveTypeReference(tv.getUpperBound(), boundTypes);
			}
		case WILDCARD:
			WildcardType wcType = (WildcardType) mirror;
			TypeMirror extendsBound = wcType.getExtendsBound();
			TypeMirror superBound = wcType.getSuperBound();
			if (extendsBound == null && superBound == null) {
				return Object.class.getName();
			}
			else if (extendsBound != null) {
				return this.resolveTypeReference(this.typeUtils.erasure(extendsBound), boundTypes);
			}
			else if (superBound != null) {
				return this.resolveTypeReference(superBound, boundTypes);
			}
			else {
				return this.resolveTypeReference(this.typeUtils.getWildcardType(extendsBound, superBound), boundTypes);
			}
		case ARRAY:
			ArrayType arrayType = (ArrayType) mirror;
			Object reference = this.resolveTypeReference(arrayType.getComponentType(), boundTypes);
			if (reference instanceof Class) {
				Class componentType = (Class) reference;
				return Array.newInstance(componentType, 0).getClass();
			}
			else if (reference instanceof String) {
				return reference + "[]";
			}
			else {
				return this.modelUtils.resolveTypeReference(mirror);
			}
		default:
			return this.modelUtils.resolveTypeReference(mirror);
		}
	}

	protected DeclaredType resolveTypeVariable(Element element, TypeVariable typeVariable) {
		Element enclosing = element.getEnclosingElement();

		while (enclosing instanceof Parameterizable) {
			Parameterizable parameterizable = (Parameterizable) enclosing;
			String name = typeVariable.toString();
			for (TypeParameterElement typeParameter : parameterizable.getTypeParameters()) {
				if (name.equals(typeParameter.toString())) {
					List<? extends TypeMirror> bounds = typeParameter.getBounds();
					if (bounds.size() == 1) {
						TypeMirror typeMirror = bounds.get(0);
						TypeKind kind = typeMirror.getKind();
						switch (kind) {
						case DECLARED:
							return (DeclaredType) typeMirror;
						case TYPEVAR:
							return this.resolveTypeVariable(element, (TypeVariable) typeMirror);
						default:
							return null;
						}
					}
				}
			}
			enclosing = enclosing.getEnclosingElement();
		}
		return null;
	}

	protected Map<String, Object> resolveBoundTypes(DeclaredType type) {
		Map<String, Object> boundTypes = new LinkedHashMap<>(2);
		TypeElement element = (TypeElement) type.asElement();

		List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
		List<? extends TypeMirror> typeArguments = type.getTypeArguments();
		if (typeArguments.size() == typeParameters.size()) {
			Iterator<? extends TypeMirror> i = typeArguments.iterator();
			for (TypeParameterElement typeParameter : typeParameters) {
				boundTypes.put(typeParameter.toString(), this.resolveTypeReference(i.next(), boundTypes));
			}
		}

		return boundTypes;
	}

	protected Map<String, TypeMirror> resolveBoundGenerics(TypeElement declaringType, TypeMirror returnType,
			Map<String, Map<String, TypeMirror>> genericsInfo) {

		if (returnType instanceof NoType) {
			return Collections.emptyMap();
		}
		else if (returnType instanceof DeclaredType) {
			DeclaredType dt = (DeclaredType) returnType;
			Element e = dt.asElement();
			List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
			if (e instanceof TypeElement) {
				TypeElement typeElement = (TypeElement) e;
				Map<String, TypeMirror> boundGenerics = this.resolveBoundGenerics(declaringType, genericsInfo);
				if (!this.modelUtils.resolveKind(typeElement, ElementKind.ENUM).isPresent()) {
					return this.alignNewGenericsInfo(typeElement.getTypeParameters(), typeArguments, boundGenerics);
				}
			}
		}
		else if (returnType instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) returnType;
			TypeMirror upperBound = tv.getUpperBound();
			Map<String, TypeMirror> boundGenerics = this.resolveBoundGenerics(declaringType, genericsInfo);

			TypeMirror bound = boundGenerics.get(tv.toString());
			if (bound != null) {
				return Collections.singletonMap(tv.toString(), bound);
			}
			else {

				Map<String, TypeMirror> generics = this.resolveBoundGenerics(declaringType, upperBound, genericsInfo);
				if (!generics.isEmpty()) {
					return generics;
				}
				else {
					return this.resolveBoundGenerics(declaringType, tv.getLowerBound(), genericsInfo);
				}
			}
		}

		return Collections.emptyMap();
	}

	private Map<String, TypeMirror> resolveBoundGenerics(TypeElement typeElement,
			Map<String, Map<String, TypeMirror>> genericsInfo) {
		String declaringTypeName = null;
		if (typeElement != null) {
			declaringTypeName = typeElement.getQualifiedName().toString();
		}
		Map<String, TypeMirror> boundGenerics = genericsInfo.get(declaringTypeName);
		if (boundGenerics == null) {
			boundGenerics = Collections.emptyMap();
		}
		return boundGenerics;
	}

	public Map<String, Map<String, TypeMirror>> alignNewGenericsInfo(TypeElement typeElement,
			List<? extends TypeMirror> typeArguments, Map<String, TypeMirror> genericsInfo) {
		String typeName = typeElement.getQualifiedName().toString();
		List<? extends TypeParameterElement> typeParameters = typeElement.getTypeParameters();
		Map<String, TypeMirror> resolved = this.alignNewGenericsInfo(typeParameters, typeArguments, genericsInfo);
		if (!resolved.isEmpty()) {
			return Collections.singletonMap(typeName, resolved);
		}
		return Collections.emptyMap();
	}

	public Map<String, TypeMirror> alignNewGenericsInfo(List<? extends TypeParameterElement> typeParameters,
			List<? extends TypeMirror> typeArguments, Map<String, TypeMirror> genericsInfo) {
		if (typeArguments.size() == typeParameters.size()) {

			Map<String, TypeMirror> resolved = new HashMap<>(typeArguments.size());
			Iterator<? extends TypeMirror> i = typeArguments.iterator();
			for (TypeParameterElement typeParameter : typeParameters) {
				TypeMirror typeParameterMirror = i.next();
				String variableName = typeParameter.getSimpleName().toString();
				this.resolveVariableForMirror(genericsInfo, resolved, variableName, typeParameterMirror);
			}
			return resolved;
		}
		return Collections.emptyMap();
	}

	private void resolveVariableForMirror(Map<String, TypeMirror> genericsInfo, Map<String, TypeMirror> resolved,
			String variableName, TypeMirror mirror) {
		if (mirror instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) mirror;
			this.resolveTypeVariable(genericsInfo, resolved, variableName, tv);
		}
		else {
			if (mirror instanceof WildcardType) {
				WildcardType wt = (WildcardType) mirror;
				TypeMirror extendsBound = wt.getExtendsBound();
				this.resolveVariableForMirror(genericsInfo, resolved, variableName, extendsBound);
			}
			else if (mirror instanceof DeclaredType) {
				resolved.put(variableName, mirror);
			}
			else if (mirror instanceof ArrayType) {
				resolved.put(variableName, mirror);
			}
		}
	}

	private void resolveTypeVariable(Map<String, TypeMirror> genericsInfo, Map<String, TypeMirror> resolved,
			String variableName, TypeVariable variable) {
		String name = variable.toString();
		TypeMirror element = genericsInfo.get(name);
		if (element != null) {
			resolved.put(variableName, element);
		}
		else {
			TypeMirror upperBound = variable.getUpperBound();
			if (upperBound instanceof TypeVariable) {
				this.resolveTypeVariable(genericsInfo, resolved, variableName, (TypeVariable) upperBound);
			}
			else if (upperBound instanceof DeclaredType) {
				resolved.put(variableName, upperBound);
			}
			else {
				TypeMirror lowerBound = variable.getLowerBound();
				if (lowerBound instanceof TypeVariable) {
					this.resolveTypeVariable(genericsInfo, resolved, variableName, (TypeVariable) lowerBound);
				}
				else if (lowerBound instanceof DeclaredType) {
					resolved.put(variableName, lowerBound);
				}
			}
		}
	}

	private void resolveGenericTypeParameter(Map<String, Object> resolvedParameters, String parameterName,
			TypeMirror mirror, Map<String, Object> boundTypes) {
		if (mirror instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) mirror;
			List<? extends TypeMirror> nestedArguments = declaredType.getTypeArguments();
			if (nestedArguments.isEmpty()) {
				resolvedParameters.put(parameterName,
						this.resolveTypeReference(this.typeUtils.erasure(mirror), resolvedParameters));
			}
			else {
				resolvedParameters.put(parameterName,
						Collections.singletonMap(
								this.resolveTypeReference(this.typeUtils.erasure(mirror), resolvedParameters),
								this.resolveGenericTypes(declaredType, boundTypes)));
			}
		}
		else if (mirror instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) mirror;
			String variableName = tv.toString();
			if (boundTypes.containsKey(variableName)) {
				resolvedParameters.put(parameterName, boundTypes.get(variableName));
			}
			else {
				TypeMirror upperBound = tv.getUpperBound();
				if (upperBound instanceof DeclaredType) {
					this.resolveGenericTypeParameter(resolvedParameters, parameterName, upperBound, boundTypes);
				}
			}
		}
	}

	private void populateTypeArguments(TypeElement typeElement, Map<String, Map<String, Object>> typeArguments) {
		TypeElement current = typeElement;
		while (current != null) {

			this.populateTypeArgumentsForInterfaces(typeArguments, current);
			TypeMirror superclass = current.getSuperclass();

			if (superclass.getKind() == TypeKind.NONE) {
				current = null;
			}
			else {
				if (superclass instanceof DeclaredType) {
					DeclaredType dt = (DeclaredType) superclass;
					List<? extends TypeMirror> superArguments = dt.getTypeArguments();

					Element te = dt.asElement();
					if (te instanceof TypeElement) {
						TypeElement child = current;
						current = (TypeElement) te;
						if (!CollectionUtils.isEmpty(superArguments)) {
							Map<String, Object> boundTypes = typeArguments.get(child.getQualifiedName().toString());
							if (boundTypes == null) {
								boundTypes = Collections.emptyMap();
							}
							Map<String, Object> types = this.resolveGenericTypes(dt, current, boundTypes);

							String name = current.getQualifiedName().toString();
							typeArguments.put(name, types);
						}

					}
					else {
						break;
					}
				}
				else {
					break;
				}
			}
		}
	}

	private void populateTypeArgumentsForInterfaces(Map<String, Map<String, Object>> typeArguments, TypeElement child) {
		for (TypeMirror anInterface : child.getInterfaces()) {
			if (anInterface instanceof DeclaredType) {
				DeclaredType declaredType = (DeclaredType) anInterface;
				Element element = declaredType.asElement();
				if (element instanceof TypeElement) {
					TypeElement te = (TypeElement) element;
					String name = te.getQualifiedName().toString();
					if (!typeArguments.containsKey(name)) {
						Map<String, Object> boundTypes = typeArguments.get(child.getQualifiedName().toString());
						if (boundTypes == null) {
							boundTypes = Collections.emptyMap();
						}
						Map<String, Object> types = this.resolveGenericTypes(declaredType, te, boundTypes);
						typeArguments.put(name, types);
					}
					this.populateTypeArgumentsForInterfaces(typeArguments, te);
				}
			}
		}
	}

	private void resolveGenericTypeParameterForPrimitiveOrArray(Map<String, Object> resolvedParameters,
			String parameterName, TypeMirror mirror, Map<String, Object> boundTypes) {
		resolvedParameters.put(parameterName,
				Collections.singletonMap(this.resolveTypeReference(this.typeUtils.erasure(mirror), resolvedParameters),
						this.resolveGenericTypes(mirror, boundTypes)));
	}

}
