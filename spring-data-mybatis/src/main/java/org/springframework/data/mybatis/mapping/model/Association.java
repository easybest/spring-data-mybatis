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
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mybatis.annotation.Fetch;
import org.springframework.data.mybatis.annotation.FetchMode;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public abstract class Association implements Serializable {

	private static final long serialVersionUID = 2263465833694781795L;

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);
		annotations.add(ElementCollection.class);

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

	}
	protected final MybatisPersistentProperty property;

	protected final Domain self;

	protected final Domain target;

	public Association(MybatisPersistentProperty sourceProperty, Domain self, Domain target) {
		this.property = sourceProperty;
		this.self = self;
		this.target = target;
	}

	public boolean isJoin() {
		Fetch fetch = this.property.findAnnotation(Fetch.class);
		if (null == fetch || fetch.value() != FetchMode.JOIN) {
			return false;
		}
		return this.getFetchType() == FetchType.EAGER;
	}

	public FetchType getFetchType() {
		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {
			Annotation annotation = this.property.findAnnotation(annotationType);
			if (null == annotation) {
				continue;
			}
			FetchType fetchType = (FetchType) AnnotationUtils.getValue(annotation, "fetch");
			return fetchType;
		}
		return FetchType.LAZY;
	}

	public boolean isOneToOne() {
		return this.getClass() == OneToOneAssociation.class;
	}

	public boolean isManyToOne() {
		return this.getClass() == ManyToOneAssociation.class;
	}

	public boolean isToOne() {
		return this.isOneToOne() || this.isManyToOne();
	}

	public boolean isOneToMany() {
		return this.getClass() == OneToManyAssociation.class;
	}

	public boolean isManyToMany() {
		return this.getClass() == ManyToManyAssociation.class;
	}

	public boolean isEmbedding() {
		return this.getClass() == Embedding.class;
	}

	public MybatisPersistentProperty getProperty() {
		return this.property;
	}

	public Domain getSelf() {
		return this.self;
	}

	public Domain getTarget() {
		return this.target;
	}

}
