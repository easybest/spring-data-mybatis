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

import org.springframework.data.mybatis.dialect.identity.DB2390IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;

/**
 * DB2 dialect.
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class DB2Dialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			if (LimitHelper.hasFirstRow(selection)) {
				return "select * from ( select inner2_.*, rownumber() over(order by order of inner2_) as rownumber_ from ( "
						+ sql + " fetch first " + selection.getMaxRows()
						+ " rows only ) as inner2_ ) as inner1_ where rownumber_ > "
						+ LimitHelper.getFirstRow(selection) + " order by rownumber_";
			}
			return sql + " fetch first " + selection.getMaxRows() + " rows only";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

	};

	public DB2Dialect() {
		super();
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new DB2390IdentityColumnSupport();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

}
