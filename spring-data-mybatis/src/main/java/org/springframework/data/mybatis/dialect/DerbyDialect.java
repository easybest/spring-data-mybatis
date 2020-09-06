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
package org.springframework.data.mybatis.dialect;

import java.lang.reflect.Method;
import java.util.Locale;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.util.ClassUtils;

/**
 * Derby dialect.
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class DerbyDialect extends DB2Dialect {

	private int driverVersionMajor;

	private int driverVersionMinor;

	private final LimitHandler limitHandler;

	public DerbyDialect() {
		super();

		this.determineDriverVersion();

		this.limitHandler = new DerbyLimitHandler();
	}

	private void determineDriverVersion() {
		try {
			final Class sysinfoClass = ClassUtils.forName("org.apache.derby.tools.sysinfo", this.getClass());
			final Method majorVersionGetter = sysinfoClass.getMethod("getMajorVersion",
					org.springframework.data.mybatis.util.ClassUtils.NO_PARAM_SIGNATURE);
			final Method minorVersionGetter = sysinfoClass.getMethod("getMinorVersion",
					org.springframework.data.mybatis.util.ClassUtils.NO_PARAM_SIGNATURE);
			this.driverVersionMajor = (Integer) majorVersionGetter.invoke(null,
					org.springframework.data.mybatis.util.ClassUtils.NO_PARAMS);
			this.driverVersionMinor = (Integer) minorVersionGetter.invoke(null,
					org.springframework.data.mybatis.util.ClassUtils.NO_PARAMS);
		}
		catch (Exception ex) {
			this.driverVersionMajor = -1;
			this.driverVersionMinor = -1;
		}
	}

	private boolean isTenPointFiveReleaseOrNewer() {
		return this.driverVersionMajor > 10 || (this.driverVersionMajor == 10 && this.driverVersionMinor >= 5);
	}

	@Override
	public LimitHandler getLimitHandler() {
		return this.limitHandler;
	}

	@Override
	public boolean supportsSequences() {
		return this.driverVersionMajor > 10 || (this.driverVersionMajor == 10 && this.driverVersionMinor >= 6);
	}

	@Override
	public String getSequenceNextValString(String sequenceName) throws MappingException {
		if (this.supportsSequences()) {
			return "values next value for " + sequenceName;
		}
		else {
			throw new MappingException("Derby does not support sequence prior to release 10.6.1.0");
		}
	}

	private final class DerbyLimitHandler extends AbstractLimitHandler {

		@Override
		public String processSql(String sql, RowSelection selection) {
			final StringBuilder sb = new StringBuilder(sql.length() + 50);
			final String normalizedSelect = sql.toLowerCase(Locale.ROOT).trim();
			final int forUpdateIndex = normalizedSelect.lastIndexOf("for update");

			if (this.hasForUpdateClause(forUpdateIndex)) {
				sb.append(sql, 0, forUpdateIndex - 1);
			}
			else if (this.hasWithClause(normalizedSelect)) {
				sb.append(sql, 0, this.getWithIndex(sql) - 1);
			}
			else {
				sb.append(sql);
			}

			if (LimitHelper.hasFirstRow(selection)) {
				sb.append(" offset ").append(LimitHelper.getFirstRow(selection)).append(" rows fetch next ");
			}
			else {
				sb.append(" fetch first ");
			}

			sb.append(selection.getMaxRows()).append(" rows only");

			if (this.hasForUpdateClause(forUpdateIndex)) {
				sb.append(' ');
				sb.append(sql.substring(forUpdateIndex));
			}
			else if (this.hasWithClause(normalizedSelect)) {
				sb.append(' ').append(sql.substring(this.getWithIndex(sql)));
			}
			return sb.toString();
		}

		@Override
		public boolean supportsLimit() {
			return DerbyDialect.this.isTenPointFiveReleaseOrNewer();
		}

		private boolean hasForUpdateClause(int forUpdateIndex) {
			return forUpdateIndex >= 0;
		}

		private boolean hasWithClause(String normalizedSelect) {
			return normalizedSelect.startsWith("with ", normalizedSelect.length() - 7);
		}

		private int getWithIndex(String querySelect) {
			int i = querySelect.lastIndexOf("with ");
			if (i < 0) {
				i = querySelect.lastIndexOf("WITH ");
			}
			return i;
		}

	}

}
