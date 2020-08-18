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
package org.springframework.data.mybatis.repository.query;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EmbeddedId;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.InvertibleLambda;
import com.samskivert.mustache.Template.Fragment;
import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mybatis.annotation.Condition;
import org.springframework.data.mybatis.annotation.Conditions;
import org.springframework.data.mybatis.annotation.Example;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2005LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2012LimitHandler;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.model.Association;
import org.springframework.data.mybatis.mapping.model.Column;
import org.springframework.data.mybatis.mapping.model.ColumnResult;
import org.springframework.data.mybatis.repository.MybatisExampleRepository;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.mybatis.repository.support.SqlSessionRepositorySupport;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Simple mybatis precompiler.
 *
 * @author JARVIS SONG
 */
class SimpleMybatisPrecompiler extends AbstractMybatisPrecompiler {

	static final String DEFAULT_SEQUENCE_NAME = "seq_spring_data_mybatis";

	SimpleMybatisPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			RepositoryInformation repositoryInformation) {
		super(mappingContext, configuration, repositoryInformation);
	}

	@Override
	protected String doPrecompile() {
		StringBuilder builder = new StringBuilder();

		// SQL Fragments
		// __base_column_list
		builder.append(this.addBaseColumnListSQL());
		// __query_by_example_where_clause
		builder.append(this.addQueryByExampleWhereClauseSQL());
		// __standard_sort
		builder.append(this.addStandardSortSQL());
		// __where_clause_by_fixed_id
		builder.append(this.addWhereClauseByFixedIdSQL());
		// __where_clause_by_entity
		builder.append(this.addWhereClauseByEntitySQL());
		// __where_clause_by_id
		builder.append(this.addWhereClauseByIdSQL());
		// __where_clause_by_ids
		builder.append(this.addWhereClauseByIdsSQL());
		// ResultMap
		builder.append(this.addResultMap());

		// Statements
		builder.append(this.addInsertStatement(true));
		builder.append(this.addInsertStatement(false));
		builder.append(this.addUpdateStatement(true, true));
		builder.append(this.addUpdateStatement(true, false));
		builder.append(this.addUpdateStatement(false, true));
		builder.append(this.addUpdateStatement(false, false));
		builder.append(this.addDeleteByIdStatement());
		builder.append(this.addDeleteByIdsStatement());
		builder.append(this.addDeleteAllStatement());
		builder.append(this.addGetByIdStatement());
		builder.append(this.addFindStatement(true));
		builder.append(this.addFindStatement(false));
		builder.append(this.addCountAllStatement());
		builder.append(this.addCountStatement());

		builder.append(this.addQueryByExample());
		builder.append(this.addQueryByExampleForPage());
		builder.append(this.addCountQueryByExample());

		if (null != this.repositoryInterface
				&& MybatisExampleRepository.class.isAssignableFrom(this.repositoryInterface)) {
			if (this.persistentEntity.isAnnotationPresent(Example.class)) {

				try {
					ClassUtils.forName(this.persistentEntity.getType().getName() + "Example",
							this.persistentEntity.getType().getClassLoader());
				}
				catch (ClassNotFoundException ex) {
					throw new MappingException(
							"Are you forget to generate " + this.persistentEntity.getType().getName() + "Example ?");
				}

				builder.append(this.addFindByExampleWhereClauseSQL());
				builder.append(this.addFindByExampleStatement());
			}
			else {
				throw new MappingException(String.format(
						"The %s extends MybatisExampleRepository, but could not find @Example on the entity: %s",
						this.repositoryInterface.getName(), this.persistentEntity.getType().getName()));
			}
		}
		return builder.toString();
	}

	protected boolean checkSqlFragment(String statementName) {
		return this.configuration.getSqlFragments()
				.containsKey(this.namespace + SqlSessionRepositorySupport.DOT + statementName);
	}

	protected boolean checkStatement(String statementName) {
		return this.configuration.hasStatement(this.namespace + SqlSessionRepositorySupport.DOT + statementName, false);
	}

	private String addBaseColumnListSQL() {
		if (this.checkSqlFragment(ResidentStatementName.BASE_COLUMN_LIST)) {
			return "";
		}
		Map<String, Object> params = new HashMap<>();
		params.put("statementName", ResidentStatementName.BASE_COLUMN_LIST);
		params.put("columns", this.mappingPropertyToColumn().values().stream()
				.map(c -> c.getName().render(this.dialect)).collect(Collectors.joining(",")));
		return render("BasicColumns", params);
	}

	private String addQueryByExampleWhereClauseSQL() {
		if (this.checkSqlFragment(ResidentStatementName.QUERY_BY_EXAMPLE_WHERE_CLAUSE)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.QUERY_BY_EXAMPLE_WHERE_CLAUSE);
		scopes.put("properties", this.mappingPropertyToColumn().entrySet());
		scopes.put("testNotNull", this.lambdaTestNotNull());
		scopes.put("dialect", this.dialect);
		scopes.put("replaceDotToUnderline", this.lambdaReplaceDotToUnderline());
		scopes.put("regexLike", this.lambdaRegexLike());
		return render("QueryByExampleWhereClause", scopes);
	}

	private String addStandardSortSQL() {
		if (this.checkSqlFragment(ResidentStatementName.STANDARD_SORT)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.STANDARD_SORT);
		scopes.put("columnsMap",
				this.mappingPropertyToColumn().entrySet().stream()
						.map(entry -> String.format("&apos;%s&apos;:&apos;%s&apos;", entry.getKey(),
								entry.getValue().getName().render(this.dialect)))
						.collect(Collectors.joining(",")));
		scopes.put("lowercaseFunction", this.dialect.getLowercaseFunction());
		return render("StandardSort", scopes);
	}

	private String addWhereClauseByFixedIdSQL() {
		if (this.checkSqlFragment(ResidentStatementName.WHERE_BY_FIXED_ID_CLAUSE)) {
			return "";
		}
		MybatisPersistentProperty idProperty = this.persistentEntity.getRequiredIdProperty();

		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.WHERE_BY_FIXED_ID_CLAUSE);
		scopes.put("embedded", idProperty.isAnnotationPresent(EmbeddedId.class));
		if ((Boolean) scopes.get("embedded")) {
			scopes.put("properties",
					this.findProperties(this.mappingContext.getRequiredPersistentEntity(idProperty.getActualType())));
		}
		else {
			scopes.put("properties", this.findProperties());
		}
		return render("WhereClauseByFixedId", scopes);
	}

	private String addWhereClauseByEntitySQL() {
		if (this.checkSqlFragment(ResidentStatementName.WHERE_BY_ENTITY_CLAUSE)) {
			return "";
		}
		MybatisPersistentProperty idProperty = this.persistentEntity.getRequiredIdProperty();

		Map<String, Object> scopes = new HashMap<>();
		scopes.put("idPropertyName", idProperty.getName());
		scopes.put("statementName", ResidentStatementName.WHERE_BY_ENTITY_CLAUSE);
		scopes.put("embedded", idProperty.isAnnotationPresent(EmbeddedId.class));
		if ((Boolean) scopes.get("embedded")) {
			scopes.put("properties",
					this.findProperties(this.mappingContext.getRequiredPersistentEntity(idProperty.getActualType())));
		}
		else {
			scopes.put("properties", this.findProperties());
		}
		return render("WhereClauseByEntity", scopes);

	}

	private String addWhereClauseByIdSQL() {
		if (this.checkSqlFragment(ResidentStatementName.WHERE_BY_ID_CLAUSE)) {
			return "";
		}
		MybatisPersistentProperty idProperty = this.persistentEntity.getRequiredIdProperty();
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("idPropertyName", idProperty.getName());
		scopes.put("statementName", ResidentStatementName.WHERE_BY_ID_CLAUSE);
		scopes.put("embedded", idProperty.isAnnotationPresent(EmbeddedId.class));
		if ((Boolean) scopes.get("embedded")) {
			scopes.put("properties",
					this.findProperties(this.mappingContext.getRequiredPersistentEntity(idProperty.getActualType())));
		}
		else {
			scopes.put("properties", this.findProperties());
		}
		return render("WhereClauseById", scopes);
	}

	private String addWhereClauseByIdsSQL() {
		if (this.checkSqlFragment(ResidentStatementName.WHERE_BY_IDS_CLAUSE)
				|| null == this.persistentEntity.getIdClass()) {
			return "";
		}
		MybatisPersistentProperty idProperty = this.persistentEntity.getRequiredIdProperty();
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.WHERE_BY_IDS_CLAUSE);
		scopes.put("idProperty", idProperty);
		scopes.put("embedded", idProperty.isAnnotationPresent(EmbeddedId.class));
		if ((Boolean) scopes.get("embedded")) {
			scopes.put("properties",
					this.findProperties(this.mappingContext.getRequiredPersistentEntity(idProperty.getActualType())));
		}
		return render("WhereClauseByIds", scopes);

	}

	private String addResultMap() {
		if (this.configuration.hasResultMap(this.namespace + '.' + ResidentStatementName.RESULT_MAP)) {
			return "";
		}

		Map<String, Object> params = new HashMap<>();
		List<ColumnResult> results = new LinkedList<>();
		List<Association> embeddedAssociations = new ArrayList<>();

		this.persistentEntity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) pp -> {
			if (pp.isAnnotationPresent(EmbeddedId.class) || pp.isEmbeddable()) {
				MybatisPersistentEntity<?> embeddedEntity = this.mappingContext
						.getRequiredPersistentEntity(pp.getActualType());

				// Notice!!! will lost data :
				// Association ass = new
				// Association().setProperty(pp.getName()).setJavaType(pp.getType().getName());
				// embeddedEntity.doWithProperties(
				// (PropertyHandler<MybatisPersistentProperty>) epp ->
				// ass.addResult(this.columnResult(epp)));
				// ass.getResults().sort((o1, o2) -> o1.isPrimaryKey() ? -1 : 1);
				// embeddedAssociations.add(ass);

				embeddedEntity.doWithProperties((PropertyHandler<MybatisPersistentProperty>) epp -> {
					ColumnResult cr = this.columnResult(epp);
					cr.setPrimaryKey(true);
					cr.setProperty(pp.getName() + "." + epp.getName());
					results.add(cr);
				});

				return;
			}
			results.add(this.columnResult(pp));
		});
		results.sort((o1, o2) -> o1.isPrimaryKey() ? -1 : 1);
		List<Association> associations = new ArrayList<>();
		this.persistentEntity.doWithAssociations((AssociationHandler<MybatisPersistentProperty>) ass -> {
			MybatisPersistentProperty inverse = ass.getInverse();
			if (inverse.isAnnotationPresent(ManyToOne.class) || inverse.isAnnotationPresent(OneToOne.class)) {
				MybatisPersistentEntity<?> targetEntity = this.mappingContext
						.getRequiredPersistentEntity(inverse.getActualType());
				Association association = new Association().setProperty(inverse.getName())
						.setJavaType(inverse.getActualType().getName());
				String fetch = null;
				ManyToOne manyToOne = inverse.findAnnotation(ManyToOne.class);
				if (null != manyToOne) {
					fetch = manyToOne.fetch().name().toLowerCase();
				}
				else {
					OneToOne oneToOne = inverse.findAnnotation(OneToOne.class);
					if (null != oneToOne) {
						fetch = oneToOne.fetch().name().toLowerCase();
					}
				}
				association.setFetch(fetch);
				String column;
				JoinColumn joinColumn = inverse.findAnnotation(JoinColumn.class);
				if (null != joinColumn && StringUtils.hasText(joinColumn.name())) {
					column = joinColumn.name();
				}
				else {
					column = inverse.getName() + "_" + targetEntity.getRequiredIdProperty().getName();
				}
				association.setColumn(column);
				Class<?> repositoryInterface = this.mappingContext.getRepositoryInterface(targetEntity.getType());
				if (null == repositoryInterface) {
					throw new MappingException(
							"Could not find namespace for entity: " + targetEntity.getType().getName());
				}
				String select = repositoryInterface.getName() + "." + ResidentStatementName.GET_BY_ID;
				association.setSelect(select);
				associations.add(association);
			}

		});

		params.put("statementName", ResidentStatementName.RESULT_MAP);
		params.put("entityType", this.persistentEntity.getType().getName());
		params.put("results", results);
		params.put("embeddedAssociations", embeddedAssociations);
		params.put("associations", associations);
		return render("ResultMap", params);

	}

	private String addInsertStatement(boolean selective) {
		if (this.checkStatement(selective ? ResidentStatementName.INSERT_SELECTIVE : ResidentStatementName.INSERT)) {
			return "";
		}

		MybatisPersistentProperty idProperty = this.persistentEntity.getIdProperty();
		String keyProperty = "";
		String keyColumn = "";
		boolean useGeneratedKeys = false;
		boolean excludeId = false;

		if (!this.persistentEntity.hasCompositeId() && null != idProperty) {
			keyProperty = idProperty.getName();
			keyColumn = idProperty.getColumn().getName().getText();
			if (idProperty.isAnnotationPresent(GeneratedValue.class)) {
				useGeneratedKeys = true;
				excludeId = true;
			}
		}

		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", selective ? ResidentStatementName.INSERT_SELECTIVE : ResidentStatementName.INSERT);
		scopes.put("properties", this.mappingPropertyToColumn().entrySet());
		scopes.put("table", this.getTableName());
		scopes.put("parameterType", this.persistentEntity.getType().getName());
		scopes.put("selective", selective);
		scopes.put("keyProperty", keyProperty);
		scopes.put("keyColumn", keyColumn);
		scopes.put("useGeneratedKeys", useGeneratedKeys);
		scopes.put("testNotNull", this.lambdaTestNotNull());
		scopes.put("excludeId", excludeId);

		if (useGeneratedKeys) {
			scopes.putAll(this.buildKeyGenerator(idProperty));
		}

		return this.render("Insert", scopes);

	}

	private Map<String, Object> buildKeyGenerator(MybatisPersistentProperty idProperty) {
		Map<String, Object> result = new HashMap<>();
		boolean executeBefore;
		String sql;
		GeneratedValue gv = idProperty.getRequiredAnnotation(GeneratedValue.class);
		if (gv.strategy() == GenerationType.IDENTITY || (gv.strategy() == GenerationType.AUTO
				&& "identity".equals(this.dialect.getNativeIdentifierGeneratorStrategy()))) {
			// identity
			sql = this.dialect.getIdentityColumnSupport().getIdentitySelectString(this.getTableName(),
					idProperty.getColumn().getName().getCanonicalName(),
					idProperty.getColumn().getJdbcType().TYPE_CODE);
			executeBefore = false;
		}
		else if (gv.strategy() == GenerationType.SEQUENCE || (gv.strategy() == GenerationType.AUTO
				&& "sequence".equals(this.dialect.getNativeIdentifierGeneratorStrategy()))) {
			String sequenceName = DEFAULT_SEQUENCE_NAME;
			if (StringUtils.hasText(gv.generator())) {
				// search sequence generator
				Map<String, String> sequenceGenerators = new HashMap<>();
				if (this.persistentEntity.isAnnotationPresent(SequenceGenerators.class)) {
					sequenceGenerators.putAll(Stream
							.of(this.persistentEntity.getRequiredAnnotation(SequenceGenerators.class).value())
							.filter(sg -> StringUtils.hasText(sg.sequenceName()))
							.collect(Collectors.toMap(SequenceGenerator::name, SequenceGenerator::sequenceName)));
				}
				if (this.persistentEntity.isAnnotationPresent(SequenceGenerator.class)) {
					SequenceGenerator sg = this.persistentEntity.getRequiredAnnotation(SequenceGenerator.class);
					if (StringUtils.hasText(sg.sequenceName())) {
						sequenceGenerators.put(sg.name(), sg.sequenceName());
					}
				}
				if (idProperty.isAnnotationPresent(SequenceGenerators.class)) {
					sequenceGenerators.putAll(Stream
							.of(idProperty.getRequiredAnnotation(SequenceGenerators.class).value())
							.filter((sg) -> StringUtils.hasText(sg.sequenceName()))
							.collect(Collectors.toMap(SequenceGenerator::name, SequenceGenerator::sequenceName)));
				}
				if (idProperty.isAnnotationPresent(SequenceGenerator.class)) {
					SequenceGenerator sg = idProperty.getRequiredAnnotation(SequenceGenerator.class);
					if (StringUtils.hasText(sg.sequenceName())) {
						sequenceGenerators.put(sg.name(), sg.sequenceName());
					}
				}
				String sn = sequenceGenerators.get(gv.generator());
				if (StringUtils.hasText(sn)) {
					sequenceName = sn;
				}
			}
			sql = this.dialect.getSequenceNextValString(sequenceName);
			executeBefore = true;
		}
		else {
			throw new UnsupportedOperationException("unsupported generated value id strategy: " + gv.strategy());
		}
		// return String.format(
		// "<selectKey keyProperty=\"%s\" keyColumn=\"%s\" order=\"%s\"
		// resultType=\"%s\">%s</selectKey>",
		// idProperty.getName(), idProperty.getColumn().getName().getText(), executeBefore
		// ? "BEFORE" : "AFTER",
		// idProperty.getType().getName(), sql);
		result.put("order", executeBefore ? "BEFORE" : "AFTER");
		result.put("keySql", sql);
		result.put("keyType", idProperty.getType().getName());
		return result;
	}

	private String addUpdateStatement(boolean selective, boolean byId) {
		String statement = (selective
				? (byId ? ResidentStatementName.UPDATE_SELECTIVE_BY_ID : ResidentStatementName.UPDATE_SELECTIVE)
				: (byId ? ResidentStatementName.UPDATE_BY_ID : ResidentStatementName.UPDATE));
		if (this.checkStatement(statement) || null == this.persistentEntity.getIdClass()) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", statement);
		scopes.put("selective", selective);
		scopes.put("properties", this.mappingPropertyToColumn().entrySet());
		scopes.put("table", this.getTableName());
		scopes.put("testNotNull", this.lambdaTestNotNull());
		scopes.put("byId", byId);

		return this.render("Update", scopes);
	}

	private String addDeleteByIdStatement() {
		if (this.checkStatement(ResidentStatementName.DELETE_BY_ID) || null == this.persistentEntity.getIdClass()) {
			return "";
		}

		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.DELETE_BY_ID);
		scopes.put("parameterType", this.persistentEntity.getIdClass().getName());
		scopes.put("table", this.getTableName());
		return render("DeleteById", scopes);
	}

	private String addDeleteByIdsStatement() {
		if (this.checkStatement(ResidentStatementName.DELETE_BY_IDS)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.DELETE_BY_IDS);
		scopes.put("table", this.getTableName());
		return render("DeleteByIds", scopes);
	}

	private String addDeleteAllStatement() {
		if (this.checkStatement(ResidentStatementName.DELETE_ALL)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.DELETE_ALL);
		scopes.put("table", this.getTableName());
		return this.render("DeleteAll", scopes);
	}

	private String addGetByIdStatement() {
		if (this.checkStatement(ResidentStatementName.GET_BY_ID) || null == this.persistentEntity.getIdClass()) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.GET_BY_ID);
		scopes.put("table", this.getTableName());
		scopes.put("parameterType", this.persistentEntity.getIdClass().getName());
		scopes.put("resultMap", ResidentStatementName.RESULT_MAP);
		return this.render("GetById", scopes);
	}

	private String addFindStatement(boolean pageable) {
		if (this.checkStatement(((pageable) ? ResidentStatementName.FIND_BY_PAGER : ResidentStatementName.FIND))) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", pageable ? ResidentStatementName.FIND_BY_PAGER : ResidentStatementName.FIND);
		scopes.put("resultMap", ResidentStatementName.RESULT_MAP);
		scopes.put("table", this.getTableName());
		scopes.put("pageable", pageable);
		if (pageable) {
			scopes.put("limitHandler", this.limitHandler());
			scopes.put("SQLServer2005", this.dialect.getLimitHandler().getClass() == SQLServer2005LimitHandler.class);
			scopes.put("SQLServer2012", this.dialect.getLimitHandler().getClass() == SQLServer2012LimitHandler.class);
		}
		scopes.put("conditionQuery", this.buildByConditionQueryCondition());
		return this.render("Find", scopes);
	}

	private String addCountAllStatement() {
		if (this.checkStatement(ResidentStatementName.COUNT_ALL)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.COUNT_ALL);
		scopes.put("table", this.getTableName());
		return this.render("CountAll", scopes);
	}

	private String addCountStatement() {
		if (this.checkStatement(ResidentStatementName.COUNT)) {
			return "";
		}

		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.COUNT);
		scopes.put("table", this.getTableName());
		scopes.put("conditionQuery", this.buildByConditionQueryCondition());

		return this.render("Count", scopes);
	}

	private String addQueryByExample() {
		if (this.checkStatement(ResidentStatementName.QUERY_BY_EXAMPLE)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.QUERY_BY_EXAMPLE);
		scopes.put("table", this.getTableName());
		scopes.put("resultMap", ResidentStatementName.RESULT_MAP);
		return this.render("QueryByExample", scopes);
	}

	private String addQueryByExampleForPage() {
		if (this.checkStatement(ResidentStatementName.QUERY_BY_EXAMPLE_FOR_PAGE)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.QUERY_BY_EXAMPLE_FOR_PAGE);
		scopes.put("table", this.getTableName());
		scopes.put("resultMap", ResidentStatementName.RESULT_MAP);
		scopes.put("limitHandler", this.limitHandler());
		scopes.put("SQLServer2005", this.dialect.getLimitHandler().getClass() == SQLServer2005LimitHandler.class);
		scopes.put("SQLServer2012", this.dialect.getLimitHandler().getClass() == SQLServer2012LimitHandler.class);
		return this.render("QueryByExampleForPage", scopes);
	}

	private String addCountQueryByExample() {
		if (this.checkStatement(ResidentStatementName.COUNT_QUERY_BY_EXAMPLE)) {
			return "";
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.COUNT_QUERY_BY_EXAMPLE);
		scopes.put("table", this.getTableName());
		return this.render("CountQueryByExample", scopes);
	}

	private String addFindByExampleStatement() {
		if (this.configuration.hasStatement(this.namespace + '.' + ResidentStatementName.FIND_BY_EXAMPLE, false)) {
			return "";
		}

		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", ResidentStatementName.FIND_BY_EXAMPLE);
		scopes.put("tableName", this.getTableName());
		scopes.put("entityType", this.persistentEntity.getType().getName());
		scopes.put("resultMap", ResidentStatementName.RESULT_MAP);

		return render("FindByExample", scopes);
	}

	private String addFindByExampleWhereClauseSQL() {
		if (this.configuration.getSqlFragments().containsKey(this.namespace + ".__example_where_clause")) {
			return "";
		}
		return render("FindByExampleWhereClause", null);
	}

	Mustache.Lambda limitHandler() {
		return (frag, out) -> out
				.write(this.dialect.getLimitHandler().processSql(frag.execute(), new RowSelection(true)));
	}

	Mustache.Lambda lambdaRegexLike() {
		return (frag, out) -> {
			String[] split = frag.execute().trim().split(";;;");
			out.write(this.dialect.getRegexLikeFunction(split[0].trim(), split[1].trim()));
		};
	}

	Mustache.Lambda lambdaReplaceDotToUnderline() {
		return (frag, out) -> out.write(frag.execute().trim().replace('.', '_'));
	}

	Mustache.InvertibleLambda lambdaTestNotNull() {
		return new InvertibleLambda() {
			@Override
			public void execute(Fragment frag, Writer out) throws IOException {
				out.write(this.testClause(frag.execute().trim(), true, true));
			}

			@Override
			public void executeInverse(Fragment frag, Writer out) throws IOException {
				out.write(this.testClause(frag.execute().trim(), false, false));
			}

			protected String testClause(String propertyName, boolean and, boolean not) {
				String[] parts = propertyName.split("\\.");
				String[] conditions = new String[parts.length];
				String prev = null;
				for (int i = 0; i < parts.length; i++) {
					conditions[i] = ((null != prev) ? (prev + ".") : "") + parts[i];
					prev = conditions[i];
				}
				String test = Stream.of(conditions).map(c -> c.trim() + (not ? " !" : " =") + "= null")
						.collect(Collectors.joining(and ? " and " : " or "));
				return test;
			}
		};
	}

	private ColumnResult columnResult(MybatisPersistentProperty p) {
		Column column = p.getColumn();
		ColumnResult cr = new ColumnResult();
		cr.setPrimaryKey(p.isIdProperty());
		cr.setColumn(column.getName().render(this.dialect));
		cr.setProperty(p.getName());
		cr.setJavaType(column.getJavaTypeString());
		cr.setJdbcType(column.getJdbcTypeString());
		cr.setTypeHandler(column.getTypeHandlerString());
		return cr;
	}

	@Deprecated
	private String buildByConditionQueryCondition() {

		String sql = this.findProperties().stream().map(pp -> {
			Set<Condition> set = new HashSet<>();
			Conditions conditions = pp.findAnnotation(Conditions.class);
			if (null != conditions && conditions.value().length > 0) {
				set.addAll(Arrays.asList(conditions.value()));
			}
			Condition condition = pp.findAnnotation(Condition.class);
			if (null != condition) {
				set.add(condition);
			}
			if (set.isEmpty()) {
				return "";
			}

			return set.stream().map(c -> {
				String[] properties = c.properties();
				if (properties.length == 0) {
					properties = new String[] { pp.getName() };
				}

				Part.Type type = Part.Type.valueOf(c.type().name());
				if (type.getNumberOfArguments() > 0 && type.getNumberOfArguments() != properties.length) {
					throw new MappingException("@Condition with type " + type + " needs " + type.getNumberOfArguments()
							+ " arguments, but only find " + properties.length + " properties in this @Condition.");
				}
				String cond = Stream.of(properties).map(property -> String.format("__condition.%s != null", property))
						.collect(Collectors.joining(" AND "));
				Part.IgnoreCaseType ignoreCaseType = Part.IgnoreCaseType.valueOf(c.ignoreCaseType().name());
				String columnName = StringUtils.hasText(c.column()) ? c.column()
						: pp.getColumn().getName().render(this.dialect);
				String left = this.buildQueryByConditionLeftSegment(columnName, ignoreCaseType, pp);
				String operator = this.buildQueryByConditionOperator(type);
				String right = this.buildQueryByConditionRightSegment(type, ignoreCaseType, properties);
				return String.format("<if test=\"%s\"> AND %s %s %s</if>", cond, left, operator, right);
			}).collect(Collectors.joining());
		}).collect(Collectors.joining());
		return String.format("<if test=\"__condition != null\"><trim prefixOverrides=\"AND |OR \">%s</trim></if>", sql);
	}

}
