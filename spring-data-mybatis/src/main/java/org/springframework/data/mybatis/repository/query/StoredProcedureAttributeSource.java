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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.StoredProcedureParameter;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mybatis.repository.Procedure;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A factory class for {@link StoredProcedureAttributes}.
 *
 * @author JARVIS SONG
 */
enum StoredProcedureAttributeSource {

	INSTANCE;

	public StoredProcedureAttributes createFrom(Method method, MybatisEntityMetadata<?> entityMetadata) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(entityMetadata, "EntityMetadata must not be null!");

		Procedure procedure = AnnotatedElementUtils.findMergedAnnotation(method, Procedure.class);
		Assert.notNull(procedure, "Method must have an @Procedure annotation!");

		NamedStoredProcedureQuery namedStoredProc = tryFindAnnotatedNamedStoredProcedureQuery(method, entityMetadata,
				procedure);

		if (namedStoredProc != null) {
			return newProcedureAttributesFrom(method, namedStoredProc, procedure);
		}

		String procedureName = deriveProcedureNameFrom(method, procedure);
		if (StringUtils.isEmpty(procedureName)) {
			throw new IllegalArgumentException(
					"Could not determine name of procedure for @Procedure annotated method: " + method);
		}

		return new StoredProcedureAttributes(procedureName, procedure.outputParameterName(), method.getReturnType());
	}

	private String deriveProcedureNameFrom(Method method, Procedure procedure) {

		if (StringUtils.hasText(procedure.value())) {
			return procedure.value();
		}

		String procedureName = procedure.procedureName();
		return StringUtils.hasText(procedureName) ? procedureName : method.getName();
	}

	private StoredProcedureAttributes newProcedureAttributesFrom(Method method,
			NamedStoredProcedureQuery namedStoredProc, Procedure procedure) {

		List<String> outputParameterNames = new ArrayList<>();
		List<Class<?>> outputParameterTypes = new ArrayList<>();

		if (!procedure.outputParameterName().isEmpty()) {
			// we give the output parameter definition from the @Procedure annotation
			// precedence
			outputParameterNames.add(procedure.outputParameterName());
		}
		else {

			// try to discover the output parameter
			List<StoredProcedureParameter> outputParameters = extractOutputParametersFrom(namedStoredProc);

			for (StoredProcedureParameter outputParameter : outputParameters) {
				outputParameterNames.add(outputParameter.name());
				outputParameterTypes.add(outputParameter.type());
			}
		}

		if (outputParameterTypes.isEmpty()) {
			outputParameterTypes.add(method.getReturnType());
		}

		return new StoredProcedureAttributes(namedStoredProc.name(), outputParameterNames, outputParameterTypes, true);
	}

	private List<StoredProcedureParameter> extractOutputParametersFrom(NamedStoredProcedureQuery namedStoredProc) {

		List<StoredProcedureParameter> outputParameters = new ArrayList<StoredProcedureParameter>();

		for (StoredProcedureParameter param : namedStoredProc.parameters()) {

			switch (param.mode()) {
			case OUT:
			case INOUT:
			case REF_CURSOR:
				outputParameters.add(param);
				break;
			case IN:
			default:
				continue;
			}
		}

		return outputParameters;
	}

	@Nullable
	private NamedStoredProcedureQuery tryFindAnnotatedNamedStoredProcedureQuery(Method method,
			MybatisEntityMetadata<?> entityMetadata, Procedure procedure) {

		Assert.notNull(method, "Method must not be null!");
		Assert.notNull(entityMetadata, "EntityMetadata must not be null!");
		Assert.notNull(procedure, "Procedure must not be null!");

		Class<?> entityType = entityMetadata.getJavaType();

		List<NamedStoredProcedureQuery> queries = collectNamedStoredProcedureQueriesFrom(entityType);

		if (queries.isEmpty()) {
			return null;
		}

		String namedProcedureName = derivedNamedProcedureNameFrom(method, entityMetadata, procedure);

		for (NamedStoredProcedureQuery query : queries) {

			if (query.name().equals(namedProcedureName)) {
				return query;
			}
		}

		return null;
	}

	private String derivedNamedProcedureNameFrom(Method method, MybatisEntityMetadata<?> entityMetadata,
			Procedure procedure) {
		return StringUtils.hasText(procedure.name()) ? procedure.name()
				: entityMetadata.getEntityName() + "." + method.getName();
	}

	private List<NamedStoredProcedureQuery> collectNamedStoredProcedureQueriesFrom(Class<?> entityType) {

		List<NamedStoredProcedureQuery> queries = new ArrayList<NamedStoredProcedureQuery>();

		NamedStoredProcedureQueries namedQueriesAnnotation = AnnotatedElementUtils.findMergedAnnotation(entityType,
				NamedStoredProcedureQueries.class);
		if (namedQueriesAnnotation != null) {
			queries.addAll(Arrays.asList(namedQueriesAnnotation.value()));
		}

		NamedStoredProcedureQuery namedQueryAnnotation = AnnotatedElementUtils.findMergedAnnotation(entityType,
				NamedStoredProcedureQuery.class);
		if (namedQueryAnnotation != null) {
			queries.add(namedQueryAnnotation);
		}

		return queries;
	}

}
