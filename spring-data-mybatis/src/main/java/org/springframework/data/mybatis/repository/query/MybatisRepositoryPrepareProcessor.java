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
package org.springframework.data.mybatis.repository.query;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;

/**
 * {@link RepositoryProxyPostProcessor} that sets up interceptors to read metadata
 * information from the invoked method.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class MybatisRepositoryPrepareProcessor implements RepositoryProxyPostProcessor {

	private final MybatisMappingContext mappingContext;

	public MybatisRepositoryPrepareProcessor(MybatisMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	@Override
	public void postProcess(ProxyFactory factory, RepositoryInformation repositoryInformation) {
		new SimpleMybatisPrecompiler(this.mappingContext, repositoryInformation).precompile();
	}

}
