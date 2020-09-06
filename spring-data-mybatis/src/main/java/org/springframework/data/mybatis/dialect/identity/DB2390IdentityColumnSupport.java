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
package org.springframework.data.mybatis.dialect.identity;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class DB2390IdentityColumnSupport extends DB2IdentityColumnSupport {

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select identity_val_local() from sysibm.sysdummy1";
	}

}
