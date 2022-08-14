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

import javax.persistence.GenerationType;

import io.easybest.mybatis.mapping.precompile.Segment;

/**
 * .
 *
 * @author Jarvis Song
 */
public class DB2Dialect extends AbstractDialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			if (null != offset) {
				return "select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( "
						+ sql + " fetch first " + fetchSize + " rows only ) as inner2_ ) as inner1_ where rownumber_ > "
						+ offset + " order by rownumber_";
			}
			return sql + " fetch first " + fetchSize + " rows only";
		}
	};

	public DB2Dialect() {

		super();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {

		return GenerationType.SEQUENCE.name().toLowerCase();
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {

		return "next value for " + sequenceName;
	}

	@Override
	public String getIdentitySelectString() {
		return "values identity_val_local()";
	}

	@Override
	public String getIdentityInsertString() {
		return "default";
	}

}
