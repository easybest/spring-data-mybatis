package org.springframework.data.mybatis.repository.query;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ELEMENT_COLLECTION;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.MANY_TO_ONE;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_MANY;
import static javax.persistence.metamodel.Attribute.PersistentAttributeType.ONE_TO_ONE;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryUtils {

	public static final String COUNT_QUERY_STRING = "select count(%s) from %s x";

	public static final String DELETE_ALL_QUERY_STRING = "delete from %s x";

	// Used Regex/Unicode categories (see
	// http://www.unicode.org/reports/tr18/#General_Category_Property):
	// Z Separator
	// Cc Control
	// Cf Format
	// P Punctuation
	private static final String IDENTIFIER = "[._[\\P{Z}&&\\P{Cc}&&\\P{Cf}&&\\P{P}]]+";
	static final String COLON_NO_DOUBLE_COLON = "(?<![:\\\\]):";
	static final String IDENTIFIER_GROUP = String.format("(%s)", IDENTIFIER);

	private static final String COUNT_REPLACEMENT_TEMPLATE = "select count(%s) $5$6$7";

	private static final String SIMPLE_COUNT_VALUE = "$2";

	private static final String COMPLEX_COUNT_VALUE = "$3$6";

	private static final String ORDER_BY_PART = "(?iu)\\s+order\\s+by\\s+.*$";

	private static final Pattern ALIAS_MATCH;

	private static final Pattern COUNT_MATCH;

	private static final Pattern PROJECTION_CLAUSE = Pattern
			.compile("select\\s+(.+)\\s+from", Pattern.CASE_INSENSITIVE);

	private static final Pattern NO_DIGITS = Pattern.compile("\\D+");

	private static final String JOIN = "join\\s+(fetch\\s+)?" + IDENTIFIER
			+ "\\s+(as\\s+)?" + IDENTIFIER_GROUP;

	private static final Pattern JOIN_PATTERN = Pattern.compile(JOIN,
			Pattern.CASE_INSENSITIVE);

	private static final String EQUALS_CONDITION_STRING = "%s.%s = :%s";

	private static final Pattern ORDER_BY = Pattern.compile(".*order\\s+by\\s+.*",
			CASE_INSENSITIVE);

	private static final Pattern NAMED_PARAMETER = Pattern.compile(
			COLON_NO_DOUBLE_COLON + IDENTIFIER + "|\\#" + IDENTIFIER, CASE_INSENSITIVE);

	private static final Pattern CONSTRUCTOR_EXPRESSION;

	private static final Map<PersistentAttributeType, Class<? extends Annotation>> ASSOCIATION_TYPES;

	private static final int QUERY_JOIN_ALIAS_GROUP_INDEX = 3;

	private static final int VARIABLE_NAME_GROUP_INDEX = 4;

	private static final Pattern PUNCTATION_PATTERN = Pattern
			.compile(".*((?![\\._])[\\p{Punct}|\\s])");

	private static final Pattern FUNCTION_PATTERN;

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
		builder.append("(?!(?:where))(\\w+)"); // the actual alias

		ALIAS_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

		builder = new StringBuilder();
		builder.append("(select\\s+((distinct )?(.+?)?)\\s+)?(from\\s+");
		builder.append(IDENTIFIER);
		builder.append("(?:\\s+as)?\\s+)");
		builder.append(IDENTIFIER_GROUP);
		builder.append("(.*)");

		COUNT_MATCH = compile(builder.toString(), CASE_INSENSITIVE);

		Map<PersistentAttributeType, Class<? extends Annotation>> persistentAttributeTypes = new HashMap<PersistentAttributeType, Class<? extends Annotation>>();
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
		builder.append("\\w+\\s*\\([\\w\\.,\\s'=]+\\)");
		// the potential alias
		builder.append("\\s+[as|AS]+\\s+(([\\w\\.]+))");

		FUNCTION_PATTERN = compile(builder.toString());
	}

}
