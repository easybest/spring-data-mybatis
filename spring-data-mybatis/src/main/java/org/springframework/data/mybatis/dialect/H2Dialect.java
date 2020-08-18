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

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.identity.H2IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.util.ClassUtils;

/**
 * H2 database dialect.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
@Slf4j
public class H2Dialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow(selection);
			if (hasOffset) {
				return sql + " LIMIT " + selection.getMaxRows() + " OFFSET " + LimitHelper.getFirstRow(selection);
			}
			return sql + " LIMIT " + selection.getMaxRows();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	private final String querySequenceString;

	public H2Dialect() {
		super();

		String querySequenceString = "select sequence_name from information_schema.sequences";
		try {
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
		}
		catch (Exception ex) {
			// probably H2 not in the classpath, though in certain app server environments
			// it might just mean we are
			// not using the correct classloader
			log.warn("Unable to determine H2 database version, certain features may not work");
		}

		this.querySequenceString = querySequenceString;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new H2IdentityColumnSupport();
	}

	@Override
	public String getSequenceNextValString(String sequenceName) throws MappingException {
		return "call next value for " + sequenceName;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getRegexLikeFunction(String column, String parameter) {
		return "REGEXP_LIKE(" + column + ", '" + parameter + "', 'i')";
	}

}
