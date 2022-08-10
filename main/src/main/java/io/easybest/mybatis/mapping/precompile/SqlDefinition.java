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

import java.util.List;

import io.easybest.mybatis.repository.support.MybatisContext;
import lombok.Getter;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
public abstract class SqlDefinition extends AbstractSegment {

	protected String id;

	protected String databaseId;

	protected List<? extends Segment> derived;

	protected String parameterType = MybatisContext.class.getSimpleName();

}
