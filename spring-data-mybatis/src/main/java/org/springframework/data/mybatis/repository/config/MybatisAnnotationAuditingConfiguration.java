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
package org.springframework.data.mybatis.repository.config;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.auditing.config.AnnotationAuditingConfiguration;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.1
 */
public class MybatisAnnotationAuditingConfiguration implements MybatisAuditingConfiguration {

	private static final String MISSING_ANNOTATION_ATTRIBUTES = "Couldn't find annotation attributes for %s in %s!";

	private final AnnotationAttributes attributes;

	/**
	 * Creates a new instance of {@link AnnotationAuditingConfiguration} for the given
	 * {@link AnnotationMetadata} and annotation type.
	 * @param metadata must not be {@literal null}.
	 * @param annotation must not be {@literal null}.
	 */
	public MybatisAnnotationAuditingConfiguration(AnnotationMetadata metadata, Class<? extends Annotation> annotation) {

		Assert.notNull(metadata, "AnnotationMetadata must not be null!");
		Assert.notNull(annotation, "Annotation must not be null!");

		Map<String, Object> attributesSource = metadata.getAnnotationAttributes(annotation.getName());

		if (attributesSource == null) {
			throw new IllegalArgumentException(String.format(MISSING_ANNOTATION_ATTRIBUTES, annotation, metadata));
		}

		this.attributes = new AnnotationAttributes(attributesSource);
	}

	@Override
	public String getAuditorAwareRef() {
		return this.attributes.getString("auditorAwareRef");
	}

	@Override
	public boolean isSetDates() {
		return this.attributes.getBoolean("setDates");
	}

	@Override
	public String getDateTimeProviderRef() {
		return this.attributes.getString("dateTimeProviderRef");
	}

	@Override
	public boolean isModifyOnCreate() {
		return this.attributes.getBoolean("modifyOnCreate");
	}

	@Override
	public String getSqlSessionTemplateRef() {
		return this.attributes.getString("sqlSessionTemplateRef");
	}

}
