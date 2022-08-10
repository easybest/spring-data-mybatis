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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * .
 *
 * @author Jarvis Song
 */
class StringQuery implements DeclaredQuery {

	private final String query;

	private final List<ParameterBinding> bindings;

	private final @Nullable String alias;

	private final boolean hasConstructorExpression;

	private final boolean containsPageableInSpel;

	private final QueryEnhancer queryEnhancer;

	public StringQuery(String query) {

		Assert.hasText(query, "Query must not be null or empty!");

		this.bindings = new ArrayList<>();
		this.containsPageableInSpel = query.contains("#pageable");

		this.query = ParameterBindingParser.INSTANCE
				.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(query, this.bindings);

		this.queryEnhancer = QueryEnhancerFactory.forQuery(this);
		this.alias = this.queryEnhancer.detectAlias();
		this.hasConstructorExpression = this.queryEnhancer.hasConstructorExpression();
	}

	boolean hasParameterBindings() {
		return !this.bindings.isEmpty();
	}

	@Override
	public boolean hasNamedParameter() {
		return this.bindings.stream().anyMatch(b -> null != b.getName());
	}

	String getProjection() {
		return this.queryEnhancer.getProjection();
	}

	@Override
	public String getQueryString() {
		return this.query;
	}

	@Override
	public String getAlias() {
		return this.alias;
	}

	@Override
	public boolean hasConstructorExpression() {
		return this.hasConstructorExpression;
	}

	@Override
	public boolean isDefaultProjection() {

		return this.getProjection().equalsIgnoreCase(this.alias);
	}

	@Override
	public List<ParameterBinding> getParameterBindings() {
		return this.bindings;
	}

	@Override
	public boolean usesPaging() {
		return this.containsPageableInSpel;
	}

	@Override
	public DeclaredQuery deriveCountQuery(String countQuery, String countQueryProjection) {

		return DeclaredQuery
				.of(null != countQuery ? countQuery : this.queryEnhancer.createCountQueryFor(countQueryProjection));
	}

	@Override
	public String toString() {
		return "StringQuery{" + "query='" + this.query + '\'' + '}';
	}

	enum ParameterBindingParser {

		INSTANCE;

		private static final String EXPRESSION_PARAMETER_PREFIX = "__$synthetic$__";

		public static final String POSITIONAL_OR_INDEXED_PARAMETER = "\\?(\\d*+(?![#\\w]))";

		// .....................................................................^ not
		// followed by a hash or a letter.
		// .................................................................^ zero or more
		// digits.
		// .............................................................^ start with a
		// question mark.
		private static final Pattern PARAMETER_BINDING_BY_INDEX = Pattern.compile(POSITIONAL_OR_INDEXED_PARAMETER);

		private static final Pattern PARAMETER_BINDING_PATTERN;

		private static final Pattern JDBC_STYLE_PARAM = Pattern.compile(" \\?(?!\\d)"); // <space>?[no

		// digit]

		private static final Pattern NUMBERED_STYLE_PARAM = Pattern.compile(" \\?(?=\\d)"); // <space>?[digit]

		private static final Pattern NAMED_STYLE_PARAM = Pattern.compile(" :\\w+"); // <space>:[text]

		private static final String MESSAGE = "Already found parameter binding with same index / parameter name but differing binding type! "
				+ "Already have: %s, found %s! If you bind a parameter multiple times make sure they use the same binding.";

		private static final int INDEXED_PARAMETER_GROUP = 4;

		private static final int NAMED_PARAMETER_GROUP = 6;

		private static final int COMPARISION_TYPE_GROUP = 1;

		static {

			List<String> keywords = new ArrayList<>();

			for (ParameterBindingType type : ParameterBindingType.values()) {
				if (type.getKeyword() != null) {
					keywords.add(type.getKeyword());
				}
			}

			StringBuilder builder = new StringBuilder();
			builder.append("(");
			builder.append(StringUtils.collectionToDelimitedString(keywords, "|")); // keywords
			builder.append(")?");
			builder.append("(?: )?"); // some whitespace
			builder.append("\\(?"); // optional braces around parameters
			builder.append("(");
			builder.append("%?(" + POSITIONAL_OR_INDEXED_PARAMETER + ")%?"); // position
			// parameter
			// and
			// parameter
			// index
			builder.append("|"); // or

			// named parameter and the parameter name
			builder.append("%?(" + QueryUtils.COLON_NO_DOUBLE_COLON + QueryUtils.IDENTIFIER_GROUP + ")%?");

			builder.append(")");
			builder.append("\\)?"); // optional braces around parameters

			PARAMETER_BINDING_PATTERN = Pattern.compile(builder.toString(), CASE_INSENSITIVE);
		}

		/**
		 * Parses {@link ParameterBinding} instances from the given query and adds them to
		 * the registered bindings. Returns the cleaned up query.
		 */
		private String parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(String query,
				List<ParameterBinding> bindings) {

			int greatestParameterIndex = tryFindGreatestParameterIndexIn(query);
			boolean parametersShouldBeAccessedByIndex = greatestParameterIndex != -1;

			/*
			 * Prefer indexed access over named parameters if only SpEL Expression
			 * parameters are present.
			 */
			if (!parametersShouldBeAccessedByIndex && query.contains("?#{")) {
				parametersShouldBeAccessedByIndex = true;
				greatestParameterIndex = 0;
			}

			SpelQueryContext.SpelExtractor spelExtractor = createSpelExtractor(query, parametersShouldBeAccessedByIndex,
					greatestParameterIndex);

			String resultingQuery = spelExtractor.getQueryString();
			Matcher matcher = PARAMETER_BINDING_PATTERN.matcher(resultingQuery);

			int expressionParameterIndex = parametersShouldBeAccessedByIndex ? greatestParameterIndex : 0;

			boolean usesJpaStyleParameters = false;
			boolean usesJdbcStyleParameters = false;

			while (matcher.find()) {

				if (spelExtractor.isQuoted(matcher.start())) {
					continue;
				}

				String parameterIndexString = matcher.group(INDEXED_PARAMETER_GROUP);
				String parameterName = parameterIndexString != null ? null : matcher.group(NAMED_PARAMETER_GROUP);
				Integer parameterIndex = getParameterIndex(parameterIndexString);

				String typeSource = matcher.group(COMPARISION_TYPE_GROUP);
				Assert.isTrue(parameterIndexString != null || parameterName != null,
						() -> String.format("We need either a name or an index! Offending query string: %s", query));
				String expression = spelExtractor
						.getParameter(parameterName == null ? parameterIndexString : parameterName);
				String replacement = null;
				usesJdbcStyleParameters = JDBC_STYLE_PARAM.matcher(resultingQuery).find();
				usesJpaStyleParameters = NUMBERED_STYLE_PARAM.matcher(resultingQuery).find()
						|| NAMED_STYLE_PARAM.matcher(resultingQuery).find();

				expressionParameterIndex++;
				if ("".equals(parameterIndexString)) {
					parameterIndex = expressionParameterIndex;
				}

				if (usesJpaStyleParameters && usesJdbcStyleParameters) {
					throw new IllegalArgumentException(
							"Mixing of ? parameters and other forms like ?1 is not supported!");
				}

				switch (ParameterBindingType.of(typeSource)) {

				case LIKE:

					Part.Type likeType = LikeParameterBinding.getLikeTypeFrom(matcher.group(2));
					replacement = matcher.group(3);

					if (parameterIndex != null) {
						checkAndRegister(new LikeParameterBinding(parameterIndex, likeType, expression), bindings);
					}
					else {
						checkAndRegister(new LikeParameterBinding(parameterName, likeType, expression), bindings);

						replacement = ":" + parameterName;
					}

					break;

				case IN:

					if (parameterIndex != null) {
						checkAndRegister(new InParameterBinding(parameterIndex, expression), bindings);
					}
					else {
						checkAndRegister(new InParameterBinding(parameterName, expression), bindings);
					}

					break;

				case AS_IS: // fall-through we don't need a special parameter binding for
					// the given parameter.
				default:

					bindings.add(parameterIndex != null //
							? new ParameterBinding(null, parameterIndex, expression) //
							: new ParameterBinding(parameterName, null, expression));
				}

				if (replacement != null) {
					resultingQuery = replaceFirst(resultingQuery, matcher.group(2), replacement);
				}

			}

			return resultingQuery;
		}

		private static SpelQueryContext.SpelExtractor createSpelExtractor(String queryWithSpel,
				boolean parametersShouldBeAccessedByIndex, int greatestParameterIndex) {

			/*
			 * If parameters need to be bound by index, we bind the synthetic expression
			 * parameters starting from position of the greatest discovered index
			 * parameter in order to not mix-up with the actual parameter indices.
			 */
			int expressionParameterIndex = parametersShouldBeAccessedByIndex ? greatestParameterIndex : 0;

			BiFunction<Integer, String, String> indexToParameterName = parametersShouldBeAccessedByIndex
					? (index, expression) -> String.valueOf(index + expressionParameterIndex + 1)
					: (index, expression) -> EXPRESSION_PARAMETER_PREFIX + (index + 1);

			String fixedPrefix = parametersShouldBeAccessedByIndex ? "?" : ":";

			BiFunction<String, String, String> parameterNameToReplacement = (prefix, name) -> fixedPrefix + name;

			return SpelQueryContext.of(indexToParameterName, parameterNameToReplacement).parse(queryWithSpel);
		}

		private static String replaceFirst(String text, String substring, String replacement) {

			int index = text.indexOf(substring);
			if (index < 0) {
				return text;
			}

			return text.substring(0, index) + replacement + text.substring(index + substring.length());
		}

		@Nullable
		private static Integer getParameterIndex(@Nullable String parameterIndexString) {

			if (parameterIndexString == null || parameterIndexString.isEmpty()) {
				return null;
			}
			return Integer.valueOf(parameterIndexString);
		}

		private static int tryFindGreatestParameterIndexIn(String query) {

			Matcher parameterIndexMatcher = PARAMETER_BINDING_BY_INDEX.matcher(query);

			int greatestParameterIndex = -1;
			while (parameterIndexMatcher.find()) {

				String parameterIndexString = parameterIndexMatcher.group(1);
				Integer parameterIndex = getParameterIndex(parameterIndexString);
				if (parameterIndex != null) {
					greatestParameterIndex = Math.max(greatestParameterIndex, parameterIndex);
				}
			}

			return greatestParameterIndex;
		}

		private static void checkAndRegister(ParameterBinding binding, List<ParameterBinding> bindings) {

			bindings.stream() //
					.filter(it -> it.hasName(binding.getName()) || it.hasPosition(binding.getPosition())) //
					.forEach(it -> Assert.isTrue(it.equals(binding), String.format(MESSAGE, it, binding)));

			if (!bindings.contains(binding)) {
				bindings.add(binding);
			}
		}

		/**
		 * An enum for the different types of bindings.
		 *
		 * @author Thomas Darimont
		 * @author Oliver Gierke
		 */
		private enum ParameterBindingType {

			// Trailing whitespace is intentional to reflect that the keywords must be
			// used with at least one whitespace
			// character, while = does not.
			LIKE("like "), IN("in "), AS_IS(null);

			private final @Nullable String keyword;

			ParameterBindingType(@Nullable String keyword) {
				this.keyword = keyword;
			}

			/**
			 * Returns the keyword that will trigger the binding type or {@literal null}
			 * if the type is not triggered by a keyword.
			 * @return the keyword
			 */
			@Nullable
			public String getKeyword() {
				return this.keyword;
			}

			/**
			 * Return the appropriate {@link ParameterBindingType} for the given
			 * {@link String}. Returns {@literal #AS_IS} in case no other
			 * {@link ParameterBindingType} could be found.
			 */
			static ParameterBindingType of(String typeSource) {

				if (!StringUtils.hasText(typeSource)) {
					return AS_IS;
				}

				for (ParameterBindingType type : values()) {
					if (type.name().equalsIgnoreCase(typeSource.trim())) {
						return type;
					}
				}

				throw new IllegalArgumentException(String.format("Unsupported parameter binding type %s!", typeSource));
			}

		}

	}

	static class ParameterBinding {

		private final @Nullable String name;

		private final @Nullable String expression;

		private final @Nullable Integer position;

		ParameterBinding(Integer position) {
			this(null, position, null);
		}

		ParameterBinding(@Nullable String name, @Nullable Integer position, @Nullable String expression) {

			if (name == null) {
				Assert.notNull(position, "Position must not be null!");
			}

			if (position == null) {
				Assert.notNull(name, "Name must not be null!");
			}

			this.name = name;
			this.position = position;
			this.expression = expression;
		}

		boolean hasName(@Nullable String name) {
			return this.position == null && this.name != null && this.name.equals(name);
		}

		boolean hasPosition(@Nullable Integer position) {
			return position != null && this.name == null && position.equals(this.position);
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		String getRequiredName() throws IllegalStateException {

			String name = this.getName();

			if (name != null) {
				return name;
			}

			throw new IllegalStateException(String.format("Required name for %s not available!", this));
		}

		@Nullable
		Integer getPosition() {
			return this.position;
		}

		int getRequiredPosition() throws IllegalStateException {

			Integer position = this.getPosition();

			if (position != null) {
				return position;
			}

			throw new IllegalStateException(String.format("Required position for %s not available!", this));
		}

		public boolean isExpression() {
			return this.expression != null;
		}

		@Override
		public int hashCode() {

			int result = 17;

			result += nullSafeHashCode(this.name);
			result += nullSafeHashCode(this.position);
			result += nullSafeHashCode(this.expression);

			return result;
		}

		@Override
		public boolean equals(Object obj) {

			if (!(obj instanceof ParameterBinding)) {
				return false;
			}

			ParameterBinding that = (ParameterBinding) obj;

			return nullSafeEquals(this.name, that.name) && nullSafeEquals(this.position, that.position)
					&& nullSafeEquals(this.expression, that.expression);
		}

		@Override
		public String toString() {
			return String.format("ParameterBinding [name: %s, position: %d, expression: %s]", this.getName(),
					this.getPosition(), this.getExpression());
		}

		@Nullable
		public Object prepare(@Nullable Object valueToBind) {
			return valueToBind;
		}

		@Nullable
		public String getExpression() {
			return this.expression;
		}

	}

	static class InParameterBinding extends ParameterBinding {

		InParameterBinding(String name, @Nullable String expression) {
			super(name, null, expression);
		}

		InParameterBinding(int position, @Nullable String expression) {
			super(null, position, expression);
		}

		@Override
		public Object prepare(@Nullable Object value) {

			if (!ObjectUtils.isArray(value)) {
				return value;
			}

			int length = Array.getLength(value);
			Collection<Object> result = new ArrayList<>(length);

			for (int i = 0; i < length; i++) {
				result.add(Array.get(value, i));
			}

			return result;
		}

	}

	static class LikeParameterBinding extends ParameterBinding {

		private static final List<Part.Type> SUPPORTED_TYPES = Arrays.asList(Part.Type.CONTAINING,
				Part.Type.STARTING_WITH, Part.Type.ENDING_WITH, Part.Type.LIKE);

		private final Part.Type type;

		LikeParameterBinding(String name, Part.Type type) {
			this(name, type, null);
		}

		LikeParameterBinding(String name, Part.Type type, @Nullable String expression) {

			super(name, null, expression);

			Assert.hasText(name, "Name must not be null or empty!");
			Assert.notNull(type, "Type must not be null!");

			Assert.isTrue(SUPPORTED_TYPES.contains(type), String.format("Type must be one of %s!",
					StringUtils.collectionToCommaDelimitedString(SUPPORTED_TYPES)));

			this.type = type;
		}

		LikeParameterBinding(int position, Part.Type type) {
			this(position, type, null);
		}

		LikeParameterBinding(int position, Part.Type type, @Nullable String expression) {

			super(null, position, expression);

			Assert.isTrue(position > 0, "Position must be greater than zero!");
			Assert.notNull(type, "Type must not be null!");

			Assert.isTrue(SUPPORTED_TYPES.contains(type), String.format("Type must be one of %s!",
					StringUtils.collectionToCommaDelimitedString(SUPPORTED_TYPES)));

			this.type = type;
		}

		public Part.Type getType() {
			return this.type;
		}

		@Nullable
		@Override
		public Object prepare(@Nullable Object value) {

			if (value == null) {
				return null;
			}

			switch (this.type) {
			case STARTING_WITH:
				return String.format("%s%%", value);
			case ENDING_WITH:
				return String.format("%%%s", value);
			case CONTAINING:
				return String.format("%%%s%%", value);
			case LIKE:
			default:
				return value;
			}
		}

		@Override
		public boolean equals(Object obj) {

			if (!(obj instanceof LikeParameterBinding)) {
				return false;
			}

			LikeParameterBinding that = (LikeParameterBinding) obj;

			return super.equals(obj) && this.type.equals(that.type);
		}

		@Override
		public int hashCode() {

			int result = super.hashCode();

			result += nullSafeHashCode(this.type);

			return result;
		}

		@Override
		public String toString() {
			return String.format("LikeBinding [name: %s, position: %d, type: %s]", this.getName(), this.getPosition(),
					this.type);
		}

		private static Part.Type getLikeTypeFrom(String expression) {

			Assert.hasText(expression, "Expression must not be null or empty!");

			if (expression.matches("%.*%")) {
				return Part.Type.CONTAINING;
			}

			if (expression.startsWith("%")) {
				return Part.Type.ENDING_WITH;
			}

			if (expression.endsWith("%")) {
				return Part.Type.STARTING_WITH;
			}

			return Part.Type.LIKE;
		}

	}

}
