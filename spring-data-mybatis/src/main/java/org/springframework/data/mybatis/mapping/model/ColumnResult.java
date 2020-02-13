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
package org.springframework.data.mybatis.mapping.model;

import java.io.Serializable;

import lombok.Data;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
@Data
public class ColumnResult implements Serializable {

	private boolean primaryKey;

	private String property;

	private String column;

	private String javaType;

	private String jdbcType;

	private String typeHandler;

}
