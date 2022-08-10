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

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@SuperBuilder
@Getter
public class Foreach extends AbstractSegment {

	private String collection;

	@Builder.Default
	private String item = "item";

	@Builder.Default
	private String index = "index";

	@Builder.Default
	private String open = "(";

	@Builder.Default
	private String close = ")";

	@Builder.Default
	private String separator = ",";

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<foreach");
		if (StringUtils.hasText(this.collection)) {
			builder.append(" collection=\"").append(this.collection).append("\"");
		}
		if (StringUtils.hasText(this.item)) {
			builder.append(" item=\"").append(this.item).append("\"");
		}
		if (StringUtils.hasText(this.index)) {
			builder.append(" index=\"").append(this.index).append("\"");
		}
		if (StringUtils.hasText(this.open)) {
			builder.append(" open=\"").append(this.open).append("\"");
		}
		if (StringUtils.hasText(this.close)) {
			builder.append(" close=\"").append(this.close).append("\"");
		}
		if (StringUtils.hasText(this.separator)) {
			builder.append(" separator=\"").append(this.separator).append("\"");
		}
		builder.append(">");
		builder.append(this.content());
		builder.append("</foreach>");
		return builder.toString();
	}

}
