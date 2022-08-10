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

package io.easybest.mybatis.dialect;

import java.util.Locale;

import io.easybest.mybatis.mapping.precompile.Segment;

import org.springframework.util.ClassUtils;

import static javax.persistence.GenerationType.SEQUENCE;

/**
 * A {@link Dialect} for HsqlDb.
 *
 * @author Jarvis Song
 */
public class HsqlDbDialect extends AbstractDialect {

	/**
	 * Singleton instance.
	 */
	public static final HsqlDbDialect INSTANCE = new HsqlDbDialect();

	private final LimitHandler limitHandler;

	private int hsqldbVersion = 180;

	public HsqlDbDialect() {

		super();

		try {
			final Class<?> props = ClassUtils.forName("org.hsqldb.persist.HsqlDatabaseProperties",
					HsqlDbDialect.class.getClassLoader());
			final String versionString = (String) props.getDeclaredField("THIS_VERSION").get(null);

			this.hsqldbVersion = Integer.parseInt(versionString.substring(0, 1)) * 100;
			this.hsqldbVersion += Integer.parseInt(versionString.substring(2, 3)) * 10;
			this.hsqldbVersion += Integer.parseInt(versionString.substring(4, 5));
		}
		catch (Throwable ex) {
			// must be a very old version
		}

		this.limitHandler = new HsqlLimitHandler();
	}

	@Override
	public String getIdentitySelectString() {
		return "CALL IDENTITY()";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "CALL NEXT VALUE FOR " + sequenceName;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return this.limitHandler;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return SEQUENCE.name().toLowerCase();
	}

	@Override
	public String regexpLike(String column, String pattern) {
		return "REGEXP_MATCHES(" + column + "," + pattern + ")";
	}

	private final class HsqlLimitHandler extends AbstractLimitHandler {

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			if (HsqlDbDialect.this.hsqldbVersion < 200) {

				return new StringBuilder(sql.length() + 10).append(sql)
						.insert(sql.toLowerCase(Locale.ROOT).indexOf("select") + 6,
								hasOffset ? (" LIMIT " + offset + " " + fetchSize) : (" TOP " + fetchSize))
						.toString();

			}

			return sql + (hasOffset ? (" OFFSET " + offset + " LIMIT " + fetchSize) : (" LIMIT " + fetchSize));
		}

	}

}
