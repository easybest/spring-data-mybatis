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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.StoredProcedureQuery;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Stored procedure configuration for JPA 2.1 {@link StoredProcedureQuery}s.
 *
 * @author JARVIS SONG
 */
class StoredProcedureAttributes {

	// A synthetic output parameter name to be used in case of derived stored procedures
	// and named parameters
	static final String SYNTHETIC_OUTPUT_PARAMETER_NAME = "out";

	private final boolean namedStoredProcedure;

	private final String procedureName;

	private final List<String> outputParameterNames;

	private final List<Class<?>> outputParameterTypes;

	/**
	 * Creates a new {@link StoredProcedureAttributes}.
	 * @param procedureName must not be {@literal null}.
	 * @param outputParameterName may be {@literal null}.
	 * @param outputParameterType must not be {@literal null}.
	 */
	StoredProcedureAttributes(String procedureName, @Nullable String outputParameterName,
			Class<?> outputParameterType) {
		this(procedureName, Collections.singletonList(outputParameterName),
				Collections.singletonList(outputParameterType), false);
	}

	/**
	 * Creates a new {@link StoredProcedureAttributes}.
	 * @param procedureName must not be {@literal null}.
	 * @param outputParameterNames may be empty, but not {@literal null}.
	 * @param outputParameterTypes must not be empty, and cannot be a single element of
	 * {@literal null}.
	 * @param namedStoredProcedure flag signaling if the stored procedure has a name.
	 */
	StoredProcedureAttributes(String procedureName, List<String> outputParameterNames,
			List<Class<?>> outputParameterTypes, boolean namedStoredProcedure) {

		Assert.notNull(procedureName, "ProcedureName must not be null!");
		Assert.notNull(outputParameterNames, "OutputParameterNames must not be null!");
		Assert.notEmpty(outputParameterTypes, "OutputParameterTypes must not be empty!");
		Assert.isTrue(outputParameterTypes.size() != 1 || outputParameterTypes.get(0) != null,
				"OutputParameterTypes must not have size 1 with a null value");

		this.procedureName = procedureName;
		this.outputParameterNames = namedStoredProcedure ? outputParameterNames
				: this.completeOutputParameterNames(outputParameterNames);
		this.outputParameterTypes = outputParameterTypes;
		this.namedStoredProcedure = namedStoredProcedure;
	}

	private List<String> completeOutputParameterNames(List<String> outputParameterNames) {

		return IntStream.range(0, outputParameterNames.size()) //
				.mapToObj(i -> this.completeOutputParameterName(i, outputParameterNames.get(i))) //
				.collect(Collectors.toList());
	}

	private String completeOutputParameterName(int i, String paramName) {

		return StringUtils.hasText(paramName) //
				? paramName //
				: this.createSyntheticParameterName(i);
	}

	private String createSyntheticParameterName(int i) {
		return SYNTHETIC_OUTPUT_PARAMETER_NAME + ((i != 0) ? i : "");
	}

	public String getProcedureName() {
		return this.procedureName;
	}

	public List<String> getOutputParameterNames() {
		return this.outputParameterNames;
	}

	public List<Class<?>> getOutputParameterTypes() {
		return this.outputParameterTypes;
	}

	public boolean isNamedStoredProcedure() {
		return this.namedStoredProcedure;
	}

	public boolean hasReturnValue() {
		return !(this.outputParameterTypes.size() == 1 && (void.class.equals(this.outputParameterTypes.get(0))
				|| Void.class.equals(this.outputParameterTypes.get(0))));
	}

}
