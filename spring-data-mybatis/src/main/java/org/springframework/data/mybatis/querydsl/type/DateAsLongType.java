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
package org.springframework.data.mybatis.querydsl.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import com.querydsl.sql.types.AbstractType;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class DateAsLongType extends AbstractType<Date> {

	public DateAsLongType() {
		super(Types.BIGINT);
	}

	public DateAsLongType(int type) {
		super(type);
	}

	@Override
	public Class<Date> getReturnedClass() {
		return Date.class;
	}

	@Override
	public Date getValue(ResultSet rs, int startIndex) throws SQLException {
		Object object = rs.getObject(startIndex);
		if (null == object) {
			return null;
		}

		Long time = Long.valueOf(String.valueOf(object));
		return new Date(time);
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, Date value) throws SQLException {
		st.setLong(startIndex, value.getTime());
	}

}
