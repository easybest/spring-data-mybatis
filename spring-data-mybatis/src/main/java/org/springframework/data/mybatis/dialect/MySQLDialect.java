package org.springframework.data.mybatis.dialect;

import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.MySQLIdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;

/**
 * @author Jarvis Song
 */
public class MySQLDialect extends Dialect {

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			if (LimitHelper.hasMaxRows(selection)) {
				return sql + " limit " + (LimitHelper.hasFirstRow(selection) ? (LimitHelper.getFirstRow(selection) + ",") : "")
						+ selection.getMaxRows();
			}

			// return sql + (hasOffset ? " limit #{pageSize}, #{offset}" : " limit #{pageSize}");
			return sql + " limit #{__offset}, #{__pageSize}";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	public MySQLDialect() {
		super();

	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MySQLIdentityColumnSupport();
	}

}
