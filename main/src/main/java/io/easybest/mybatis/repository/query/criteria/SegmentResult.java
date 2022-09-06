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

package io.easybest.mybatis.repository.query.criteria;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import io.easybest.mybatis.mapping.precompile.Segment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * .
 *
 * @author Jarvis Song
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SegmentResult implements Serializable {

	private List<Segment> segments;

	private Set<String> connectors;

	public void add(Set<String> connectors) {

		if (null == this.connectors) {
			this.connectors = new LinkedHashSet<>();
		}

		this.connectors.addAll(connectors);
	}

	public void add(Segment segment) {
		if (null == this.segments) {
			this.segments = new LinkedList<>();
		}
		this.segments.add(segment);
	}

}
