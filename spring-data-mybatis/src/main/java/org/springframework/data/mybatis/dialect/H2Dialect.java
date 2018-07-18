package org.springframework.data.mybatis.dialect;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.impl.H2IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;
import org.springframework.util.ClassUtils;

/**
 * @author Jarvis Song
 */
@Slf4j
public class H2Dialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			if (null != selection && selection.getMaxRows() > 0) {
				return sql + " limit " + (LimitHelper.hasFirstRow(selection) ? (LimitHelper.getFirstRow(selection) + ",") : "")
						+ selection.getMaxRows();
			}
			// final boolean hasOffset = LimitHelper.hasFirstRow(selection);
			// return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
			return sql + " limit #{offset}, #{pageSize}";
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

	private final String querySequenceString;

	public H2Dialect() {
		super();
		String querySequenceString = "select sequence_name from information_schema.sequences";

		try {
			// HHH-2300
			final Class h2ConstantsClass = ClassUtils.forName("org.h2.engine.Constants", null);
			final int majorVersion = (Integer) h2ConstantsClass.getDeclaredField("VERSION_MAJOR").get(null);
			final int minorVersion = (Integer) h2ConstantsClass.getDeclaredField("VERSION_MINOR").get(null);
			final int buildId = (Integer) h2ConstantsClass.getDeclaredField("BUILD_ID").get(null);
			if (buildId < 32) {
				querySequenceString = "select name from information_schema.sequences";
			}
			if (!(majorVersion > 1 || minorVersion > 2 || buildId >= 139)) {
				log.warn(String.format(
						"The %s.%s.%s version of H2 implements temporary table creation such that it commits current transaction; multi-table, bulk hql/jpaql will not work properly",
						majorVersion, minorVersion, buildId));

			}
		} catch (Exception e) {
			// probably H2 not in the classpath, though in certain app server environments it might just mean we are
			// not using the correct classloader
			log.warn("Unable to determine H2 database version, certain features may not work");
		}

		this.querySequenceString = querySequenceString;
	}

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence if exists " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	@Override
	public String getQuerySequencesString() {
		return querySequenceString;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "call current_timestamp()";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new H2IdentityColumnSupport();
	}
}
