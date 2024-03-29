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

package io.easybest.mybatis.repository.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_ONE;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_ONE;

/**
 * .
 *
 * @author Jarvis Song
 */
public abstract class QueryUtils {

	/**
	 * COUNT_QUERY_STRING.
	 */
	public static final String COUNT_QUERY_STRING = "select count(%s) from %s x";

	/**
	 * DELETE_ALL_QUERY_STRING.
	 */
	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";

	/**
	 * DELETE_ALL_QUERY_BY_ID_STRING.
	 */
	public static final String DELETE_ALL_QUERY_BY_ID_STRING = "delete from %s x where %s in :ids";

	private static final String IDENTIFIER = "[._$[\\P{Z}&&\\P{Cc}&&\\P{Cf}&&\\P{Punct}]]+";

	/**
	 * .
	 */
	public static final String COLON_NO_DOUBLE_COLON = "(?<![:\\\\]):";

	/**
	 * .
	 */
	public static final String IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER);

	private static final String COUNT_REPLACEMENT_TEMPLATE = "select count(%s) $5$6$7";

	private static final String SIMPLE_COUNT_VALUE = "$2";

	private static final String COMPLEX_COUNT_VALUE = "$3 $6";

	private static final String COMPLEX_COUNT_LAST_VALUE = "$6";

	private static final String ORDER_BY_PART = "(?iu)\\s+order\\s+by\\s+.*";

	private static final Pattern ALIAS_MATCH;

	private static final Pattern COUNT_MATCH;

	private static final Pattern STARTS_WITH_PAREN = Pattern.compile("^\\s*\\(");

	private static final Pattern PARENS_TO_REMOVE = Pattern.compile("(\\(.*\\bfrom\\b[^)]+\\))", CASE_INSENSITIVE);

	private static final Pattern PROJECTION_CLAUSE = Pattern.compile("select\\s+(?:distinct\\s+)?(.+)\\s+from",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PROJECTION_CLAUSE_TO_REPLACE = Pattern
			.compile("(select\\s+(?:distinct\\s+)?)(.+)(\\s+from)", Pattern.CASE_INSENSITIVE);

	private static final Pattern NO_DIGITS = Pattern.compile("\\D+");

	private static final String JOIN = "join\\s+(fetch\\s+)?" + IDENTIFIER + "\\s+(as\\s+)?" + IDENTIFIER_GROUP;

	private static final Pattern JOIN_PATTERN = Pattern.compile(JOIN, Pattern.CASE_INSENSITIVE);

	private static final String EQUALS_CONDITION_STRING = "%s.%s = :%s";

	private static final Pattern ORDER_BY = Pattern.compile("(order\\s+by\\s+)", CASE_INSENSITIVE);

	private static final Pattern ORDER_BY_IN_WINDOW_OR_SUBSELECT = Pattern
			.compile("\\([\\s\\S]*order\\s+by\\s[\\s\\S]*\\)", CASE_INSENSITIVE);

	private static final Pattern NAMED_PARAMETER = Pattern
			.compile(COLON_NO_DOUBLE_COLON + IDENTIFIER + "|#" + IDENTIFIER, CASE_INSENSITIVE);

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
		builder.append("(?<=\\bfrom)"); // from as starting delimiter
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append(IDENTIFIER_GROUP); // Entity name, can be qualified (any
		builder.append("(?:\\sas)*"); // exclude possible "as" keyword
		builder.append("(?:\\s)+"); // at least one space separating
		builder.append("(?!(?:where|group\\s*by|order\\s*by))(\\w+)"); // the actual alias

		ALIAS_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

		builder = new StringBuilder();
		builder.append("\\s*");
		builder.append("(select\\s+((distinct)?((?s).+?)?)\\s+)?(from\\s+");
		builder.append(IDENTIFIER);
		builder.append("(?:\\s+as)?\\s+)");
		builder.append(IDENTIFIER_GROUP);
		builder.append("(.*)");

		COUNT_MATCH = compile(builder.toString(), CASE_INSENSITIVE | DOTALL);

		Map<Attribute.PersistentAttributeType, Class<? extends Annotation>> persistentAttributeTypes = new HashMap<>();
		persistentAttributeTypes.put(ONE_TO_ONE, OneToOne.class);
		persistentAttributeTypes.put(ONE_TO_MANY, null);
		persistentAttributeTypes.put(MANY_TO_ONE, ManyToOne.class);
		persistentAttributeTypes.put(MANY_TO_MANY, null);
		persistentAttributeTypes.put(ELEMENT_COLLECTION, null);

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

		CONSTRUCTOR_EXPRESSION = compile(builder.toString(), CASE_INSENSITIVE + DOTALL);

		builder = new StringBuilder();
		// any function call including parameters within the brackets
		builder.append("\\w+\\s*\\([\\w\\.,\\s'=:\\\\?]+\\)");
		// the potential alias
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))");

		FUNCTION_PATTERN = compile(builder.toString());

		builder = new StringBuilder();
		builder.append("\\s+"); // at least one space
		builder.append("[^\\s\\(\\)]+"); // No white char no bracket
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))"); // the potential alias

		FIELD_ALIAS_PATTERN = compile(builder.toString());

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

		if (hasOrderByClause(query)) {
			builder.append(", ");
		}
		else {
			builder.append(" order by ");
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

	private static boolean hasOrderByClause(String query) {
		return countOccurences(ORDER_BY, query) > countOccurences(ORDER_BY_IN_WINDOW_OR_SUBSELECT, query);
	}

	private static int countOccurences(Pattern pattern, String string) {

		Matcher matcher = pattern.matcher(string);

		int occurences = 0;
		while (matcher.find()) {
			occurences++;
		}
		return occurences;
	}

	private static String getOrderClause(Set<String> joinAliases, Set<String> selectionAlias, @Nullable String alias,
			Sort.Order order) {

		String property = order.getProperty();

		checkSortExpression(order);

		if (selectionAlias.contains(property)) {

			return String.format("%s %s", //
					order.isIgnoreCase() ? String.format("lower(%s)", property) : property, //
					toJpaDirection(order));
		}

		boolean qualifyReference = !property.contains("("); // ( indicates a function

		for (String joinAlias : joinAliases) {

			if (property.startsWith(joinAlias.concat("."))) {

				qualifyReference = false;
				break;
			}
		}

		String reference = qualifyReference && StringUtils.hasText(alias) ? String.format("%s.%s", alias, property)
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
			String alias = matcher.group(1);

			if (StringUtils.hasText(alias)) {
				result.add(alias);
			}
		}
		return result;
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

	@Nullable
	// @Deprecated
	public static String detectAlias(String query) {

		String alias = null;
		Matcher matcher = ALIAS_MATCH.matcher(removeSubqueries(query));
		while (matcher.find()) {
			alias = matcher.group(2);
		}
		return alias;
	}

	static String removeSubqueries(String query) {

		if (!StringUtils.hasText(query)) {
			return query;
		}

		List<Integer> opens = new ArrayList<>();
		List<Integer> closes = new ArrayList<>();
		List<Boolean> closeMatches = new ArrayList<>();

		for (int i = 0; i < query.length(); i++) {

			char c = query.charAt(i);
			if (c == '(') {
				opens.add(i);
			}
			else if (c == ')') {
				closes.add(i);
				closeMatches.add(Boolean.FALSE);
			}
		}

		StringBuilder sb = new StringBuilder(query);
		boolean startsWithParen = STARTS_WITH_PAREN.matcher(query).find();
		for (int i = opens.size() - 1; i >= (startsWithParen ? 1 : 0); i--) {

			Integer open = opens.get(i);
			Integer close = findClose(open, closes, closeMatches) + 1;

			if (close > open) {

				String subquery = sb.substring(open, close);
				Matcher matcher = PARENS_TO_REMOVE.matcher(subquery);
				if (matcher.find()) {
					sb.replace(open, close, new String(new char[close - open]).replace('\0', ' '));
				}
			}
		}

		return sb.toString();
	}

	private static Integer findClose(final Integer open, final List<Integer> closes, final List<Boolean> closeMatches) {

		for (int i = 0; i < closes.size(); i++) {

			int close = closes.get(i);
			if (close > open && !closeMatches.get(i)) {
				closeMatches.set(i, Boolean.TRUE);
				return close;
			}
		}

		return -1;
	}

	public static <T> Query applyAndBind(String queryString, Iterable<T> entities, EntityManager entityManager) {

		Assert.notNull(queryString, "Querystring must not be null!");
		Assert.notNull(entities, "Iterable of entities must not be null!");
		Assert.notNull(entityManager, "EntityManager must not be null!");

		Iterator<T> iterator = entities.iterator();

		if (!iterator.hasNext()) {
			return entityManager.createQuery(queryString);
		}

		String alias = detectAlias(queryString);
		StringBuilder builder = new StringBuilder(queryString);
		builder.append(" where");

		int i = 0;

		while (iterator.hasNext()) {

			iterator.next();

			builder.append(String.format(" %s = ?%d", alias, ++i));

			if (iterator.hasNext()) {
				builder.append(" or");
			}
		}

		Query query = entityManager.createQuery(builder.toString());

		iterator = entities.iterator();
		i = 0;

		while (iterator.hasNext()) {
			query.setParameter(++i, iterator.next());
		}

		return query;
	}

	/**
	 * Creates a count projected query from the given original query.
	 * @param originalQuery must not be {@literal null} or empty.
	 * @return guaranteed to be not {@literal null}.
	 * @deprecated use {@link DeclaredQuery#deriveCountQuery(String, String)} instead.
	 */
	// @Deprecated
	public static String createCountQueryFor(String originalQuery) {
		return createCountQueryFor(originalQuery, null);
	}

	/**
	 * Creates a count projected query from the given original query.
	 * @param originalQuery must not be {@literal null}.
	 * @param countProjection may be {@literal null}.
	 * @return a query String to be used a count query for pagination. Guaranteed to be
	 * not {@literal null}.
	 */
	// @Deprecated
	public static String createCountQueryFor(String originalQuery, @Nullable String countProjection) {

		Assert.hasText(originalQuery, "OriginalQuery must not be null or empty!");

		Matcher matcher = COUNT_MATCH.matcher(originalQuery);
		String countQuery;

		if (countProjection == null) {

			String variable = matcher.matches() ? matcher.group(VARIABLE_NAME_GROUP_INDEX) : null;
			boolean useVariable = StringUtils.hasText(variable) //
					&& !variable.startsWith("new") // select [new com.example.User...
					&& !variable.startsWith(" new") // select distinct[ new
					// com.example.User...
					&& !variable.startsWith("count(") // select [count(...
					&& !variable.contains(",");

			String complexCountValue = matcher.matches()
					&& StringUtils.hasText(matcher.group(COMPLEX_COUNT_FIRST_INDEX)) ? COMPLEX_COUNT_VALUE
							: COMPLEX_COUNT_LAST_VALUE;

			String replacement = useVariable ? SIMPLE_COUNT_VALUE : complexCountValue;

			String alias = QueryUtils.detectAlias(originalQuery);
			if ("*".equals(variable) && alias != null) {
				replacement = alias;
			}

			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, replacement));
		}
		else {
			countQuery = matcher.replaceFirst(String.format(COUNT_REPLACEMENT_TEMPLATE, countProjection));
		}

		return countQuery.replaceFirst(ORDER_BY_PART, "");
	}

	/**
	 * Returns whether the given {@link Query} contains named parameters.
	 * @param query must not be {@literal null}.
	 * @return whether the given {@link Query} contains named parameters.
	 */
	public static boolean hasNamedParameter(Query query) {

		Assert.notNull(query, "Query must not be null!");

		for (Parameter<?> parameter : query.getParameters()) {

			String name = parameter.getName();

			// Hibernate 3 specific hack as it returns the index as String for the name.
			if (name != null && NO_DIGITS.matcher(name).find()) {
				return true;
			}
		}

		return false;
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

	/**
	 * Turns the given {@link Sort} into {@link javax.persistence.criteria.Order}s.
	 * @param sort the {@link Sort} instance to be transformed into JPA
	 * {@link javax.persistence.criteria.Order}s.
	 * @param from must not be {@literal null}.
	 * @param cb must not be {@literal null}.
	 * @return a {@link List} of {@link javax.persistence.criteria.Order}s.
	 */
	public static List<javax.persistence.criteria.Order> toOrders(Sort sort, From<?, ?> from, CriteriaBuilder cb) {

		if (sort.isUnsorted()) {
			return Collections.emptyList();
		}

		Assert.notNull(from, "From must not be null!");
		Assert.notNull(cb, "CriteriaBuilder must not be null!");

		List<javax.persistence.criteria.Order> orders = new ArrayList<>();

		for (org.springframework.data.domain.Sort.Order order : sort) {
			orders.add(toJpaOrder(order, from, cb));
		}

		return orders;
	}

	/**
	 * Returns whether the given JPQL query contains a constructor expression.
	 * @param query must not be {@literal null} or empty.
	 * @return whether the given JPQL query contains a constructor expression.
	 * @since 1.10
	 */
	public static boolean hasConstructorExpression(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		return CONSTRUCTOR_EXPRESSION.matcher(query).find();
	}

	/**
	 * Returns the projection part of the query, i.e. everything between {@code select}
	 * and {@code from}.
	 * @param query must not be {@literal null} or empty.
	 * @return the projection part of the query.
	 * @since 1.10.2
	 */
	public static String getProjection(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		Matcher matcher = PROJECTION_CLAUSE.matcher(query);
		String projection = matcher.find() ? matcher.group(1) : "";
		return projection.trim();
	}

	public static String replaceProjection(String query, String toReplace) {

		Assert.hasText(query, "Query must not be null or empty!");

		// "(select\\s+(?:distinct\\s+)?)(.+)(\\s+from)"
		Matcher matcher = PROJECTION_CLAUSE_TO_REPLACE.matcher(query);
		if (matcher.find()) {
			query = matcher.replaceFirst("$1" + toReplace + "$3");
		}
		return query;

	}

	/**
	 * Creates a criteria API {@link javax.persistence.criteria.Order} from the given
	 * {@link Sort.Order}.
	 * @param order the order to transform into a JPA
	 * {@link javax.persistence.criteria.Order}
	 * @param from the {@link From} the {@link Sort.Order} expression is based on
	 * @param cb the {@link CriteriaBuilder} to build the
	 * {@link javax.persistence.criteria.Order} with
	 * @return guaranteed to be not {@literal null}.
	 */
	@SuppressWarnings("unchecked")
	private static javax.persistence.criteria.Order toJpaOrder(Sort.Order order, From<?, ?> from, CriteriaBuilder cb) {

		PropertyPath property = PropertyPath.from(order.getProperty(), from.getJavaType());
		Expression<?> expression = toExpressionRecursively(from, property);

		if (order.isIgnoreCase() && String.class.equals(expression.getJavaType())) {
			Expression<String> upper = cb.lower((Expression<String>) expression);
			return order.isAscending() ? cb.asc(upper) : cb.desc(upper);
		}
		else {
			return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
		}
	}

	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property) {
		return toExpressionRecursively(from, property, false);
	}

	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property, boolean isForSelection) {
		return toExpressionRecursively(from, property, isForSelection, false);
	}

	/**
	 * Creates an expression with proper inner and left joins by recursively navigating
	 * the path.
	 * @param from the {@link From}
	 * @param property the property path
	 * @param isForSelection is the property navigated for the selection or ordering part
	 * of the query?
	 * @param hasRequiredOuterJoin has a parent already required an outer join?
	 * @param <T> the type of the expression
	 * @return the expression
	 */
	@SuppressWarnings("unchecked")
	static <T> Expression<T> toExpressionRecursively(From<?, ?> from, PropertyPath property, boolean isForSelection,
			boolean hasRequiredOuterJoin) {

		String segment = property.getSegment();

		boolean isLeafProperty = !property.hasNext();

		boolean requiresOuterJoin = requiresOuterJoin(from, property, isForSelection, hasRequiredOuterJoin);

		// if it does not require an outer join and is a leaf, simply get the segment
		if (!requiresOuterJoin && isLeafProperty) {
			return from.get(segment);
		}

		// get or create the join
		JoinType joinType = requiresOuterJoin ? JoinType.LEFT : JoinType.INNER;
		Join<?, ?> join = getOrCreateJoin(from, segment, joinType);

		// if it's a leaf, return the join
		if (isLeafProperty) {
			return (Expression<T>) join;
		}

		PropertyPath nextProperty = Objects.requireNonNull(property.next(), "An element of the property path is null!");

		// recurse with the next property
		return toExpressionRecursively(join, nextProperty, isForSelection, requiresOuterJoin);
	}

	/**
	 * Checks if this attribute requires an outer join. This is the case e.g. if it hadn't
	 * already been fetched with an inner join and if it's an optional association, and if
	 * previous paths has already required outer joins. It also ensures outer joins are
	 * used even when Hibernate defaults to inner joins (HHH-12712 and HHH-12999).
	 * @param from the {@link From} to check for fetches.
	 * @param property the property path
	 * @param isForSelection is the property navigated for the selection or ordering part
	 * of the query? if true, we need to generate an explicit outer join in order to
	 * prevent Hibernate to use an inner join instead. see
	 * https://hibernate.atlassian.net/browse/HHH-12999
	 * @param hasRequiredOuterJoin has a parent already required an outer join?
	 * @return whether an outer join is to be used for integrating this attribute in a
	 * query.
	 */
	private static boolean requiresOuterJoin(From<?, ?> from, PropertyPath property, boolean isForSelection,
			boolean hasRequiredOuterJoin) {

		String segment = property.getSegment();

		// already inner joined so outer join is useless
		if (isAlreadyInnerJoined(from, segment)) {
			return false;
		}

		Bindable<?> propertyPathModel;
		Bindable<?> model = from.getModel();

		// required for EclipseLink: we try to avoid using from.get as EclipseLink
		// produces an inner join
		// regardless of which join operation is specified next
		// see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=413892
		// still occurs as of 2.7
		ManagedType<?> managedType = null;
		if (model instanceof ManagedType) {
			managedType = (ManagedType<?>) model;
		}
		else if (model instanceof SingularAttribute
				&& ((SingularAttribute<?, ?>) model).getType() instanceof ManagedType) {
			managedType = (ManagedType<?>) ((SingularAttribute<?, ?>) model).getType();
		}
		if (managedType != null) {
			propertyPathModel = (Bindable<?>) managedType.getAttribute(segment);
		}
		else {
			propertyPathModel = from.get(segment).getModel();
		}

		// is the attribute of Collection type?
		boolean isPluralAttribute = model instanceof PluralAttribute;

		boolean isLeafProperty = !property.hasNext();

		if (propertyPathModel == null && isPluralAttribute) {
			return true;
		}

		if (!(propertyPathModel instanceof Attribute)) {
			return false;
		}

		Attribute<?, ?> attribute = (Attribute<?, ?>) propertyPathModel;

		// not a persistent attribute type association (@OneToOne, @ManyToOne)
		if (!ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
			return false;
		}

		boolean isCollection = attribute.isCollection();
		// if this path is an optional one to one attribute navigated from the not owning
		// side we also need an
		// explicit outer join to avoid https://hibernate.atlassian.net/browse/HHH-12712
		// and https://github.com/eclipse-ee4j/jpa-api/issues/170
		boolean isInverseOptionalOneToOne = Attribute.PersistentAttributeType.ONE_TO_ONE == attribute
				.getPersistentAttributeType() && StringUtils.hasText(getAnnotationProperty(attribute, "mappedBy", ""));

		if (isLeafProperty && !isForSelection && !isCollection && !isInverseOptionalOneToOne && !hasRequiredOuterJoin) {
			return false;
		}

		return hasRequiredOuterJoin || getAnnotationProperty(attribute, "optional", true);
	}

	@Nullable
	@SuppressWarnings({ "unchecked" })
	private static <T> T getAnnotationProperty(Attribute<?, ?> attribute, String propertyName, T defaultValue) {

		Class<? extends Annotation> associationAnnotation = ASSOCIATION_TYPES
				.get(attribute.getPersistentAttributeType());

		if (associationAnnotation == null) {
			return defaultValue;
		}

		Member member = attribute.getJavaMember();

		if (!(member instanceof AnnotatedElement)) {
			return defaultValue;
		}

		Annotation annotation = AnnotationUtils.getAnnotation((AnnotatedElement) member, associationAnnotation);

		return annotation == null ? defaultValue : (T) AnnotationUtils.getValue(annotation, propertyName);
	}

	/**
	 * Returns an existing join for the given attribute if one already exists or creates a
	 * new one if not.
	 * @param from the {@link From} to get the current joins from.
	 * @param attribute the {@link Attribute} to look for in the current joins.
	 * @param joinType the join type to create if none was found
	 * @return will never be {@literal null}.
	 */
	private static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute, JoinType joinType) {

		for (Join<?, ?> join : from.getJoins()) {

			if (join.getAttribute().getName().equals(attribute)) {
				return join;
			}
		}
		return from.join(attribute, joinType);
	}

	/**
	 * Return whether the given {@link From} contains an inner join for the attribute with
	 * the given name.
	 * @param from the {@link From} to check for joins.
	 * @param attribute the attribute name to check.
	 * @return true if the attribute has already been inner joined
	 */
	private static boolean isAlreadyInnerJoined(From<?, ?> from, String attribute) {

		for (Fetch<?, ?> fetch : from.getFetches()) {

			if (fetch.getAttribute().getName().equals(attribute) //
					&& fetch.getJoinType().equals(JoinType.INNER)) {
				return true;
			}
		}

		for (Join<?, ?> join : from.getJoins()) {

			if (join.getAttribute().getName().equals(attribute) //
					&& join.getJoinType().equals(JoinType.INNER)) {
				return true;
			}
		}

		return false;
	}

	static void checkSortExpression(Sort.Order order) {

		if (PUNCTATION_PATTERN.matcher(order.getProperty()).find()) {
			throw new InvalidDataAccessApiUsageException(String.format(UNSAFE_PROPERTY_REFERENCE, order));
		}
	}

}
