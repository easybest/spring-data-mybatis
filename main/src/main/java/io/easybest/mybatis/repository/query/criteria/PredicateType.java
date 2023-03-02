/*
 * Copyright 2019-2023 the original author or authors.
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

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * .
 *
 * @author Jarvis Song
 */
public enum PredicateType {

	/**
	 * CUSTOM.
	 */

	CUSTOM(0),

	/**
	 * .
	 */
	BETWEEN(2, "IsBetween", "Between"),
	/**
	 * .
	 */
	IS_NOT_NULL(0, "IsNotNull", "NotNull"),
	/**
	 * .
	 */
	IS_NULL(0, "IsNull", "Null"),
	/**
	 * .
	 */
	LESS_THAN("IsLessThan", "LessThan"),
	/**
	 * .
	 */
	LESS_THAN_EQUAL("IsLessThanEqual", "LessThanEqual"),
	/**
	 * .
	 */
	GREATER_THAN("IsGreaterThan", "GreaterThan"),
	/**
	 * .
	 */
	GREATER_THAN_EQUAL("IsGreaterThanEqual", "GreaterThanEqual"), BEFORE("IsBefore", "Before"),
	/**
	 * .
	 */
	AFTER("IsAfter", "After"),
	/**
	 * .
	 */
	NOT_LIKE("IsNotLike", "NotLike"),
	/**
	 * .
	 */
	LIKE("IsLike", "Like"),
	/**
	 * .
	 */
	STARTING_WITH("IsStartingWith", "StartingWith", "StartsWith"),
	/**
	 * .
	 */
	ENDING_WITH("IsEndingWith", "EndingWith", "EndsWith"),
	/**
	 * .
	 */
	IS_NOT_EMPTY(0, "IsNotEmpty", "NotEmpty"),
	/**
	 * .
	 */
	IS_EMPTY(0, "IsEmpty", "Empty"),
	/**
	 * .
	 */
	NOT_CONTAINING("IsNotContaining", "NotContaining", "NotContains"),
	/**
	 * .
	 */
	CONTAINING("IsContaining", "Containing", "Contains"),
	/**
	 * .
	 */
	NOT_IN("IsNotIn", "NotIn"), IN("IsIn", "In"),
	/**
	 * .
	 */
	NEAR("IsNear", "Near"),
	/**
	 * .
	 */
	WITHIN("IsWithin", "Within"),
	/**
	 * .
	 */
	REGEX("MatchesRegex", "Matches", "Regex"),
	/**
	 * .
	 */
	EXISTS(0, "Exists"),
	/**
	 * .
	 */
	TRUE(0, "IsTrue", "True"),
	/**
	 * .
	 */
	FALSE(0, "IsFalse", "False"),
	/**
	 * .
	 */
	NEGATING_SIMPLE_PROPERTY("IsNot", "Not"),
	/**
	 * .
	 */
	SIMPLE_PROPERTY("Is", "Equals");

	private static final List<PredicateType> ALL = Arrays.asList(IS_NOT_NULL, IS_NULL, BETWEEN, LESS_THAN,
			LESS_THAN_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, BEFORE, AFTER, NOT_LIKE, LIKE, STARTING_WITH,
			ENDING_WITH, IS_NOT_EMPTY, IS_EMPTY, NOT_CONTAINING, CONTAINING, NOT_IN, IN, NEAR, WITHIN, REGEX, EXISTS,
			TRUE, FALSE, NEGATING_SIMPLE_PROPERTY, SIMPLE_PROPERTY);

	/**
	 * .
	 */
	public static final Collection<String> ALL_KEYWORDS;

	static {
		List<String> allKeywords = new ArrayList<>();
		for (PredicateType type : ALL) {
			allKeywords.addAll(type.keywords);
		}
		ALL_KEYWORDS = Collections.unmodifiableList(allKeywords);
	}

	private final List<String> keywords;

	private final int numberOfArguments;

	PredicateType(int numberOfArguments, String... keywords) {

		this.numberOfArguments = numberOfArguments;
		this.keywords = Arrays.asList(keywords);
	}

	PredicateType(String... keywords) {
		this(1, keywords);
	}

	public static PredicateType fromProperty(String rawProperty) {

		for (PredicateType type : ALL) {
			if (type.supports(rawProperty)) {
				return type;
			}
		}

		return SIMPLE_PROPERTY;
	}

	public Collection<String> getKeywords() {
		return Collections.unmodifiableList(this.keywords);
	}

	private boolean supports(String property) {

		for (String keyword : this.keywords) {
			if (property.endsWith(keyword)) {
				return true;
			}
		}

		return false;
	}

	public int getNumberOfArguments() {
		return this.numberOfArguments;
	}

	public String extractProperty(String part) {

		String candidate = Introspector.decapitalize(part);

		for (String keyword : this.keywords) {
			if (candidate.endsWith(keyword)) {
				return candidate.substring(0, candidate.length() - keyword.length());
			}
		}

		return candidate;
	}

	@Override
	public String toString() {
		return String.format("%s (%s): %s", this.name(), this.getNumberOfArguments(), this.getKeywords());
	}

}
