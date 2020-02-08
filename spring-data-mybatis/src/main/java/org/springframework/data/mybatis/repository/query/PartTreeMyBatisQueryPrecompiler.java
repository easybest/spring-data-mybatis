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

import java.util.stream.Collectors;

import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Part tree query precompiler.
 *
 * @author JARVIS SONG
 */
class PartTreeMyBatisQueryPrecompiler extends AbstractMybatisPrecompiler {

	private final PartTreeMybatisQuery query;

	PartTreeMyBatisQueryPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			PartTreeMybatisQuery query) {
		super(mappingContext, configuration, query.getQueryMethod().getNamespace(),
				query.getQueryMethod().getEntityInformation().getJavaType());

		this.query = query;
	}

	@Override
	protected String doPrecompile() {
		PartTree tree = this.query.getTree();
		MybatisQueryMethod method = this.query.getQueryMethod();

		if (tree.isDelete()) {
			return this.addDeleteStatement(tree, method);
		}

		return null;
	}

	private String addDeleteStatement(PartTree tree, MybatisQueryMethod method) {

		ResultProcessor processor = method.getResultProcessor();
		ParameterMetadataProvider provider = new ParameterMetadataProvider(null, method.getParameters(), escape);
		ReturnedType returnedType = processor.getReturnedType();

		String where = new TreeQueryCreator(tree, returnedType, provider).createQuery();

		return String.format("<delete id=\"%s\">delete from %s <where>%s</where></delete>", //
				method.getStatementName(), this.getTableName(), where);
	}

	private String buildTreeOrConditionSegment(PartTree tree, MybatisQueryMethod method) {
		return tree.stream().map(node -> String.format("(%s)", this.buildTreeAndConditionSegment(node)))
				.collect(Collectors.joining(" or "));
	}

	private String buildTreeAndConditionSegment(PartTree.OrPart parts) {
		return parts.stream().map(part -> this.buildTreePredicateSegment(part)).collect(Collectors.joining(" and "));
	}

	private String buildTreePredicateSegment(Part part) {

		PropertyPath property = part.getProperty();
		Part.Type type = part.getType();

		return null;
	}

}
