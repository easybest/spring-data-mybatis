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
package org.springframework.data.mybatis.repository.query;

import javax.persistence.Query;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Streamable;

/**
 * A {@link AbstractMybatisQuery} implementation based on a {@link PartTree}.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class PartTreeMybatisQuery extends AbstractMybatisQuery {

	private final PartTree tree;

	private final MybatisParameters parameters;

	private final EscapeCharacter escape;

	PartTreeMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method) {
		this(sqlSessionTemplate, method, EscapeCharacter.DEFAULT);
	}

	PartTreeMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method, EscapeCharacter escape) {
		super(sqlSessionTemplate, method);
		this.escape = escape;
		Class<?> domainClass = method.getEntityInformation().getJavaType();
		this.parameters = method.getParameters();

		boolean recreationRequired = this.parameters.hasDynamicProjection()
				|| this.parameters.potentiallySortsDynamically();
		try {
			this.tree = new PartTree(method.getName(), domainClass);
			validate(this.tree, this.parameters, method.toString());
		}
		catch (Exception ex) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s! %s", method, ex.getMessage()), ex);
		}
	}

	@Override
	public Object execute(Object[] parameters) {
		return null;
	}

	@Override
	protected Query doCreateQuery(MybatisParametersParameterAccessor parameters) {
		return null;
	}

	private static void validate(PartTree tree, MybatisParameters parameters, String methodName) {
		int argCount = 0;
		Iterable<Part> parts = () -> tree.stream().flatMap(Streamable::stream).iterator();
		for (Part part : parts) {
			int numberOfArguments = part.getNumberOfArguments();
			for (int i = 0; i < numberOfArguments; i++) {
				throwExceptionOnArgumentMismatch(methodName, part, parameters, argCount);
				argCount++;
			}
		}
	}

	private static void throwExceptionOnArgumentMismatch(String methodName, Part part, MybatisParameters parameters,
			int index) {

		Part.Type type = part.getType();
		String property = part.getProperty().toDotPath();

		if (!parameters.getBindableParameters().hasParameterAt(index)) {
			throw new IllegalStateException(String.format(
					"Method %s expects at least %d arguments but only found %d. This leaves an operator of type %s for property %s unbound.",
					methodName, index + 1, index, type.name(), property));
		}

		MybatisParameters.MybatisParameter parameter = parameters.getBindableParameter(index);

		if (expectsCollection(type) && !parameterIsCollectionLike(parameter)) {
			throw new IllegalStateException(
					wrongParameterTypeMessage(methodName, property, type, "Collection", parameter));
		}
		else if (!expectsCollection(type) && !parameterIsScalarLike(parameter)) {
			throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "scalar", parameter));
		}
	}

	private static String wrongParameterTypeMessage(String methodName, String property, Part.Type operatorType,
			String expectedArgumentType, MybatisParameters.MybatisParameter parameter) {

		return String.format("Operator %s on %s requires a %s argument, found %s in method %s.", operatorType.name(),
				property, expectedArgumentType, parameter.getType(), methodName);
	}

	private static boolean parameterIsCollectionLike(MybatisParameters.MybatisParameter parameter) {
		return Iterable.class.isAssignableFrom(parameter.getType()) || parameter.getType().isArray();
	}

	private static boolean parameterIsScalarLike(MybatisParameters.MybatisParameter parameter) {
		return !Iterable.class.isAssignableFrom(parameter.getType());
	}

	private static boolean expectsCollection(Part.Type type) {
		return type == Part.Type.IN || type == Part.Type.NOT_IN;
	}

}
