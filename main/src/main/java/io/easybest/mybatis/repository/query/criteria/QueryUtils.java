/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.repository.query.criteria;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.mapping.precompile.Parameter;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * .
 *
 * @author Jarvis Song
 */
public abstract class QueryUtils {

	private static final String POSITIONAL_OR_INDEXED_PARAMETER = "\\?(\\d*+(?![#\\w]))";

	private static final Pattern PARAMETER_BINDING_PATTERN;

	private static final int INDEXED_PARAMETER_GROUP = 4;

	private static final int NAMED_PARAMETER_GROUP = 6;

	private static final int COMPARISION_TYPE_GROUP = 1;
	static {

		List<String> keywords = new ArrayList<>();

		keywords.add("like ");
		keywords.add("in ");

		String builder = "(" + StringUtils.collectionToDelimitedString(keywords, "|") + // keywords
				")?" + "(?: )?" + // some whitespace
				"\\(?" + // optional braces around parameters
				"(" + "%?(" + POSITIONAL_OR_INDEXED_PARAMETER + ")%?" + // position
				// parameter
				// and
				// parameter
				// index
				"|" + // or

				// named parameter and the parameter name
				"%?(" + io.easybest.mybatis.repository.query.QueryUtils.COLON_NO_DOUBLE_COLON
				+ io.easybest.mybatis.repository.query.QueryUtils.IDENTIFIER_GROUP + ")%?" + ")" + "\\)?"; // optional
		// braces
		// around
		// parameters

		PARAMETER_BINDING_PATTERN = Pattern.compile(builder, CASE_INSENSITIVE);
	}

	public static String parse(String sql, ParamValueCallback callback,
			Function<Integer, ParamValue> getParamValueFun) {

		// TODO Parse parameters, such as ?1 or :param
		Matcher matcher = PARAMETER_BINDING_PATTERN.matcher(sql);
		while (matcher.find()) {

			String parameterIndexString = matcher.group(INDEXED_PARAMETER_GROUP);
			// String parameterName = parameterIndexString != null ? null :
			// matcher.group(NAMED_PARAMETER_GROUP);
			Integer parameterIndex = getParameterIndex(parameterIndexString);
			Parameter p;
			if (null != parameterIndex) {
				ParamValue pv = getParamValueFun.apply(parameterIndex - 1);

				if (null != callback) {
					p = callback.apply(pv);
				}
				else {
					p = Parameter.of(pv);
				}

				sql = sql.replace("?" + parameterIndex, p.toString());
			}

		}
		return sql;
	}

	@Nullable
	private static Integer getParameterIndex(@Nullable String parameterIndexString) {

		if (parameterIndexString == null || parameterIndexString.isEmpty()) {
			return null;
		}
		return Integer.valueOf(parameterIndexString);
	}

}
