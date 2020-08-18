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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Query utility.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public abstract class QueryUtils {

	/**
	 * count query string.
	 */
	public static final String COUNT_QUERY_STRING = "select count(%s) from %s x";

	/**
	 * delete sql string.
	 */
	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";

	// Used Regex/Unicode categories (see
	// https://www.unicode.org/reports/tr18/#General_Category_Property):
	// Z Separator
	// Cc Control
	// Cf Format
	// Punct Punctuation
	private static final String IDENTIFIER = "[._$[\\P{Z}&&\\P{Cc}&&\\P{Cf}&&\\P{Punct}]]+";
	static final String COLON_NO_DOUBLE_COLON = "(?<![:\\\\]):";
	static final String IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER);

	private static final String COUNT_REPLACEMENT_TEMPLATE = "select count(%s) $5$6$7";

	private static final String SIMPLE_COUNT_VALUE = "$2";

	private static final String COMPLEX_COUNT_VALUE = "$3 $6";

	private static final String COMPLEX_COUNT_LAST_VALUE = "$6";

	private static final String ORDER_BY_PART = "(?iu)\\s+order\\s+by\\s+.*";

	private static final Pattern ALIAS_MATCH;

	private static final Pattern COUNT_MATCH;

	private static final Pattern PROJECTION_CLAUSE = Pattern.compile("select\\s+(.+)\\s+from",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern NO_DIGITS = Pattern.compile("\\D+");

	private static final String JOIN = "join\\s+(fetch\\s+)?" + IDENTIFIER + "\\s+(as\\s+)?" + IDENTIFIER_GROUP;

	private static final Pattern JOIN_PATTERN = Pattern.compile(JOIN, Pattern.CASE_INSENSITIVE);

	private static final String EQUALS_CONDITION_STRING = "%s.%s = :%s";

	private static final Pattern ORDER_BY = Pattern.compile(".*order\\s+by\\s+.*", Pattern.CASE_INSENSITIVE);

	private static final Pattern NAMED_PARAMETER = Pattern
			.compile(COLON_NO_DOUBLE_COLON + IDENTIFIER + "|#" + IDENTIFIER, Pattern.CASE_INSENSITIVE);

	private static final Pattern CONSTRUCTOR_EXPRESSION;

	private static final Map<Attribute.PersistentAttributeType, Class<? extends Annotation>> ASSOCIATION_TYPES;

	private static final int QUERY_JOIN_ALIAS_GROUP_INDEX = 3;

	private static final int VARIABLE_NAME_GROUP_INDEX = 4;

	private static final int COMPLEX_COUNT_FIRST_INDEX = 3;

	private static final Pattern PUNCTATION_PATTERN = Pattern.compile(".*((?![._])[\\p{Punct}|\\s])");

	private static final Pattern FUNCTION_PATTERN;

	private static final Pattern FIELD_ALIAS_PATTERN;

	private static final String UNSAFE_PROPERTY_REFERENCE = "Sort expression '%s' must only contain property references or "
			+ "aliases used in the select clause. If you really want to use something other than that for sorting, please use "
			+ "JpaSort.unsafe(…)!";

	static {

		StringBuilder builder = new StringBuilder();
		builder.append("(?<=from)"); // from as starting delimiter
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append(IDENTIFIER_GROUP); // Entity name, can be qualified (any
		builder.append("(?:\\sas)*"); // exclude possible "as" keyword
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append("(?!(?:where|group\\s*by|order\\s*by))(\\w+)"); // the actual alias

		ALIAS_MATCH = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);

		builder = new StringBuilder();
		builder.append("(select\\s+((distinct)?((?s).+?)?)\\s+)?(from\\s+");
		builder.append(IDENTIFIER);
		builder.append("(?:\\s+as)?\\s+)");
		builder.append(IDENTIFIER_GROUP);
		builder.append("(.*)");

		COUNT_MATCH = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);

		Map<Attribute.PersistentAttributeType, Class<? extends Annotation>> persistentAttributeTypes = new HashMap<>();
		persistentAttributeTypes.put(Attribute.PersistentAttributeType.ONE_TO_ONE, OneToOne.class);
		persistentAttributeTypes.put(Attribute.PersistentAttributeType.ONE_TO_MANY, null);
		persistentAttributeTypes.put(Attribute.PersistentAttributeType.MANY_TO_ONE, ManyToOne.class);
		persistentAttributeTypes.put(Attribute.PersistentAttributeType.MANY_TO_MANY, null);
		persistentAttributeTypes.put(Attribute.PersistentAttributeType.ELEMENT_COLLECTION, null);

		ASSOCIATION_TYPES = Collections.unmodifiableMap(persistentAttributeTypes);

		builder = new StringBuilder();
		builder.append("select");
		builder.append("\\s+"); // at least one space separating
		builder.append("(.*\\s+)?"); // anything in between (e.g. distinct) at least one
										// space separating
		builder.append("new");
		builder.append("\\s+"); // at least one space separating
		builder.append(IDENTIFIER);
		builder.append("\\s*"); // zero to unlimited space separating
		builder.append("\\(");
		builder.append(".*");
		builder.append("\\)");

		CONSTRUCTOR_EXPRESSION = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE + Pattern.DOTALL);

		builder = new StringBuilder();
		// any function call including parameters within the brackets
		builder.append("\\w+\\s*\\([\\w\\.,\\s'=]+\\)");
		// the potential alias
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))");

		FUNCTION_PATTERN = Pattern.compile(builder.toString());

		builder = new StringBuilder();
		builder.append("\\s+"); // at least one space
		builder.append("([^\\s\\(\\)]+)"); // No white char no bracket
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))"); // the potential alias

		FIELD_ALIAS_PATTERN = Pattern.compile(builder.toString());

	}

	private QueryUtils() {

	}

	public static String getExistsQueryString(String entityName, String countQueryPlaceHolder,
			Iterable<String> idAttributes) {

		String whereClause = Streamable.of(idAttributes).stream() //
				.map(idAttribute -> String.format(EQUALS_CONDITION_STRING, "x", idAttribute, idAttribute)) //
				.collect(Collectors.joining(" AND ", " WHERE ", ""));

		return String.format(COUNT_QUERY_STRING, countQueryPlaceHolder, entityName) + whereClause;
	}

	public static String getQueryString(String template, String entityName) {

		Assert.hasText(entityName, "Entity name must not be null or empty!");

		return String.format(template, entityName);
	}

	public static String applySorting(String query, Sort sort) {
		return applySorting(query, sort, detectAlias(query));
	}

	public static String applySorting(String query, Sort sort, @Nullable String alias) {

		Assert.hasText(query, "Query must not be null or empty!");

		if (sort.isUnsorted()) {
			return query;
		}

		StringBuilder builder = new StringBuilder(query);

		if (!ORDER_BY.matcher(query).matches()) {
			builder.append(" order by ");
		}
		else {
			builder.append(", ");
		}

		Set<String> joinAliases = getOuterJoinAliases(query);
		Set<String> selectionAliases = getFunctionAliases(query);
		selectionAliases.addAll(getFieldAliases(query));

		for (Sort.Order order : sort) {
			builder.append(getOrderClause(joinAliases, selectionAliases, alias, order)).append(", ");
		}

		builder.delete(builder.length() - 2, builder.length());

		return builder.toString();
	}

	private static String getOrderClause(Set<String> joinAliases, Set<String> selectionAlias, @Nullable String alias,
			Sort.Order order) {

		String property = order.getProperty();

		checkSortExpression(order);

		if (selectionAlias.contains(property)) {
			return String.format("%s %s", property, toJpaDirection(order));
		}

		boolean qualifyReference = !property.contains("("); // ( indicates a function

		for (String joinAlias : joinAliases) {
			if (property.startsWith(joinAlias.concat("."))) {
				qualifyReference = false;
				break;
			}
		}

		String reference = (qualifyReference && StringUtils.hasText(alias)) ? String.format("%s.%s", alias, property)
				: property;
		String wrapped = order.isIgnoreCase() ? String.format("lower(%s)", reference) : reference;

		return String.format("%s %s", wrapped, toJpaDirection(order));
	}

	static Set<String> getOuterJoinAliases(String query) {

		Set<String> result = new HashSet<>();
		Matcher matcher = JOIN_PATTERN.matcher(query);

		while (matcher.find()) {

			String alias = matcher.group(QUERY_JOIN_ALIAS_GROUP_INDEX);
			if (StringUtils.hasText(alias)) {
				result.add(alias);
			}
		}

		return result;
	}

	private static Set<String> getFieldAliases(String query) {
		Set<String> result = new HashSet<>();
		Matcher matcher = FIELD_ALIAS_PATTERN.matcher(query);

		while (matcher.find()) {
			String alias = matcher.group(2);

			if (StringUtils.hasText(alias)) {
				result.add(alias);
			}
		}
		return result;
	}

	public static String quoteFieldAliases(String query, Dialect dialect) {
		Matcher matcher = FIELD_ALIAS_PATTERN.matcher(query);

		while (matcher.find()) {

			String column = matcher.group(1);
			String alias = matcher.group(2);
			if (StringUtils.hasText(alias)) {

				query = query.replace(matcher.group(),
						String.format(" %s as %s ", column, dialect.quoteCertainly(alias)));

			}

		}

		return query;
	}

	static Set<String> getFunctionAliases(String query) {

		Set<String> result = new HashSet<>();
		Matcher matcher = FUNCTION_PATTERN.matcher(query);

		while (matcher.find()) {

			String alias = matcher.group(1);

			if (StringUtils.hasText(alias)) {
				result.add(alias);
			}
		}

		return result;
	}

	private static String toJpaDirection(Sort.Order order) {
		return order.getDirection().name().toLowerCase(Locale.US);
	}

	/**
	 * Resolves the alias for the entity to be retrieved from the given JPA query.
	 * @param query must not be {@literal null}.
	 * @return might return {@literal null}.
	 * @deprecated use {@link DeclaredQuery#getAlias()} instead.
	 */
	@Nullable
	@Deprecated
	public static String detectAlias(String query) {

		Matcher matcher = ALIAS_MATCH.matcher(query);

		return matcher.find() ? matcher.group(2) : null;
	}

	/**
	 * Creates a count projected query from the given original query.
	 * @param originalQuery must not be {@literal null} or empty.
	 * @return guaranteed to be not {@literal null}.
	 * @deprecated use {@link DeclaredQuery#deriveCountQuery(String, String)} instead.
	 */
	@Deprecated
	public static String createCountQueryFor(String originalQuery) {
		return createCountQueryFor(originalQuery, null);
	}

	/**
	 * Creates a count projected query from the given original query.
	 * @param originalQuery must not be {@literal null}.
	 * @param countProjection may be {@literal null}.
	 * @return a query String to be used a count query for pagination. Guaranteed to be
	 * not {@literal null}.
	 * @since 1.6
	 * @deprecated use {@link DeclaredQuery#deriveCountQuery(String, String)} instead.
	 */
	@Deprecated
	public static String createCountQueryFor(String originalQuery, @Nullable String countProjection) {

		Assert.hasText(originalQuery, "OriginalQuery must not be null or empty!");

		Matcher matcher = COUNT_MATCH.matcher(originalQuery);
		String countQuery;

		if (countProjection == null) {

			String variable = matcher.matches() ? matcher.group(VARIABLE_NAME_GROUP_INDEX) : null;
			boolean useVariable = StringUtils.hasText(variable) //
					&& !variable.startsWith(" new") //
					&& !variable.startsWith("count(") //
					&& !variable.contains(","); //

			String complexCountValue = (matcher.matches()
					&& StringUtils.hasText(matcher.group(COMPLEX_COUNT_FIRST_INDEX))) ? COMPLEX_COUNT_VALUE
							: COMPLEX_COUNT_LAST_VALUE;

			String replacement = useVariable ? SIMPLE_COUNT_VALUE : complexCountValue;
			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, replacement));
		}
		else {
			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, countProjection));
		}

		return countQuery.replaceFirst(ORDER_BY_PART, "");
	}

	/**
	 * Returns whether the given query contains named parameters.
	 * @param query can be {@literal null} or empty.
	 * @return whether the given query contains named parameters.
	 */
	@Deprecated
	static boolean hasNamedParameter(@Nullable String query) {
		return StringUtils.hasText(query) && NAMED_PARAMETER.matcher(query).find();
	}

	public static boolean hasConstructorExpression(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		return CONSTRUCTOR_EXPRESSION.matcher(query).find();
	}

	public static String getConstructorExpression(String query) {
		Matcher matcher = CONSTRUCTOR_EXPRESSION.matcher(query);
		if (matcher.find()) {
			String exp = matcher.group().trim();

			int idx = exp.toLowerCase().indexOf(" new ");

			return exp.substring(idx).trim();

		}
		return null;
	}

	public static String getProjection(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		Matcher matcher = PROJECTION_CLAUSE.matcher(query);
		String projection = matcher.find() ? matcher.group(1) : "";
		return projection.trim();
	}

	private static void checkSortExpression(Sort.Order order) {

		if (PUNCTATION_PATTERN.matcher(order.getProperty()).find()) {
			throw new InvalidDataAccessApiUsageException(String.format(UNSAFE_PROPERTY_REFERENCE, order));
		}
	}

}
