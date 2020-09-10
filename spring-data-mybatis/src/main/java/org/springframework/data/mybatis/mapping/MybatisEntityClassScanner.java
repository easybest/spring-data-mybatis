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
package org.springframework.data.mybatis.mapping;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Scans packages for entities. The entity classes annotated with
 * {@link #getEntityAnnotations()} will be selected.
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class MybatisEntityClassScanner {

	private Set<String> entityBasePackages = new HashSet<>();

	private Set<Class<?>> entityBasePackageClasses = new HashSet<>();

	private @Nullable ClassLoader beanClassLoader;

	public static Set<Class<?>> scan(String... entityBasePackages) throws ClassNotFoundException {
		return new MybatisEntityClassScanner(entityBasePackages).scanForEntityClasses();
	}

	public static Set<Class<?>> scan(Class<?>... entityBasePackageClasses) throws ClassNotFoundException {
		return new MybatisEntityClassScanner(entityBasePackageClasses).scanForEntityClasses();
	}

	public static Set<Class<?>> scan(Collection<String> entityBasePackages) throws ClassNotFoundException {
		return new MybatisEntityClassScanner(entityBasePackages).scanForEntityClasses();
	}

	public static Set<Class<?>> scan(Collection<String> entityBasePackages,
			Collection<Class<?>> entityBasePackageClasses) throws ClassNotFoundException {
		return new MybatisEntityClassScanner(entityBasePackages, entityBasePackageClasses).scanForEntityClasses();
	}

	public MybatisEntityClassScanner() {
	}

	public MybatisEntityClassScanner(Class<?>... entityBasePackages) {
		this.setEntityBasePackageClasses(Arrays.asList(entityBasePackages));
	}

	public MybatisEntityClassScanner(String... entityBasePackages) {
		this(Arrays.asList(entityBasePackages));
	}

	public MybatisEntityClassScanner(Collection<String> entityBasePackages) {
		this.setEntityBasePackages(entityBasePackages);
	}

	public MybatisEntityClassScanner(Collection<String> entityBasePackages,
			Collection<Class<?>> entityBasePackageClasses) {
		this.setEntityBasePackages(entityBasePackages);
		this.setEntityBasePackageClasses(entityBasePackageClasses);
	}

	public Set<Class<?>> scanForEntityClasses() throws ClassNotFoundException {
		Set<Class<?>> classes = new HashSet<>();
		for (String basePackage : this.getEntityBasePackages()) {
			classes.addAll(this.scanBasePackageForEntities(basePackage));
		}

		for (Class<?> basePackageClass : this.getEntityBasePackageClasses()) {
			classes.addAll(this.scanBasePackageForEntities(basePackageClass.getPackage().getName()));
		}

		return classes;
	}

	protected Set<Class<?>> scanBasePackageForEntities(String basePackage) throws ClassNotFoundException {
		HashSet<Class<?>> classes = new HashSet<>();
		if (StringUtils.isEmpty(basePackage)) {
			return classes;
		}

		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		for (Class<? extends Annotation> annotation : this.getEntityAnnotations()) {
			componentProvider.addIncludeFilter(new AnnotationTypeFilter(annotation));
		}

		for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
			if (null != candidate.getBeanClassName()) {
				classes.add(ClassUtils.forName(candidate.getBeanClassName(), this.beanClassLoader));
			}
		}
		return classes;
	}

	protected Class<? extends Annotation>[] getEntityAnnotations() {
		return new Class[] { Entity.class };
	}

	public Set<String> getEntityBasePackages() {
		return Collections.unmodifiableSet(this.entityBasePackages);
	}

	public void setEntityBasePackages(Collection<String> entityBasePackages) {
		this.entityBasePackages = new HashSet<>(entityBasePackages);
	}

	public Set<Class<?>> getEntityBasePackageClasses() {
		return Collections.unmodifiableSet(this.entityBasePackageClasses);
	}

	public void setEntityBasePackageClasses(Collection<Class<?>> entityBasePackageClasses) {
		this.entityBasePackageClasses = new HashSet<>(entityBasePackageClasses);
	}

	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

}
