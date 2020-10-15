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
package org.springframework.data.mybatis.repository.sample;

import java.util.HashMap;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.mybatis.repository.support.SqlSessionRepositorySupport;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class UserRepositoryImpl extends SqlSessionRepositorySupport implements UserRepositoryCustom {

	public UserRepositoryImpl(SqlSessionTemplate sqlSessionTemplate) {
		super(sqlSessionTemplate);
	}

	@Override
	protected String getNamespace() {
		return User.class.getName();
	}

	@Override
	public int insertUserRole(Long userId, Long roleId) {
		Map<String, Object> map = new HashMap<>();
		map.put("user_id", userId);
		map.put("role_id", roleId);
		return this.getSqlSession().insert("associative.user_role." + ResidentStatementName.INSERT, map);
	}

}
