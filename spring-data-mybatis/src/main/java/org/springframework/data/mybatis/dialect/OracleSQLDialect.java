package org.springframework.data.mybatis.dialect;

import java.util.Locale;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.OracleIdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;

public class OracleSQLDialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow(selection);
			sql = sql.trim();
			String forUpdateClause = null;
			boolean isForUpdate = false;
			final int forUpdateIndex = sql.toLowerCase(Locale.ROOT)
					.lastIndexOf("for update");
			if (forUpdateIndex > -1) {
				// save 'for update ...' and then remove it
				forUpdateClause = sql.substring(forUpdateIndex);
				sql = sql.substring(0, forUpdateIndex - 1);
				isForUpdate = true;
			}

			final StringBuilder pagingSelect = new StringBuilder(sql.length() + 100);
			if (hasOffset) {
				pagingSelect
						.append("select * from ( select row_.*, rownum rownum_ from ( ");
			}
			else {
				pagingSelect.append("select * from ( ");
			}
			pagingSelect.append(sql);
			if (hasOffset) {
				pagingSelect.append(
						" ) row_ where rownum <![CDATA[<=]]> #{__offsetEnd}) where rownum_ <![CDATA[>]]> #{__offset}");
			}
			else {
				pagingSelect.append(" ) where rownum <![CDATA[<=]]> #{__offsetEnd}");
			}

			if (isForUpdate) {
				pagingSelect.append(" ");
				pagingSelect.append(forUpdateClause);
			}

			return pagingSelect.toString();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean bindLimitParametersInReverseOrder() {
			return true;
		}
	};

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new OracleIdentityColumnSupport();
	}

}
