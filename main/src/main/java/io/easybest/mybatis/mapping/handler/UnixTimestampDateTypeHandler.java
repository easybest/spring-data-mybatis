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

package io.easybest.mybatis.mapping.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * Date to unix time stamp type handler.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class UnixTimestampDateTypeHandler extends BaseTypeHandler<Long> {

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setTimestamp(i, new Timestamp(parameter));
	}

	@Override
	public Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(columnName);
		if (null == timestamp) {
			return null;
		}
		return timestamp.getTime();
	}

	@Override
	public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(columnIndex);
		if (null == timestamp) {
			return null;
		}
		return timestamp.getTime();
	}

	@Override
	public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		Timestamp timestamp = cs.getTimestamp(columnIndex);
		if (null == timestamp) {
			return null;
		}
		return timestamp.getTime();
	}

}
