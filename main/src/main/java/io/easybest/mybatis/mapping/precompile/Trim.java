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

import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
@SuperBuilder
@Getter
public class Trim extends AbstractSegment {

	private String prefix;

	private String prefixOverrides;

	private String suffix;

	private String suffixOverrides;

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("<trim");
		if (StringUtils.hasText(this.prefix)) {
			builder.append(" prefix=\"").append(this.prefix).append("\"");
		}
		if (StringUtils.hasText(this.prefixOverrides)) {
			builder.append(" prefixOverrides=\"").append(this.prefixOverrides).append("\"");
		}
		if (StringUtils.hasText(this.suffix)) {
			builder.append(" suffix=\"").append(this.suffix).append("\"");
		}
		if (StringUtils.hasText(this.suffixOverrides)) {
			builder.append(" suffixOverrides=\"").append(this.suffixOverrides).append("\"");
		}
		builder.append(">");
		builder.append(this.content());
		builder.append("</trim>");

		return builder.toString();
	}

}
