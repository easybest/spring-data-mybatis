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

import java.lang.annotation.Annotation;

import javax.persistence.OneToOne;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class OneToOneAssociation extends ManyToOneAssociation {

	private static final long serialVersionUID = 240806944716674713L;

	public OneToOneAssociation(MybatisMappingContext mappingContext, Model owner, MybatisPersistentProperty property,
			String alias) {
		super(mappingContext, owner, property, alias);
	}

	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return OneToOne.class;
	}

}
