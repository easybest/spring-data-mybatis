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

package io.easybest.mybatis.dialect.pagination;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.easybest.mybatis.dialect.AbstractPaginationHandler;
import io.easybest.mybatis.dialect.internal.StringHelper;
import io.easybest.mybatis.mapping.precompile.Segment;

/**
 * .
 *
 * @author Jarvis Song
 */
public class SQLServer2005PaginationHandler extends AbstractPaginationHandler {

	private static final String SELECT = "select";

	private static final String FROM = "from";

	private static final String DISTINCT = "distinct";

	private static final String ORDER_BY = "order by";

	private static final String SELECT_DISTINCT = SELECT + " " + DISTINCT;

	private static final String SELECT_DISTINCT_SPACE = SELECT_DISTINCT + " ";

	private static final String SELECT_SPACE = "select ";

	private static final Pattern SELECT_DISTINCT_PATTERN = buildShallowIndexPattern(SELECT_DISTINCT_SPACE, true);

	private static final Pattern SELECT_PATTERN = buildShallowIndexPattern(SELECT + "(.*)", true);

	private static final Pattern FROM_PATTERN = buildShallowIndexPattern(FROM, true);

	private static final Pattern DISTINCT_PATTERN = buildShallowIndexPattern(DISTINCT, true);

	private static final Pattern ORDER_BY_PATTERN = buildShallowIndexPattern(ORDER_BY, true);

	private static final Pattern COMMA_PATTERN = buildShallowIndexPattern(",", false);

	private static final Pattern ALIAS_PATTERN = Pattern
			.compile("(?![^\\[]*(\\]))\\S+\\s*(\\s(?i)as\\s)\\s*(\\S+)*\\s*$|(?![^\\[]*(\\]))\\s+(\\S+)$");

	private static final String SPACE_NEWLINE_LINEFEED = "[\\s\\t\\n\\r]*";

	private static final Pattern WITH_CTE = Pattern
			.compile("(^" + SPACE_NEWLINE_LINEFEED + "WITH" + SPACE_NEWLINE_LINEFEED + ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern WITH_EXPRESSION_NAME = Pattern.compile(
			"(^" + SPACE_NEWLINE_LINEFEED + "[a-zA-Z0-9]*" + SPACE_NEWLINE_LINEFEED + ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern WITH_COLUMN_NAMES_START = Pattern.compile("(^" + SPACE_NEWLINE_LINEFEED + "\\()",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern WITH_COLUMN_NAMES_END = Pattern.compile("(\\))", Pattern.CASE_INSENSITIVE);

	private static final Pattern WITH_AS = Pattern
			.compile("(^" + SPACE_NEWLINE_LINEFEED + "AS" + SPACE_NEWLINE_LINEFEED + ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern WITH_COMMA = Pattern.compile("(^" + SPACE_NEWLINE_LINEFEED + ",)",
			Pattern.CASE_INSENSITIVE);

	private boolean topAdded;

	private boolean isCTE;

	@Override
	public String processSql(String sql, Segment offsetSegment, Segment fetchSize, Segment offsetEnd) {

		final StringBuilder sb = new StringBuilder(sql);
		if (sb.charAt(sb.length() - 1) == ';') {
			sb.setLength(sb.length() - 1);
		}

		final int offset = this.getStatementIndex(sb);

		if (null == offsetSegment) {
			this.addTopExpression(sb, offset, fetchSize);
		}
		else {
			final String selectClause = this.fillAliasInSelectClause(sb, offset);

			if (shallowIndexOfPattern(sb, ORDER_BY_PATTERN, offset) > 0) {
				this.addTopExpression(sb, offset, fetchSize);
			}

			this.encloseWithOuterQuery(sb, offset);

			sb.insert(offset, !this.isCTE ? "with query as (" : ", query as (");
			sb.append(") select ").append(selectClause).append(" from query ");
			sb.append("where __row__ &gt;= ").append(offsetSegment).append(" and __row__ &lt; ").append(offsetEnd);
		}

		return sb.toString();

	}

	protected void encloseWithOuterQuery(StringBuilder sql, int offset) {
		sql.insert(offset, "select inner_query.*, row_number() over (order by current_timestamp) as __row__ from ( ");
		sql.append(" ) inner_query ");
	}

	protected String fillAliasInSelectClause(StringBuilder sb, int offset) {
		final String separator = System.lineSeparator();
		final List<String> aliases = new LinkedList<>();
		final int startPos = this.getSelectColumnsStartPosition(sb, offset);
		int endPos = shallowIndexOfPattern(sb, FROM_PATTERN, startPos);

		int nextComa = startPos;
		int prevComa = startPos;
		int unique = 0;
		boolean selectsMultipleColumns = false;

		while (nextComa != -1) {
			prevComa = nextComa;
			nextComa = shallowIndexOfPattern(sb, COMMA_PATTERN, nextComa);
			if (nextComa > endPos) {
				break;
			}
			if (nextComa != -1) {
				final String expression = sb.substring(prevComa, nextComa);
				if (this.selectsMultipleColumns(expression)) {
					selectsMultipleColumns = true;
				}
				else {
					String alias = this.getAlias(expression);
					if (alias == null) {
						// Inserting alias. It is unlikely that we would have to add
						// alias, but just in case.
						alias = StringHelper.generateAlias("page", unique);
						sb.insert(nextComa, " as " + alias);
						final int aliasExprLength = (" as " + alias).length();
						++unique;
						nextComa += aliasExprLength;
						endPos += aliasExprLength;
					}
					aliases.add(alias);
				}
				++nextComa;
			}
		}

		endPos = shallowIndexOfPattern(sb, FROM_PATTERN, startPos);
		final String expression = sb.substring(prevComa, endPos);
		if (this.selectsMultipleColumns(expression)) {
			selectsMultipleColumns = true;
		}
		else {
			String alias = this.getAlias(expression);
			if (alias == null) {
				alias = StringHelper.generateAlias("page", unique);
				final boolean endWithSeparator = sb.substring(endPos - separator.length()).startsWith(separator);
				sb.insert(endPos - (endWithSeparator ? 2 : 1), " as " + alias);
			}
			aliases.add(alias);
		}

		return selectsMultipleColumns ? "*" : String.join(", ", aliases);
	}

	private String getAlias(String expression) {

		expression = expression.replaceFirst("(\\((.)*\\))", "").trim();

		final Matcher matcher = ALIAS_PATTERN.matcher(expression);

		String alias = null;
		if (matcher.find() && matcher.groupCount() > 1) {
			alias = matcher.group(3);
			if (alias == null) {
				alias = matcher.group(0);
			}
		}

		return (alias != null ? alias.trim() : null);
	}

	private int getSelectColumnsStartPosition(StringBuilder sb, int offset) {
		final int startPos = this.getSelectStartPosition(sb, offset);

		final String sql = sb.substring(startPos).toLowerCase();
		if (sql.startsWith(SELECT_DISTINCT_SPACE)) {
			return (startPos + SELECT_DISTINCT_SPACE.length());
		}
		else if (sql.startsWith(SELECT_SPACE)) {
			return (startPos + SELECT_SPACE.length());
		}
		return startPos;
	}

	private int getSelectStartPosition(StringBuilder sb, int offset) {
		return shallowIndexOfPattern(sb, SELECT_PATTERN, offset);
	}

	private boolean selectsMultipleColumns(String expression) {
		final String lastExpr = expression.trim().replaceFirst("(?i)(.)*\\s", "").trim();
		return "*".equals(lastExpr) || lastExpr.endsWith(".*");
	}

	protected void addTopExpression(StringBuilder sql, int offset, Segment fetchSize) {

		final int selectPos = shallowIndexOfPattern(sql, SELECT_PATTERN, offset);
		final int selectDistinctPos = shallowIndexOfPattern(sql, SELECT_DISTINCT_PATTERN, offset);
		if (selectPos == selectDistinctPos) {
			sql.insert(selectDistinctPos + SELECT_DISTINCT.length(), " top(" + fetchSize + ")");
		}
		else {
			sql.insert(selectPos + SELECT.length(), " top(" + fetchSize + ")");
		}
		this.topAdded = true;
	}

	private static int shallowIndexOfPattern(final StringBuilder sb, final Pattern pattern, int fromIndex) {
		int index = -1;
		final String matchString = sb.toString();

		if (matchString.length() < fromIndex || fromIndex < 0) {
			return -1;
		}

		List<IgnoreRange> ignoreRangeList = generateIgnoreRanges(matchString);

		Matcher matcher = pattern.matcher(matchString);
		matcher.region(fromIndex, matchString.length());

		if (ignoreRangeList.isEmpty()) {
			if (matcher.find() && matcher.groupCount() > 0) {
				index = matcher.start();
			}
		}
		else {
			while (matcher.find() && matcher.groupCount() > 0) {
				final int position = matcher.start();
				if (!isPositionIgnorable(ignoreRangeList, position)) {
					index = position;
					break;
				}
			}
		}
		return index;
	}

	private static List<IgnoreRange> generateIgnoreRanges(String sql) {
		List<IgnoreRange> ignoreRangeList = new ArrayList<>();

		int depth = 0;
		int start = -1;
		boolean insideAStringValue = false;
		for (int i = 0; i < sql.length(); ++i) {
			final char ch = sql.charAt(i);
			if (ch == '\'') {
				insideAStringValue = !insideAStringValue;
			}
			else if (ch == '(' && !insideAStringValue) {
				depth++;
				if (depth == 1) {
					start = i;
				}
			}
			else if (ch == ')' && !insideAStringValue) {
				if (depth > 0) {
					if (depth == 1) {
						ignoreRangeList.add(new IgnoreRange(start, i));
						start = -1;
					}
					depth--;
				}
				else {
					throw new IllegalStateException("Found an unmatched ')' at position " + i + ": " + sql);
				}
			}
		}

		if (depth != 0) {
			throw new IllegalStateException("Unmatched parenthesis in rendered SQL (" + depth + " depth): " + sql);
		}

		return ignoreRangeList;
	}

	private static boolean isPositionIgnorable(List<IgnoreRange> ignoreRangeList, int position) {
		for (IgnoreRange ignoreRange : ignoreRangeList) {
			if (ignoreRange.isWithinRange(position)) {
				return true;
			}
		}
		return false;
	}

	private int getStatementIndex(StringBuilder sql) {
		final Matcher matcher = WITH_CTE.matcher(sql.toString());
		if (matcher.find() && matcher.groupCount() > 0) {
			this.isCTE = true;
			return this.locateQueryInCTEStatement(sql, matcher.end());
		}
		return 0;
	}

	private int locateQueryInCTEStatement(StringBuilder sql, int offset) {
		while (true) {
			Matcher matcher = WITH_EXPRESSION_NAME.matcher(sql.substring(offset));
			if (matcher.find() && matcher.groupCount() > 0) {
				offset += matcher.end();
				matcher = WITH_COLUMN_NAMES_START.matcher(sql.substring(offset));
				if (matcher.find() && matcher.groupCount() > 0) {
					offset += matcher.end();
					matcher = WITH_COLUMN_NAMES_END.matcher(sql.substring(offset));
					if (matcher.find() && matcher.groupCount() > 0) {
						offset += matcher.end();
						offset += this.advanceOverCTEInnerQuery(sql, offset);
						matcher = WITH_COMMA.matcher(sql.substring(offset));
						if (matcher.find() && matcher.groupCount() > 0) {
							// another CTE fragment exists, re-start parse of CTE
							offset += matcher.end();
						}
						else {
							// last CTE fragment, we're at the start of the SQL.
							return offset;
						}
					}
					else {
						throw new IllegalArgumentException(String.format(Locale.ROOT,
								"Failed to parse CTE expression columns at offset %d, SQL [%s]", offset, sql));
					}
				}
				else {
					matcher = WITH_AS.matcher(sql.substring(offset));
					if (matcher.find() && matcher.groupCount() > 0) {
						offset += matcher.end();
						offset += this.advanceOverCTEInnerQuery(sql, offset);
						matcher = WITH_COMMA.matcher(sql.substring(offset));
						if (matcher.find() && matcher.groupCount() > 0) {
							offset += matcher.end();
						}
						else {
							return offset;
						}
					}
					else {
						throw new IllegalArgumentException(String.format(Locale.ROOT,
								"Failed to locate AS keyword in CTE query at offset %d, SQL [%s]", offset, sql));
					}
				}
			}
			else {
				throw new IllegalArgumentException(String.format(Locale.ROOT,
						"Failed to locate CTE expression name at offset %d, SQL [%s]", offset, sql));
			}
		}
	}

	private int advanceOverCTEInnerQuery(StringBuilder sql, int offset) {
		int brackets = 0;
		int index = offset;
		boolean inString = false;
		for (; index < sql.length(); ++index) {
			if (sql.charAt(index) == '\'' && !inString) {
				inString = true;
			}
			else if (sql.charAt(index) == '\'' && inString) {
				inString = false;
			}
			else if (sql.charAt(index) == '(' && !inString) {
				brackets++;
			}
			else if (sql.charAt(index) == ')' && !inString) {
				brackets--;
				if (brackets == 0) {
					break;
				}
			}
		}

		if (brackets > 0) {
			throw new IllegalArgumentException(
					"Failed to parse the CTE query inner query because closing ')' was not found.");
		}

		return index - offset + 1;
	}

	private static Pattern buildShallowIndexPattern(String pattern, boolean wordBoundary) {
		return Pattern.compile(
				"(" + (wordBoundary ? "\\b" : "") + pattern + (wordBoundary ? "\\b" : "") + ")(?![^\\(|\\[]*(\\)|\\]))",
				Pattern.CASE_INSENSITIVE);
	}

	static class IgnoreRange {

		private final int start;

		private final int end;

		IgnoreRange(int start, int end) {
			this.start = start;
			this.end = end;
		}

		boolean isWithinRange(int position) {
			return position >= this.start && position <= this.end;
		}

	}

}
