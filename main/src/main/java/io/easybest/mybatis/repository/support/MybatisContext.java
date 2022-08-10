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

package io.easybest.mybatis.repository.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.sql.Identifier;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * {@link MybatisContext} instances get passed to MyBatis mapped statements as arguments.
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <ID> primary key
 */
public class MybatisContext<T, ID> implements Serializable {

	private static final long serialVersionUID = -897131812481903913L;

	/**
	 * Instance.
	 */
	public static final String PARAM_INSTANCE_PREFIX = "instance.";

	/**
	 * Bindable parameter prefix.
	 */
	public static final String PARAM_BINDABLE_PREFIX = "bindable.";

	/**
	 * Additional Values.
	 */
	public static final String PARAM_ADDITIONAL_VALUES_PREFIX = "additionalValues.";

	/**
	 * PARAM_PAGEABLE_PREFIX.
	 */
	public static final String PARAM_PAGEABLE_PREFIX = "pageable.";

	private @Nullable ID id;

	private @Nullable T instance;

	private @Nullable Identifier identifier;

	private @Nullable Class<?> domainType;

	private Map<String, Object> additionalValues;

	private Pageable pageable;

	private Sort sort;

	private Example<?> example;

	private EntityManager entityManager;

	private Map<String, Object> bindable;

	private boolean basic;

	public MybatisContext() {
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType,
			Map<String, Object> additionalValues, boolean basic) {

		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = additionalValues;
		this.pageable = null;
		this.sort = null;
		this.example = null;
		this.basic = basic;
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType, boolean basic) {

		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = Collections.emptyMap();
		this.pageable = null;
		this.sort = null;
		this.example = null;
		this.basic = basic;
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType,
			Map<String, Object> additionalValues, Pageable pageable, Sort sort, boolean basic) {

		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = additionalValues;
		this.pageable = pageable;
		this.sort = sort;
		this.example = null;
		this.basic = basic;
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType,
			Map<String, Object> additionalValues, Pageable pageable, Sort sort, Example<?> example, boolean basic) {

		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = additionalValues;
		this.pageable = pageable;
		this.sort = sort;
		this.example = example;
		this.basic = basic;
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType,
			Map<String, Object> additionalValues, Pageable pageable, Sort sort, Example<?> example,
			EntityManager entityManager, boolean basic) {

		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = additionalValues;
		this.pageable = pageable;
		this.sort = sort;
		this.example = example;
		this.basic = basic;
		this.entityManager = entityManager;
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType,
			Map<String, Object> additionalValues, Pageable pageable, Sort sort, EntityManager entityManager,
			boolean basic) {

		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = additionalValues;
		this.pageable = pageable;
		this.sort = sort;
		this.example = null;
		this.basic = basic;
		this.entityManager = entityManager;
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType, Pageable pageable,
			Sort sort, EntityManager entityManager, boolean basic) {

		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = Collections.emptyMap();
		this.pageable = pageable;
		this.sort = sort;
		this.example = null;
		this.basic = basic;
		this.entityManager = entityManager;
	}

	public MybatisContext(@Nullable ID id, @Nullable T instance, @Nullable Class<?> domainType, Sort sort,
			EntityManager entityManager, boolean basic) {
		this.id = id;
		this.identifier = null;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = Collections.emptyMap();
		this.pageable = null;
		this.sort = sort;
		this.example = null;
		this.basic = basic;
		this.entityManager = entityManager;
	}

	public MybatisContext(Identifier identifier, @Nullable T instance, @Nullable Class<?> domainType, Pageable pageable,
			Sort sort, Example<?> example, boolean basic) {

		this.id = null;
		this.identifier = identifier;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = Collections.emptyMap();
		this.pageable = pageable;
		this.sort = sort;
		this.example = example;
		this.basic = basic;
	}

	public MybatisContext(Identifier identifier, @Nullable T instance, @Nullable Class<?> domainType, Pageable pageable,
			Sort sort, Example<?> example, EntityManager entityManager, boolean basic) {

		this.id = null;
		this.identifier = identifier;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = Collections.emptyMap();
		this.pageable = pageable;
		this.sort = sort;
		this.example = example;
		this.basic = basic;
		this.entityManager = entityManager;
	}

	public MybatisContext(Identifier identifier, @Nullable T instance, @Nullable Class<?> domainType,
			Example<?> example, EntityManager entityManager, boolean basic) {

		this.id = null;
		this.identifier = identifier;
		this.instance = instance;
		this.domainType = domainType;
		this.additionalValues = Collections.emptyMap();
		this.pageable = null;
		this.sort = null;
		this.example = example;
		this.basic = basic;
		this.entityManager = entityManager;
	}

	public MybatisContext<T, ID> setBindable(String key, Object value) {

		if (null == this.bindable) {
			this.bindable = new HashMap<>();
		}
		this.bindable.put(key, value);
		return this;
	}

	public String getDtype() {

		if (null == this.domainType) {
			return null;
		}

		if (null != this.example) {

			Class<?> probeType = this.example.getProbeType();
			if (probeType == this.domainType) {
				return null;
			}

			if (this.domainType.isAssignableFrom(probeType)) {
				return probeType.getSimpleName();
			}

		}

		return null;
	}

	/**
	 * Returns a value for the given key.
	 * @param key must not be {@code null}
	 * @return might return {@code null}
	 */
	@Nullable
	public Object get(String key) {

		Object value = null;
		if (this.identifier != null) {
			value = this.identifier.toMap().get(key);
		}

		return value == null ? this.additionalValues.get(key) : value;
	}

	@Nullable
	public ID getId() {
		return this.id;
	}

	@Nullable
	public T getInstance() {
		return this.instance;
	}

	public void setId(@Nullable ID id) {
		this.id = id;
	}

	public void setInstance(@Nullable T instance) {
		this.instance = instance;
	}

	public Map<String, Object> getAdditionalValues() {
		return this.additionalValues;
	}

	public void setAdditionalValues(Map<String, Object> additionalValues) {
		this.additionalValues = additionalValues;
	}

	@Nullable
	public Class<?> getDomainType() {
		return this.domainType;
	}

	public Class<?> getRequiredDomainType() {
		Class<?> type = this.getDomainType();
		if (null == type) {
			throw new IllegalArgumentException("Domain type is required!");
		}
		return type;
	}

	public void setDomainType(@Nullable Class<?> domainType) {
		this.domainType = domainType;
	}

	@Nullable
	public Identifier getIdentifier() {
		return this.identifier;
	}

	public void setIdentifier(@Nullable Identifier identifier) {
		this.identifier = identifier;
	}

	public EntityManager getEntityManager() {
		return this.entityManager;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public boolean isBasic() {
		return this.basic;
	}

	public void setBasic(boolean basic) {
		this.basic = basic;
	}

	public Pageable getPageable() {
		return this.pageable;
	}

	public void setPageable(Pageable pageable) {
		this.pageable = pageable;
	}

	public Sort getSort() {
		return this.sort;
	}

	public void setSort(Sort sort) {
		this.sort = sort;
	}

	public Example<?> getExample() {
		return this.example;
	}

	public void setExample(Example<?> example) {
		this.example = example;
	}

	public Map<String, Object> getBindable() {

		if (null == this.bindable) {
			return Collections.emptyMap();
		}
		return this.bindable;
	}

}
