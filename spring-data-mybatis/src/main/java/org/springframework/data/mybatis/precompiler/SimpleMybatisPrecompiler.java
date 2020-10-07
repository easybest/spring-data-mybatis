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
package org.springframework.data.mybatis.precompiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.samskivert.mustache.Mustache.Lambda;

import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.model.ManyToManyAssociation;
import org.springframework.data.mybatis.mapping.model.ManyToOneAssociation;
import org.springframework.data.mybatis.mapping.model.Model;
import org.springframework.data.mybatis.mapping.model.OneToManyAssociation;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple mybatis mapper precompiler.
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class SimpleMybatisPrecompiler extends AbstractMybatisPrecompiler {

	public SimpleMybatisPrecompiler(MybatisMappingContext mappingContext, Model domain) {
		super(mappingContext, domain);
	}

	@Override
	protected Collection<String> prepareStatements() {
		List<String> statements = new ArrayList<>();
		statements.add(this.columnListFragment());
		statements.add(this.fromFragment());
		statements.add(this.selectFragment());
		statements.add(this.whereClauseByIdFragment());
		statements.add(this.whereClauseByIdsFragment());
		statements.add(this.standardSortFragment());
		statements.add(this.queryByExampleWhereClauseFragment());

		statements.add(this.resultMap());

		statements.add(this.insertStatement());
		statements.add(this.insertSelectiveStatement());
		statements.add(this.updateStatement());
		statements.add(this.updateByIdStatement());
		statements.add(this.updateSelectiveStatement());
		statements.add(this.updateSelectiveByIdStatement());
		statements.add(this.deleteByIdStatement());
		statements.add(this.deleteByIdsStatement());
		statements.add(this.deleteAllStatement());
		statements.add(this.getByIdStatement());
		statements.add(this.findStatement());
		statements.add(this.findByPageStatement());
		statements.add(this.countAllStatement());
		statements.add(this.countStatement());
		statements.add(this.queryByExampleStatement());
		statements.add(this.queryByExampleForPageStatement());
		statements.add(this.countByExampleStatement());

		this.resultMapOneToManySelectStatement();
		this.resultMapManyToManySelectStatement();
		this.associativeTableStatement();

		// Basic
		// statements.add(this.basicColumnListFragment());
		// statements.add(this.basicResultMap());

		return statements.stream().filter(StringUtils::hasText).collect(Collectors.toList());
	}

	private void resultMapOneToManySelectStatement() {
		if (CollectionUtils.isEmpty(this.domain.getOneToManyAssociations())) {
			return;
		}
		this.domain.getOneToManyAssociations().forEach(ass -> {
			String statementName = "__association_" + this.domain.getTable() + '_'
					+ ((OneToManyAssociation) ass).getProperty().getName();
			if (this.checkStatement(ass.getMappingEntity().getName(), statementName)) {
				return;
			}
			Map<String, Object> scopes = new HashMap<>();
			scopes.put(SCOPE_STATEMENT_NAME, statementName);
			scopes.put(SCOPE_RESULT_MAP, ResidentStatementName.RESULT_MAP);
			scopes.put("ass", ass);
			String ns = ass.getMappingEntity().getName();
			this.compileMapper("select/" + ns.replace('.', '/') + "/" + statementName, ns,
					Collections.singletonList(this.render("ResultMapOneToManySelect", scopes)));
		});
	}

	private void resultMapManyToManySelectStatement() {
		if (CollectionUtils.isEmpty(this.domain.getManyToManyAssociations())) {
			return;
		}
		this.domain.getManyToManyAssociations().forEach(ass -> {
			String statementName = "__association_" + this.domain.getTable() + '_'
					+ ((ManyToManyAssociation) ass).getProperty().getName();
			String ns = ass.getMappingEntity().getName();
			if (this.checkStatement(ns, statementName)) {
				return;
			}
			Map<String, Object> scopes = new HashMap<>();
			scopes.put(SCOPE_STATEMENT_NAME, statementName);
			scopes.put(SCOPE_RESULT_MAP, ResidentStatementName.RESULT_MAP);
			scopes.put("ass", ass);
			this.compileMapper("select/" + ns.replace('.', '/') + "/" + statementName, ns,
					Collections.singletonList(this.render("ResultMapManyToManySelect", scopes)));
		});
	}

	private void associativeTableStatement() {
		if (CollectionUtils.isEmpty(this.domain.getManyToManyAssociations())) {
			return;
		}
		this.domain.getManyToManyAssociations().forEach(ass -> {
			String namespace = "associative." + ((ManyToManyAssociation) ass).getJoinTable().getTable().toString();
			if (this.checkStatement(namespace, ResidentStatementName.INSERT)) {
				return;
			}
			Map<String, Object> scopes = new HashMap<>();
			scopes.put("ass", ass);

			scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.INSERT);
			this.compileMapper("associative/" + namespace.replace('.', '/') + "/insert", namespace,
					Collections.singletonList(this.render("AssociativeInsert", scopes)));
			scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.UPDATE);
			this.compileMapper("associative/" + namespace.replace('.', '/') + "/update", namespace,
					Collections.singletonList(this.render("AssociativeUpdate", scopes)));
			scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.DELETE);
			this.compileMapper("associative/" + namespace.replace('.', '/') + "/delete", namespace,
					Collections.singletonList(this.render("AssociativeDelete", scopes)));
		});

	}

	private String basicColumnListFragment() {
		if (this.checkSqlFragment(ResidentStatementName.BASIC_COLUMN_LIST)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.BASIC_COLUMN_LIST);
		return this.render("BasicColumnList", scopes);
	}

	private String allColumnListFragment() {
		if (this.checkSqlFragment(ResidentStatementName.ALL_COLUMN_LIST)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.ALL_COLUMN_LIST);
		return this.render("AllColumnList", scopes);
	}

	private String columnListFragment() {
		if (this.checkSqlFragment(ResidentStatementName.COLUMN_LIST)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.COLUMN_LIST);
		return this.render("ColumnList", scopes);
	}

	private String fromFragment() {
		if (this.checkSqlFragment(ResidentStatementName.FROM)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.FROM);
		return this.render("From", scopes);
	}

	private String selectFragment() {
		if (this.checkSqlFragment(ResidentStatementName.SELECT)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.SELECT);
		return this.render("Select", scopes);
	}

	private String whereClauseByIdFragment() {
		if (this.checkSqlFragment(ResidentStatementName.WHERE_BY_ID_CLAUSE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.WHERE_BY_ID_CLAUSE);
		return this.render("WhereClauseById", scopes);
	}

	private String whereClauseByIdsFragment() {
		if (this.checkSqlFragment(ResidentStatementName.WHERE_BY_IDS_CLAUSE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.WHERE_BY_IDS_CLAUSE);
		return this.render("WhereClauseByIds", scopes);
	}

	private String standardSortFragment() {
		if (this.checkSqlFragment(ResidentStatementName.STANDARD_SORT)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.STANDARD_SORT);
		return this.render("StandardSort", scopes);
	}

	private String queryByExampleWhereClauseFragment() {
		if (this.checkSqlFragment(ResidentStatementName.QUERY_BY_EXAMPLE_WHERE_CLAUSE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.QUERY_BY_EXAMPLE_WHERE_CLAUSE);
		scopes.put("replaceDotToUnderline", (Lambda) (frag, out) -> out.write(frag.execute().trim().replace('.', '_')));

		List columns = new LinkedList<>();
		if (null != this.domain.getPrimaryKey()) {
			columns.addAll(this.domain.getPrimaryKey().getColumns());
		}
		if (!CollectionUtils.isEmpty(this.domain.getNormalColumns())) {
			columns.addAll(this.domain.getNormalColumns());
		}
		if (!CollectionUtils.isEmpty(this.domain.getVersionColumns())) {
			columns.addAll(this.domain.getVersionColumns());
		}
		if (!CollectionUtils.isEmpty(this.domain.getEmbeddings())) {
			this.domain.getEmbeddings().stream().forEach(embedding -> {
				if (null != embedding.getPrimaryKey()) {
					columns.addAll(embedding.getPrimaryKey().getColumns());
				}
				if (!CollectionUtils.isEmpty(embedding.getNormalColumns())) {
					columns.addAll(embedding.getNormalColumns());
				}
				if (!CollectionUtils.isEmpty(embedding.getVersionColumns())) {
					columns.addAll(embedding.getVersionColumns());
				}
			});
		}
		if (!CollectionUtils.isEmpty(this.domain.getManyToOneAssociations())) {
			this.domain.getManyToOneAssociations().stream().forEach(association -> {
				ManyToOneAssociation ass = (ManyToOneAssociation) association;
				if (!ass.isLeftJoin()) {
					return;
				}

				if (null != ass.getPrimaryKey()) {
					columns.addAll(ass.getPrimaryKey().getColumns());
				}
				if (!CollectionUtils.isEmpty(ass.getNormalColumns())) {
					columns.addAll(ass.getNormalColumns());
				}
				if (!CollectionUtils.isEmpty(ass.getVersionColumns())) {
					columns.addAll(ass.getVersionColumns());
				}

			});
		}

		scopes.put("combinedColumns", columns);

		return this.render("QueryByExampleWhereClause", scopes);
	}

	private String basicResultMap() {
		if (this.checkResultMap(ResidentStatementName.BASIC_RESULT_MAP)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.BASIC_RESULT_MAP);
		return this.render("BasicResultMap", scopes);
	}

	private String resultMap() {
		if (this.checkResultMap(ResidentStatementName.RESULT_MAP)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.RESULT_MAP);
		return this.render("ResultMap", scopes);
	}

	private String insertStatement() {
		if (this.checkStatement(ResidentStatementName.INSERT)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.INSERT);
		return this.render("Insert", scopes);
	}

	private String insertSelectiveStatement() {
		if (this.checkStatement(ResidentStatementName.INSERT_SELECTIVE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.INSERT_SELECTIVE);
		return this.render("InsertSelective", scopes);
	}

	private String updateStatement() {
		if (this.checkStatement(ResidentStatementName.UPDATE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.UPDATE);
		scopes.put("byId", false);
		return this.render("Update", scopes);
	}

	private String updateByIdStatement() {
		if (this.checkStatement(ResidentStatementName.UPDATE_BY_ID)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.UPDATE_BY_ID);
		scopes.put("byId", true);
		return this.render("Update", scopes);
	}

	private String updateSelectiveStatement() {
		if (this.checkStatement(ResidentStatementName.UPDATE_SELECTIVE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.UPDATE_SELECTIVE);
		scopes.put("byId", false);
		return this.render("UpdateSelective", scopes);
	}

	private String updateSelectiveByIdStatement() {
		if (this.checkStatement(ResidentStatementName.UPDATE_SELECTIVE_BY_ID)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.UPDATE_SELECTIVE_BY_ID);
		scopes.put("byId", true);
		return this.render("UpdateSelective", scopes);
	}

	private String deleteByIdStatement() {
		if (this.checkStatement(ResidentStatementName.DELETE_BY_ID)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.DELETE_BY_ID);
		return this.render("DeleteById", scopes);
	}

	private String deleteByIdsStatement() {
		if (this.checkStatement(ResidentStatementName.DELETE_BY_IDS)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.DELETE_BY_IDS);
		return this.render("DeleteByIds", scopes);
	}

	private String deleteAllStatement() {
		if (this.checkStatement(ResidentStatementName.DELETE_ALL)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.DELETE_ALL);
		// FIXME: change to use truncate?
		return this.render("DeleteAll", scopes);
	}

	private String getByIdStatement() {
		if (this.checkStatement(ResidentStatementName.GET_BY_ID)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.GET_BY_ID);
		scopes.put(SCOPE_RESULT_MAP, ResidentStatementName.RESULT_MAP);
		return this.render("GetById", scopes);
	}

	private String findStatement() {
		if (this.checkStatement(ResidentStatementName.FIND)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.FIND);
		scopes.put(SCOPE_RESULT_MAP, ResidentStatementName.RESULT_MAP);
		scopes.put("conditionQuery", ""); // TODO
		return this.render("Find", scopes);
	}

	private String findByPageStatement() {
		if (this.checkStatement(ResidentStatementName.FIND_BY_PAGER)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.FIND_BY_PAGER);
		scopes.put(SCOPE_RESULT_MAP, ResidentStatementName.RESULT_MAP);
		scopes.put("limitHandler", (Lambda) (frag, out) -> out.write(
				this.mappingContext.getDialect().getLimitHandler().processSql(frag.execute(), new RowSelection(true))));
		scopes.put("conditionQuery", ""); // TODO
		return this.render("FindByPage", scopes);
	}

	private String countAllStatement() {
		if (this.checkStatement(ResidentStatementName.COUNT_ALL)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.COUNT_ALL);
		return this.render("CountAll", scopes);
	}

	private String countStatement() {
		if (this.checkStatement(ResidentStatementName.COUNT)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.COUNT);
		return this.render("Count", scopes);
	}

	private String queryByExampleStatement() {
		if (this.checkStatement(ResidentStatementName.QUERY_BY_EXAMPLE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.QUERY_BY_EXAMPLE);
		scopes.put(SCOPE_RESULT_MAP, ResidentStatementName.RESULT_MAP);
		return this.render("QueryByExample", scopes);
	}

	private String queryByExampleForPageStatement() {
		if (this.checkStatement(ResidentStatementName.QUERY_BY_EXAMPLE_FOR_PAGE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.QUERY_BY_EXAMPLE_FOR_PAGE);
		scopes.put(SCOPE_RESULT_MAP, ResidentStatementName.RESULT_MAP);
		scopes.put("limitHandler", (Lambda) (frag, out) -> out.write(
				this.mappingContext.getDialect().getLimitHandler().processSql(frag.execute(), new RowSelection(true))));
		return this.render("QueryByExampleForPage", scopes);
	}

	private String countByExampleStatement() {
		if (this.checkStatement(ResidentStatementName.COUNT_QUERY_BY_EXAMPLE)) {
			return null;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put(SCOPE_STATEMENT_NAME, ResidentStatementName.COUNT_QUERY_BY_EXAMPLE);
		return this.render("CountQueryByExample", scopes);
	}

}
