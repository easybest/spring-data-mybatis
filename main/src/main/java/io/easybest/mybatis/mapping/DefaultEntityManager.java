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

package io.easybest.mybatis.mapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import io.easybest.mybatis.dialect.Dialect;
import io.easybest.mybatis.mapping.precompile.StagingMappers;
import io.easybest.mybatis.repository.config.DialectResolver;
import io.easybest.mybatis.repository.query.EscapeCharacter;
import io.easybest.mybatis.repository.support.MybatisContext;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.AnnotatedTypeScanner;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StopWatch;

import static io.easybest.mybatis.mapping.precompile.MybatisAggregateRootPrecompile.compile;

/**
 * Entity manager.
 *
 * @author Jarvis Song
 */
@Slf4j
public class DefaultEntityManager
		extends AbstractMappingContext<MybatisPersistentEntityImpl<?>, MybatisPersistentPropertyImpl>
		implements EntityManager, InitializingBean, DisposableBean {

	private final SqlSessionTemplate sqlSessionTemplate;

	private Dialect dialect;

	private String[] entityPackages;

	private NamingStrategy namingStrategy = NamingStrategy.INSTANCE;

	private final Map<String, String> namedQueries = new HashMap<>();

	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

	public DefaultEntityManager(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	@Override
	public void afterPropertiesSet() {

		log.info("Initializing Spring Data Mybatis EntityManager...");

		StopWatch sw = new StopWatch();
		sw.start();

		if (null == this.dialect) {
			this.dialect = DialectResolver.getDialect(this.sqlSessionTemplate);
			log.info("Auto detected dialect " + this.dialect.getClass().getSimpleName());
		}

		if (null != this.entityPackages) {
			Set<Class<?>> types = new AnnotatedTypeScanner(Entity.class).findTypes(this.entityPackages);
			this.setInitialEntitySet(types);
		}

		super.afterPropertiesSet();

		// enhance mybatis configuration
		this.getSqlSessionTemplate().getConfiguration().getTypeAliasRegistry()
				.registerAlias(MybatisContext.class.getSimpleName(), MybatisContext.class);
		this.getSqlSessionTemplate().getConfiguration().setObjectFactory(new MappingObjectFactory(this));

		Collection<MybatisPersistentEntityImpl<?>> persistentEntities = this.getPersistentEntities();
		StagingMappers stagingMappers = new StagingMappers();
		// prepare named queries
		persistentEntities.forEach(this::addNamedQueries);
		// precompile persistent entities
		persistentEntities.stream().filter(entity -> entity.isAnnotationPresent(Entity.class))
				.forEach(entity -> compile(this, stagingMappers, entity));

		compile(this, stagingMappers);
		stagingMappers.complete();

		sw.stop();
		log.info("Finished initializing EntityManager in " + sw.getTotalTimeMillis() + " ms, and "
				+ persistentEntities.size() + " entities were found.");
	}

	@Override
	protected <T> MybatisPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		return new MybatisPersistentEntityImpl<>(typeInformation, this);
	}

	@Override
	protected MybatisPersistentPropertyImpl createPersistentProperty(Property property,
			MybatisPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {

		return new MybatisPersistentPropertyImpl(property, owner, simpleTypeHolder, this.namingStrategy, this);
	}

	@Override
	public SqlSessionTemplate getSqlSessionTemplate() {
		return this.sqlSessionTemplate;
	}

	@Override
	public Dialect getDialect() {
		return this.dialect;
	}

	@Override
	public String getNamedQuery(String name) {
		return this.namedQueries.get(name);
	}

	@Override
	public void destroy() throws Exception {
		log.info("Destroyed Spring Data Mybatis EntityManager.");
	}

	private void addNamedQueries(MybatisPersistentEntityImpl<?> entity) {
		NamedQuery namedQueryAnn = entity.findAnnotation(NamedQuery.class);
		if (null != namedQueryAnn) {
			this.namedQueries.put(namedQueryAnn.name(), namedQueryAnn.query());
		}
		NamedQueries namedQueriesAnn = entity.findAnnotation(NamedQueries.class);
		if (null != namedQueriesAnn) {
			this.namedQueries.putAll(Arrays.stream(namedQueriesAnn.value())
					.collect(Collectors.toMap(NamedQuery::name, NamedQuery::query)));
		}
		NamedNativeQuery namedNativeQueryAnn = entity.findAnnotation(NamedNativeQuery.class);
		if (null != namedNativeQueryAnn) {
			this.namedQueries.put(namedNativeQueryAnn.name(), namedNativeQueryAnn.query());
		}
		NamedNativeQueries namedNativeQueriesAnn = entity.findAnnotation(NamedNativeQueries.class);
		if (null != namedNativeQueriesAnn) {
			this.namedQueries.putAll(Arrays.stream(namedNativeQueriesAnn.value())
					.collect(Collectors.toMap(NamedNativeQuery::name, NamedNativeQuery::query)));
		}
	}

	@Override
	public EscapeCharacter getEscapeCharacter() {
		return this.escapeCharacter;
	}

	public void setEntityPackages(String[] entityPackages) {
		this.entityPackages = entityPackages;
	}

	public void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	public void setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	public void setEscapeCharacter(EscapeCharacter escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

}
