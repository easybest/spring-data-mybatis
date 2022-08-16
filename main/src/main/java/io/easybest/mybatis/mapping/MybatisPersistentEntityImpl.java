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

import java.util.Comparator;
import java.util.Optional;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.IdClass;
import javax.persistence.Table;

import io.easybest.mybatis.annotation.LogicDelete;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> entity type
 */
public class MybatisPersistentEntityImpl<T> extends BasicPersistentEntity<T, MybatisPersistentPropertyImpl>
		implements MybatisPersistentEntity<T> {

	private final EntityManager entityManager;

	private final Lazy<SqlIdentifier> tableName;

	private boolean forceQuote = false;

	private final Lazy<Boolean> basic;

	public MybatisPersistentEntityImpl(TypeInformation<T> information, EntityManager entityManager) {
		this(information, entityManager, (o1, o2) -> o1.isIdProperty() ? (o2.isIdProperty() ? 0 : -1) : 1);
	}

	public MybatisPersistentEntityImpl(TypeInformation<T> information, EntityManager entityManager,
			Comparator<MybatisPersistentPropertyImpl> comparator) {

		super(information, comparator);
		this.entityManager = entityManager;

		this.tableName = Lazy.of(() -> {
			Table tableAnn = this.findAnnotation(Table.class);
			if (null == tableAnn) {
				return this.createSqlIdentifier(this.getEntityName());
			}

			String name = (null != entityManager.getUniformTablePrefix() ? entityManager.getUniformTablePrefix() : "")
					+ (StringUtils.hasText(tableAnn.name()) ? tableAnn.name() : this.getEntityName());
			SqlIdentifier tableName = this.createSqlIdentifier(name);
			return

			(StringUtils.hasText(tableAnn.schema())
					? SqlIdentifier.from(this.createDerivedSqlIdentifier(tableAnn.schema()), tableName) : tableName);
		});

		this.basic = Lazy.of(() -> StreamUtils.createStreamFromIterator(this.iterator())
				.noneMatch(MybatisPersistentPropertyImpl::isAssociation));
	}

	@Override
	public String getEntityName() {
		return Optional.ofNullable(this.findAnnotation(Entity.class)).map(Entity::name).filter(StringUtils::hasText)
				.orElse(this.getType().getSimpleName());
	}

	@Override
	public boolean isCompositeId() {
		if (this.isAnnotationPresent(IdClass.class)) {
			return true;
		}
		return this.hasIdProperty() && this.getRequiredIdProperty().isAnnotationPresent(EmbeddedId.class);
	}

	@Override
	public boolean isBasic() {
		return this.basic.get();
	}

	@Override
	public Optional<String> getLogicDeleteColumn() {
		return Optional.ofNullable(this.findAnnotation(LogicDelete.class)).map(LogicDelete::value);
	}

	public GenerationType getGenerationType() {
		if (!this.hasIdProperty() || this.isCompositeId()) {
			return null;
		}
		MybatisPersistentPropertyImpl idProperty = this.getRequiredIdProperty();
		GeneratedValue gv = idProperty.findAnnotation(GeneratedValue.class);
		if (null == gv) {
			return null;
		}

		if (gv.strategy() == GenerationType.IDENTITY || (gv.strategy() == GenerationType.AUTO && GenerationType.IDENTITY
				.name().equalsIgnoreCase(this.entityManager.getDialect().getNativeIdentifierGeneratorStrategy()))) {
			return GenerationType.IDENTITY;
		}
		else if (gv.strategy() == GenerationType.SEQUENCE
				|| (gv.strategy() == GenerationType.AUTO && GenerationType.SEQUENCE.name()
						.equalsIgnoreCase(this.entityManager.getDialect().getNativeIdentifierGeneratorStrategy()))) {
			return GenerationType.SEQUENCE;
		}
		return null;
	}

	private SqlIdentifier createSqlIdentifier(String name) {
		return this.isForceQuote() ? SqlIdentifier.quoted(name) : SqlIdentifier.unquoted(name);
	}

	private SqlIdentifier createDerivedSqlIdentifier(String name) {
		return new DerivedSqlIdentifier(name, this.isForceQuote());
	}

	@Override
	public SqlIdentifier getTableName() {
		return this.tableName.get();
	}

	public boolean isForceQuote() {
		return this.forceQuote;
	}

	public void setForceQuote(boolean forceQuote) {
		this.forceQuote = forceQuote;
	}

	@Override
	public String toString() {
		return "MybatisPersistentEntityImpl{" + "tableName=" + this.getTableName() + ", forceQuote="
				+ this.isForceQuote() + ", basic=" + this.isBasic() + '}';
	}

}
