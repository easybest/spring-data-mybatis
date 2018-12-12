package org.springframework.data.mybatis.auditing;

import java.time.temporal.TemporalAccessor;
import java.util.Optional;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.auditing.AuditableBeanWrapper;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.Auditable;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MybatisAuditingHandler implements InitializingBean {

	private final DefaultAuditableBeanWrapperFactory factory;

	private DateTimeProvider dateTimeProvider = CurrentDateTimeProvider.INSTANCE;

	private Optional<AuditorAware<?>> auditorAware;

	private boolean dateTimeForNow = true;

	private boolean modifyOnCreation = true;

	/**
	 * Creates a new {@link AuditableBeanWrapper} using the given {@link MappingContext}
	 * when looking up auditing metadata via reflection.
	 * @param mappingContext must not be {@literal null}.
	 * @since 1.8
	 * @deprecated use {@link AuditingHandler ( PersistentEntities )} instead.
	 */
	@Deprecated
	public MybatisAuditingHandler(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
		this(PersistentEntities.of(mappingContext));
	}

	/**
	 * Creates a new {@link AuditableBeanWrapper} using the given
	 * {@link PersistentEntities} when looking up auditing metadata via reflection.
	 * @param entities must not be {@literal null}.
	 * @since 1.10
	 */
	public MybatisAuditingHandler(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.factory = new MappingAuditableBeanWrapperFactory(entities);
		this.auditorAware = Optional.empty();
	}

	/**
	 * Setter to inject a {@code AuditorAware} component to retrieve the current auditor.
	 * @param auditorAware must not be {@literal null}.
	 */
	public void setAuditorAware(AuditorAware<?> auditorAware) {

		Assert.notNull(auditorAware, "AuditorAware must not be null!");
		this.auditorAware = Optional.of(auditorAware);
	}

	/**
	 * Setter do determine if {@link Auditable#setCreatedDate(TemporalAccessor)} and
	 * {@link Auditable#setLastModifiedDate(TemporalAccessor)} shall be filled with the
	 * current Java time. Defaults to {@code true}. One might set this to {@code false} to
	 * use database features to set entity time.
	 * @param dateTimeForNow the dateTimeForNow to set
	 */
	public void setDateTimeForNow(boolean dateTimeForNow) {
		this.dateTimeForNow = dateTimeForNow;
	}

	/**
	 * Set this to false if you want to treat entity creation as modification and thus set
	 * the current date as modification date, too. Defaults to {@code true}.
	 * @param modifyOnCreation if modification information shall be set on creation, too
	 */
	public void setModifyOnCreation(boolean modifyOnCreation) {
		this.modifyOnCreation = modifyOnCreation;
	}

	/**
	 * Sets the {@link DateTimeProvider} to be used to determine the dates to be set.
	 * @param dateTimeProvider
	 */
	public void setDateTimeProvider(DateTimeProvider dateTimeProvider) {
		this.dateTimeProvider = dateTimeProvider == null
				? CurrentDateTimeProvider.INSTANCE : dateTimeProvider;
	}

	/**
	 * Marks the given object as created.
	 * @param source
	 */
	public <T> T markCreated(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return touch(source, true);
	}

	/**
	 * Marks the given object as modified.
	 * @param source
	 */
	public <T> T markModified(T source) {

		Assert.notNull(source, "Entity must not be null!");

		return touch(source, false);
	}

	/**
	 * Returns whether the given source is considered to be auditable in the first place
	 * @param source must not be {@literal null}.
	 * @return
	 */
	protected final boolean isAuditable(Object source) {

		Assert.notNull(source, "Source must not be null!");

		return factory.getBeanWrapperFor(source).isPresent();
	}

	private <T> T touch(T target, boolean isNew) {

		Optional<AuditableBeanWrapper<T>> wrapper = factory.getBeanWrapperFor(target);

		return wrapper.map(it -> {

			Optional<Object> auditor = touchAuditor(it, isNew);
			Optional<TemporalAccessor> now = dateTimeForNow ? touchDate(it, isNew)
					: Optional.empty();

			if (log.isDebugEnabled()) {

				Object defaultedNow = now.map(Object::toString).orElse("not set");
				Object defaultedAuditor = auditor.map(Object::toString).orElse("unknown");

				log.debug("Touched {} - Last modification at {} by {}", target,
						defaultedNow, defaultedAuditor);
			}

			return it.getBean();

		}).orElse(target);
	}

	/**
	 * Sets modifying and creating auditor. Creating auditor is only set on new
	 * auditables.
	 * @return
	 */
	private Optional<Object> touchAuditor(AuditableBeanWrapper<?> wrapper,
			boolean isNew) {

		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");

		return auditorAware.map(it -> {

			Optional<?> auditor = it.getCurrentAuditor();

			Assert.notNull(auditor,
					() -> String.format("Auditor must not be null! Returned by: %s!",
							AopUtils.getTargetClass(it)));

			auditor.filter(__ -> isNew).ifPresent(foo -> wrapper.setCreatedBy(foo));
			auditor.filter(__ -> !isNew || modifyOnCreation)
					.ifPresent(foo -> wrapper.setLastModifiedBy(foo));

			return auditor;
		});
	}

	/**
	 * Touches the auditable regarding modification and creation date. Creation date is
	 * only set on new auditables.
	 * @param wrapper
	 * @return
	 */
	private Optional<TemporalAccessor> touchDate(AuditableBeanWrapper<?> wrapper,
			boolean isNew) {

		Assert.notNull(wrapper, "AuditableBeanWrapper must not be null!");

		Optional<TemporalAccessor> now = dateTimeProvider.getNow();

		Assert.notNull(now, () -> String.format("Now must not be null! Returned by: %s!",
				dateTimeProvider.getClass()));

		now.filter(__ -> isNew).ifPresent(it -> wrapper.setCreatedDate(it));
		now.filter(__ -> !isNew || modifyOnCreation)
				.ifPresent(it -> wrapper.setLastModifiedDate(it));

		return now;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {

		if (!auditorAware.isPresent()) {
			log.debug("No AuditorAware set! Auditing will not be applied!");
		}
	}

}
