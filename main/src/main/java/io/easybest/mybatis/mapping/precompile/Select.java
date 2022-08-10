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

package io.easybest.mybatis.mapping.precompile;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@SuperBuilder
@Getter
public class Select extends SqlDefinition {

	private String resultMap;

	private String resultType;

	@Override
	public String toString() {
		String sql = this.content();
		if (!StringUtils.hasText(sql)) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("<select id=\"").append(this.getId()).append("\"");
		if (StringUtils.hasText(this.resultMap)) {
			builder.append(" resultMap=\"").append(this.resultMap).append("\"");
		}
		if (StringUtils.hasText(this.resultType)) {
			builder.append(" resultType=\"").append(this.resultType).append("\"");
		}
		if (StringUtils.hasText(this.parameterType)) {
			builder.append(" parameterType=\"").append(this.parameterType).append("\"");
		}

		if (StringUtils.hasText(this.databaseId)) {
			builder.append(" databaseId=\"").append(this.databaseId).append("\"");
		}

		builder.append(">").append(sql).append("</select>");
		if (!CollectionUtils.isEmpty(this.derived)) {
			this.derived.forEach(builder::append);
		}
		return builder.toString();
	}

}
