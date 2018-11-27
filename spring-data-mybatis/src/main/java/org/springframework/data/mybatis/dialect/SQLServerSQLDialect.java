package org.springframework.data.mybatis.dialect;

import java.util.Locale;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.SQLServerIdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;

public class SQLServerSQLDialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			if (LimitHelper.hasFirstRow(selection)) {
				throw new UnsupportedOperationException(
						"query result offset is not supported");
			}

			final int selectIndex = sql.toLowerCase(Locale.ROOT).indexOf("select");
			final int selectDistinctIndex = sql.toLowerCase(Locale.ROOT)
					.indexOf("select distinct");
			final int insertionPoint = selectIndex
					+ (selectDistinctIndex == selectIndex ? 15 : 6);

			StringBuilder sb = new StringBuilder(sql.length() + 8).append(sql);

			sb.insert(insertionPoint, " TOP " + getMaxOrLimit(selection) + " ");

			return sb.toString();
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
		return new SQLServerIdentityColumnSupport();
	}

}
