/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.auditing;

import java.lang.annotation.Annotation;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.auditing.AuditableBeanWrapper;
import org.springframework.data.auditing.AuditableBeanWrapperFactory;
import org.springframework.data.domain.Auditable;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link AuditableBeanWrapperFactory} that will create am {@link AuditableBeanWrapper}
 * using mapping information obtained from a {@link MappingContext} to detect auditing
 * configuration and eventually invoking setting the auditing values.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @since 1.8
 */
public class MappingAuditableBeanWrapperFactory
		extends DefaultAuditableBeanWrapperFactory {

	private final PersistentEntities entities;

	private final Map<Class<?>, MappingAuditingMetadata> metadataCache;

	/**
	 * Creates a new {@link MappingAuditableBeanWrapperFactory} using the given
	 * {@link PersistentEntities}.
	 * @param entities must not be {@literal null}.
	 */
	public MappingAuditableBeanWrapperFactory(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.entities = entities;
		this.metadataCache = new ConcurrentReferenceHashMap<>();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.data.auditing.AuditableBeanWrapperFactory#getBeanWrapperFor(
	 * java.lang.Object)
	 */
	@Override
	public <T> Optional<AuditableBeanWrapper<T>> getBeanWrapperFor(T source) {

		return Optional.of(source).flatMap(it -> {

			if (it instanceof Auditable) {
				return super.getBeanWrapperFor(source);
			}

			return entities.mapOnContext(it.getClass(), (context, entity) -> {

				MappingAuditingMetadata metadata = metadataCache.computeIfAbsent(
						it.getClass(),
						key -> new MappingAuditingMetadata(context, it.getClass()));

				return Optional.<AuditableBeanWrapper<T>>ofNullable(metadata.isAuditable() //
						? new MappingMetadataAuditableBeanWrapper<T>(
								entity.getPropertyAccessor(it), metadata)
						: null);

			}).orElseGet(() -> super.getBeanWrapperFor(source));
		});
	}

	/**
	 * Captures {@link PersistentProperty} instances equipped with auditing annotations.
	 *
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	static class MappingAuditingMetadata {

		private final PersistentPropertyPaths<?, ? extends PersistentProperty<?>> createdByPaths,
				createdDatePaths, lastModifiedByPaths, lastModifiedDatePaths;

		private final Lazy<Boolean> isAuditable;

		/**
		 * Creates a new {@link MappingAuditingMetadata} instance from the given
		 * {@link PersistentEntity}.
		 */
		public <P> MappingAuditingMetadata(
				MappingContext<?, ? extends PersistentProperty<?>> context,
				Class<?> type) {

			Assert.notNull(type, "Type must not be null!");

			this.createdByPaths = context.findPersistentPropertyPaths(type,
					withAnnotation(CreatedBy.class,
							org.springframework.data.mybatis.annotation.CreatedBy.class));
			this.createdDatePaths = context.findPersistentPropertyPaths(type,
					withAnnotation(CreatedDate.class,
							org.springframework.data.mybatis.annotation.CreatedDate.class));
			this.lastModifiedByPaths = context.findPersistentPropertyPaths(type,
					withAnnotation(LastModifiedBy.class,
							org.springframework.data.mybatis.annotation.LastModifiedBy.class));
			this.lastModifiedDatePaths = context.findPersistentPropertyPaths(type,
					withAnnotation(LastModifiedDate.class,
							org.springframework.data.mybatis.annotation.LastModifiedDate.class));

			this.isAuditable = Lazy.of( //
					() -> Arrays
							.asList(createdByPaths, createdDatePaths, lastModifiedByPaths,
									lastModifiedDatePaths) //
							.stream() //
							.anyMatch(it -> !it.isEmpty())//
			);
		}

		private static Predicate<PersistentProperty<?>> withAnnotation(
				Class<? extends Annotation>... types) {

			return t -> Stream.of(types).anyMatch(type -> null != t.findAnnotation(type));
		}

		/**
		 * Returns whether the {@link PersistentEntity} is auditable at all (read: any of
		 * the auditing annotations is present).
		 * @return
		 */
		public boolean isAuditable() {
			return isAuditable.get();
		}

	}

	/**
	 * {@link AuditableBeanWrapper} using {@link MappingAuditingMetadata} and a
	 * {@link PersistentPropertyAccessor} to set values on auditing properties.
	 *
	 * @author Oliver Gierke
	 * @since 1.8
	 */
	static class MappingMetadataAuditableBeanWrapper<T>
			extends DateConvertingAuditableBeanWrapper<T> {

		private final PersistentPropertyAccessor<T> accessor;

		private final MappingAuditingMetadata metadata;

		/**
		 * Creates a new {@link MappingMetadataAuditableBeanWrapper} for the given target
		 * and {@link MappingAuditingMetadata}.
		 * @param accessor must not be {@literal null}.
		 * @param metadata must not be {@literal null}.
		 */
		public MappingMetadataAuditableBeanWrapper(PersistentPropertyAccessor<T> accessor,
				MappingAuditingMetadata metadata) {

			Assert.notNull(accessor, "PersistentPropertyAccessor must not be null!");
			Assert.notNull(metadata, "Auditing metadata must not be null!");

			this.accessor = accessor;
			this.metadata = metadata;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.data.auditing.AuditableBeanWrapper#setCreatedBy(java.util.
		 * Optional)
		 */
		@Override
		public Object setCreatedBy(Object value) {

			metadata.createdByPaths.forEach(it -> this.accessor.setProperty(it, value));

			return value;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.data.auditing.AuditableBeanWrapper#setCreatedDate(java.util
		 * .Optional)
		 */
		@Override
		public TemporalAccessor setCreatedDate(TemporalAccessor value) {
			return setDateProperty(metadata.createdDatePaths, value);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedBy(java.
		 * util.Optional)
		 */
		@Override
		public Object setLastModifiedBy(Object value) {
			return setProperty(metadata.lastModifiedByPaths, value);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.data.auditing.AuditableBeanWrapper#getLastModifiedDate()
		 */
		@Override
		public Optional<TemporalAccessor> getLastModifiedDate() {

			Optional<Object> firstValue = metadata.lastModifiedDatePaths.getFirst() //
					.map(accessor::getProperty);

			return getAsTemporalAccessor(firstValue, TemporalAccessor.class);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.data.auditing.AuditableBeanWrapper#setLastModifiedDate(java
		 * .util.Optional)
		 */
		@Override
		public TemporalAccessor setLastModifiedDate(TemporalAccessor value) {
			return setDateProperty(metadata.lastModifiedDatePaths, value);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.springframework.data.auditing.AuditableBeanWrapper#getBean()
		 */
		@Override
		public T getBean() {
			return accessor.getBean();
		}

		private <S, P extends PersistentProperty<?>> S setProperty(
				PersistentPropertyPaths<?, ? extends PersistentProperty<?>> paths,
				S value) {

			paths.forEach(it -> this.accessor.setProperty(it, value));

			return value;
		}

		private <P extends PersistentProperty<?>> TemporalAccessor setDateProperty(
				PersistentPropertyPaths<?, ? extends PersistentProperty<?>> property,
				TemporalAccessor value) {

			property.forEach(it -> this.accessor.setProperty(it, getDateValueToSet(value,
					it.getRequiredLeafProperty().getType(), accessor.getBean())));

			return value;
		}

	}

}
