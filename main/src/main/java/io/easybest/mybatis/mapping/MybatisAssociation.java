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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.sql.IdentifierProcessing;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import io.easybest.mybatis.repository.Fetch;
import io.easybest.mybatis.repository.FetchMode;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.util.Lazy;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static io.easybest.mybatis.mapping.MybatisAssociation.Type.MANY2MANY;
import static io.easybest.mybatis.mapping.MybatisAssociation.Type.MANY2ONE;
import static io.easybest.mybatis.mapping.MybatisAssociation.Type.ONE2MANY;
import static io.easybest.mybatis.mapping.MybatisAssociation.Type.ONE2ONE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_ID;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisAssociation extends Association<MybatisPersistentPropertyImpl> {

	private final EntityManager entityManager;

	private final boolean bidirectional;

	private final boolean owningSide;

	private final Type type;

	private final boolean forceQuote;

	private final Lazy<List<JoinColumn>> joinColumns;

	private final Lazy<JoinTable> joinTable;

	public MybatisAssociation(MybatisPersistentPropertyImpl inverse, MybatisPersistentPropertyImpl obverse,
			EntityManager entityManager, boolean forceQuote) {

		super(inverse, obverse);

		this.entityManager = entityManager;
		this.type = Arrays.stream(Type.values()).filter(t -> inverse.isAnnotationPresent(t.getAnnotation())).findFirst()
				.orElseThrow(() -> new MappingException("No right annotation found when property is association."));
		this.forceQuote = forceQuote;
		if (this.getMappedBy().isPresent()) {
			this.bidirectional = true;
			this.owningSide = false;
		}
		else {
			this.bidirectional = false;
			this.owningSide = true;
		}

		this.joinColumns = Lazy.of(this::obtainJoinColumns);
		this.joinTable = Lazy.of(this::obtainJoinTable);
	}

	public String getSelectStatementId() {

		return this.getTargetType().getName() + '.' + this.getSelectStatementName();
	}

	public String getSelectStatementName() {

		return FIND_BY_ID;
	}

	public String getSelectMappingColumn() {

		return '{' + this.getJoinColumns().stream()
				.map(jc -> "id="
						+ jc.getColumnName().getReference(this.entityManager.getDialect().getIdentifierProcessing()))
				.collect(Collectors.joining()) + '}';
	}

	public String connect(SqlIdentifier parentAlias) {

		IdentifierProcessing identifierProcessing = this.entityManager.getDialect().getIdentifierProcessing();

		SqlIdentifier tableName = this.getTargetEntity().getTableName();

		String pname = this.getInverse().getName();
		SqlIdentifier alias = SqlIdentifier.quoted(SQL.ROOT_ALIAS.getValue().equals(parentAlias.getReference()) ? pname
				: (parentAlias.getReference() + '.' + pname));

		StringBuilder builder = new StringBuilder();
		JoinTable jt = this.getJoinTable();
		List<JoinColumn> jcs = this.getJoinColumns();
		if (null != jt) {
			SqlIdentifier joinTableAlias = SqlIdentifier.quoted(alias.getReference() + "_ass");
			builder.append(" LEFT OUTER JOIN ").append(jt.getTable().toSql(identifierProcessing)).append(" ")
					.append(joinTableAlias.toSql(identifierProcessing));
			builder.append(" ON ");
			builder.append(Arrays.stream(jt.getJoinColumns())
					.map(jc -> parentAlias.toSql(identifierProcessing) + '.'
							+ jc.getReferencedColumnName().toSql(identifierProcessing) + " = "
							+ joinTableAlias.toSql(identifierProcessing) + '.'
							+ jc.getColumnName().toSql(identifierProcessing))
					.collect(Collectors.joining(" AND ")));
			builder.append(" LEFT OUTER JOIN ").append(tableName.toSql(identifierProcessing)).append(" ")
					.append(alias.toSql(identifierProcessing)).append(" ON ");
			builder.append(Arrays.stream(jt.getInverseJoinColumns())
					.map(jc -> joinTableAlias.toSql(identifierProcessing) + '.'
							+ jc.getColumnName().getReference(identifierProcessing) + " = "
							+ alias.toSql(identifierProcessing) + '.'
							+ jc.getReferencedColumnName().getReference(identifierProcessing))
					.collect(Collectors.joining(" AND ")));
		}
		else if (!CollectionUtils.isEmpty(jcs)) {

			builder.append(" LEFT OUTER JOIN ").append(tableName.toSql(identifierProcessing)).append(" ")
					.append(alias.toSql(identifierProcessing)).append(" ON ");
			builder.append(jcs.stream().map(jc -> parentAlias.toSql(identifierProcessing) + '.'
					+ jc.getColumnName().getReference(identifierProcessing) + " = " + alias.toSql(identifierProcessing)
					+ '.' + jc.getReferencedColumnName().getReference(identifierProcessing))
					.collect(Collectors.joining(" AND ")));

		}
		return builder.toString();
	}

	private JoinTable obtainJoinTable() {

		if (!this.isToMany()) {
			return null;
		}

		MybatisPersistentPropertyImpl inverse = this.getInverse();
		if (null == inverse.getAssociationTargetType()) {
			throw new MappingException("Could not find target entity by " + inverse);
		}

		MybatisPersistentEntityImpl<?> owningEntity = inverse.getOwner();

		MybatisPersistentEntityImpl<?> targetEntity = this.entityManager
				.getRequiredPersistentEntity(inverse.getAssociationTargetType());

		Optional<String> mappedBy = this.getMappedBy();
		if (mappedBy.isPresent()) {

			if (inverse.isAnnotationPresent(OneToMany.class)) {
				// no join tables
				return null;
			}

		}

		// owning side
		String joinTableName = owningEntity.getTableName().getReference() + '_'
				+ targetEntity.getTableName().getReference();

		if (null != this.entityManager.getUniformTablePrefix()) {
			joinTableName = this.entityManager.getUniformTablePrefix() + joinTableName;
		}

		JoinColumn[] joinColumns = null;
		JoinColumn[] inverseJoinColumns = null;

		if (inverse.isAnnotationPresent(javax.persistence.JoinTable.class)) {

			javax.persistence.JoinTable joinTableAnn = inverse.getRequiredAnnotation(javax.persistence.JoinTable.class);

			if (StringUtils.hasText(joinTableAnn.name())) {
				joinTableName = joinTableAnn.name();
			}

			if (null != joinTableAnn.joinColumns() && joinTableAnn.joinColumns().length > 0) {

				joinColumns = Arrays.stream(joinTableAnn.joinColumns()).map(ann -> {

					String columnName = StringUtils.hasText(ann.name()) ? ann.name() : (owningEntity.getEntityName()
							+ '_' + owningEntity.getRequiredIdProperty().getRequiredColumnName().getReference());
					String referencedColumnName = StringUtils.hasText(ann.referencedColumnName())
							? ann.referencedColumnName()
							: (owningEntity.getRequiredIdProperty().getRequiredColumnName().getReference());

					return new JoinColumn(SqlIdentifier.unquoted(columnName),
							SqlIdentifier.unquoted(referencedColumnName));

				}).toArray(JoinColumn[]::new);
			}

			if (null != joinTableAnn.inverseJoinColumns() && joinTableAnn.inverseJoinColumns().length > 0) {
				inverseJoinColumns = Arrays.stream(joinTableAnn.inverseJoinColumns()).map(ann -> {
					String columnName = StringUtils.hasText(ann.name()) ? ann.name() : (inverse.getName() + '_'
							+ owningEntity.getRequiredIdProperty().getRequiredColumnName().getReference());
					String referencedColumnName = StringUtils.hasText(ann.referencedColumnName())
							? ann.referencedColumnName()
							: (targetEntity.getRequiredIdProperty().getRequiredColumnName().getReference());

					return new JoinColumn(SqlIdentifier.unquoted(columnName),
							SqlIdentifier.unquoted(referencedColumnName));
				}).toArray(JoinColumn[]::new);
			}

		}

		if (null == joinColumns || joinColumns.length == 0) {
			// TODO composite primary key
			joinColumns = new JoinColumn[] { new JoinColumn(
					SqlIdentifier.unquoted(owningEntity.getEntityName() + '_'
							+ owningEntity.getRequiredIdProperty().getRequiredColumnName().getReference()),
					SqlIdentifier
							.unquoted(owningEntity.getRequiredIdProperty().getRequiredColumnName().getReference())) };
		}
		if (null == inverseJoinColumns || inverseJoinColumns.length == 0) {
			// TODO composite primary key
			inverseJoinColumns = new JoinColumn[] { new JoinColumn(
					SqlIdentifier.unquoted(inverse.getName() + '_'
							+ owningEntity.getRequiredIdProperty().getRequiredColumnName().getReference()),
					SqlIdentifier
							.unquoted(targetEntity.getRequiredIdProperty().getRequiredColumnName().getReference())) };
		}

		return new JoinTable(SqlIdentifier.unquoted(joinTableName), joinColumns, inverseJoinColumns);
	}

	private List<JoinColumn> obtainJoinColumns() {

		List<JoinColumn> jcs = new LinkedList<>();

		MybatisPersistentPropertyImpl inverse = this.getInverse();
		if (null == inverse.getAssociationTargetType()) {
			throw new MappingException("Could not find target entity by " + inverse);
		}

		MybatisPersistentEntityImpl<?> targetEntity = this.entityManager
				.getRequiredPersistentEntity(inverse.getAssociationTargetType());

		Optional<String> mappedBy = this.getMappedBy();
		if (mappedBy.isPresent()) {
			MybatisPersistentPropertyImpl mapped = targetEntity.getRequiredPersistentProperty(mappedBy.get());
			List<JoinColumn> mappedJoinColumns = mapped.getRequiredAssociation().getJoinColumns();

			if (!CollectionUtils.isEmpty(mappedJoinColumns)) {
				for (JoinColumn sample : mappedJoinColumns) {
					JoinColumn jc = new JoinColumn();
					jc.setColumnName(sample.getReferencedColumnName());
					jc.setReferencedColumnName(sample.getColumnName());
					this.entityManager
							.findPersistentPropertyPaths(targetEntity.getType(),
									p -> null != p.getColumnName() && p.getRequiredColumnName().getReference()
											.equalsIgnoreCase(jc.getReferencedColumnName().getReference()))
							.getFirst().ifPresent(jc::setReferencedPropertyPath);
					jcs.add(jc);
				}

			}
			return jcs;
		}

		if (inverse.isAnnotationPresent(javax.persistence.JoinColumn.class)) {

			JoinColumn jc = new JoinColumn();

			javax.persistence.JoinColumn anno = inverse.getRequiredAnnotation(javax.persistence.JoinColumn.class);
			jc.setColumnName(Optional.ofNullable(anno.name()).filter(StringUtils::hasText)
					.map(this::createSqlIdentifier).orElse(this.createDerivedSqlIdentifier(inverse.getName() + '_'
							+ targetEntity.getRequiredIdProperty().getColumnName().getReference())));
			jc.setReferencedColumnName(Optional.ofNullable(anno.referencedColumnName()).filter(StringUtils::hasText)
					.map(this::createSqlIdentifier).orElse(this.createDerivedSqlIdentifier(
							targetEntity.getRequiredIdProperty().getColumnName().getReference())));

			jcs.add(jc);

		}
		else if (inverse.isAnnotationPresent(JoinColumns.class)) {
			javax.persistence.JoinColumn[] value = inverse.getRequiredAnnotation(JoinColumns.class).value();
			if (value.length > 0) {
				for (javax.persistence.JoinColumn anno : value) {
					JoinColumn jc = new JoinColumn();
					jc.setColumnName(this.createSqlIdentifier(anno.name()));
					jc.setReferencedColumnName(this.createSqlIdentifier(anno.referencedColumnName()));
					jcs.add(jc);
				}
			}
		}
		else {
			JoinColumn jc = new JoinColumn();
			jc.setColumnName(this.createDerivedSqlIdentifier(inverse.getName() + '_'
					+ targetEntity.getRequiredIdProperty().getRequiredColumnName().getReference()));
			jc.setReferencedColumnName(this.createDerivedSqlIdentifier(
					targetEntity.getRequiredIdProperty().getRequiredColumnName().getReference()));
			jcs.add(jc);
		}

		for (JoinColumn jc : jcs) {
			this.entityManager
					.findPersistentPropertyPaths(inverse.getAssociationTargetType(),
							p -> null != p.getColumnName() && p.getRequiredColumnName().getReference()
									.equalsIgnoreCase(jc.getReferencedColumnName().getReference()))
					.getFirst().ifPresent(jc::setReferencedPropertyPath);
		}

		return jcs;
	}

	public List<JoinColumn> getJoinColumns() {
		return this.joinColumns.get();
	}

	private SqlIdentifier createSqlIdentifier(String name) {
		return this.forceQuote ? SqlIdentifier.quoted(name) : SqlIdentifier.unquoted(name);
	}

	private SqlIdentifier createDerivedSqlIdentifier(String name) {
		return new DerivedSqlIdentifier(name, this.forceQuote);
	}

	public Annotation getAnnotation() {
		return this.getInverse().getRequiredAnnotation(this.getType().getAnnotation());
	}

	public Optional<String> getMappedBy() {
		String mappedBy = (String) AnnotationUtils.getValue(this.getAnnotation(), "mappedBy");
		return Optional.ofNullable(StringUtils.hasText(mappedBy) ? mappedBy.trim() : null);
	}

	public boolean isLazy() {
		return this.getFetchType() == FetchType.LAZY;
	}

	public FetchType getFetchType() {
		return (FetchType) AnnotationUtils.getValue(this.getAnnotation(), "fetch");
	}

	public Type getType() {
		return this.type;
	}

	public boolean isBidirectional() {
		return this.bidirectional;
	}

	public boolean isOwningSide() {
		return this.owningSide;
	}

	public boolean isToOne() {
		return this.type == ONE2ONE || this.type == MANY2ONE;
	}

	public boolean isToMany() {
		return this.type == MANY2MANY || this.type == ONE2MANY;
	}

	public boolean isUseJoin() {
		// TODO Whether to use join query when an association is eager and to One
		Fetch fetch = this.getInverse().findAnnotation(Fetch.class);
		if (null == fetch) {
			return false;
		}
		return fetch.value() == FetchMode.JOIN;
	}

	public JoinTable getJoinTable() {
		return this.joinTable.getNullable();
	}

	public Class<?> getTargetType() {
		return this.getInverse().getAssociationTargetType();
	}

	public MybatisPersistentEntityImpl<?> getTargetEntity() {
		return this.entityManager.getRequiredPersistentEntity(this.getTargetType());
	}

	public enum Type {

		/**
		 * OneToOne.
		 */
		ONE2ONE(OneToOne.class, OneToOne.class),
		/**
		 * OneToMany.
		 */
		ONE2MANY(OneToMany.class, ManyToOne.class),
		/**
		 * ManyToOne.
		 */
		MANY2ONE(ManyToOne.class, OneToMany.class),
		/**
		 * ManyToMany.
		 */
		MANY2MANY(ManyToMany.class, ManyToMany.class),
		/**
		 * ElementCollection.
		 */
		COLLECTION(ElementCollection.class, null);

		private final Class<? extends Annotation> annotation;

		private final Class<? extends Annotation> inverse;

		Type(Class<? extends Annotation> annotation, Class<? extends Annotation> inverse) {
			this.annotation = annotation;
			this.inverse = inverse;
		}

		public Class<? extends Annotation> getAnnotation() {
			return this.annotation;
		}

		public Class<? extends Annotation> getInverse() {
			return this.inverse;
		}

		public static Type of(Class<? extends Annotation> annotation) {
			return Arrays.stream(Type.values()).filter(t -> t.getAnnotation() == annotation).findFirst().orElse(null);
		}

	}

	public static class JoinColumn {

		private SqlIdentifier columnName;

		private SqlIdentifier referencedColumnName;

		private PersistentPropertyPath<MybatisPersistentPropertyImpl> referencedPropertyPath;

		public JoinColumn() {
		}

		public JoinColumn(SqlIdentifier columnName, SqlIdentifier referencedColumnName) {
			this.columnName = columnName;
			this.referencedColumnName = referencedColumnName;
		}

		public JoinColumn(SqlIdentifier columnName, SqlIdentifier referencedColumnName,
				PersistentPropertyPath<MybatisPersistentPropertyImpl> referencedPropertyPath) {
			this.columnName = columnName;
			this.referencedColumnName = referencedColumnName;
			this.referencedPropertyPath = referencedPropertyPath;
		}

		public SqlIdentifier getColumnName() {
			return this.columnName;
		}

		public SqlIdentifier getReferencedColumnName() {
			return this.referencedColumnName;
		}

		public void setReferencedColumnName(SqlIdentifier referencedColumnName) {
			this.referencedColumnName = referencedColumnName;
		}

		public void setColumnName(SqlIdentifier columnName) {
			this.columnName = columnName;
		}

		public PersistentPropertyPath<MybatisPersistentPropertyImpl> getReferencedPropertyPath() {
			return this.referencedPropertyPath;
		}

		public void setReferencedPropertyPath(
				PersistentPropertyPath<MybatisPersistentPropertyImpl> referencedPropertyPath) {
			this.referencedPropertyPath = referencedPropertyPath;
		}

	}

	@Data
	@NoArgsConstructor

	public static class JoinTable {

		private SqlIdentifier table;

		private JoinColumn[] joinColumns;

		private JoinColumn[] inverseJoinColumns;

		public JoinTable(SqlIdentifier table, JoinColumn[] joinColumns, JoinColumn[] inverseJoinColumns) {
			this.table = table;
			this.joinColumns = joinColumns;
			this.inverseJoinColumns = inverseJoinColumns;
		}

	}

}
