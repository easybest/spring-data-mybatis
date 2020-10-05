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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Mustache.InvertibleLambda;
import com.samskivert.mustache.Mustache.Lambda;
import com.samskivert.mustache.Template.Fragment;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.model.Identifier;
import org.springframework.data.mybatis.mapping.model.Model;
import org.springframework.data.mybatis.repository.query.DefaultCollector;
import org.springframework.data.mybatis.repository.support.SqlSessionRepositorySupport;
import org.springframework.util.CollectionUtils;

/**
 * .
 *
 * @author JARVIS SONG
 */
@Slf4j
abstract class AbstractMybatisPrecompiler implements MybatisPrecompiler {

	protected static final String LINE_BREAKS = "\n";

	protected static final String SCOPE_STATEMENT_NAME = "statementName";

	protected static final String SCOPE_RESULT_MAP = "resultMap";

	protected final MybatisMappingContext mappingContext;

	protected final Model domain;

	protected final Mustache.Compiler mustache;

	protected AbstractMybatisPrecompiler(MybatisMappingContext mappingContext, Model domain) {
		this.mappingContext = mappingContext;
		this.domain = domain;
		this.mustache = Mustache.compiler().withLoader(name -> {
			String path = this.getTemplatePath(name);
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
			return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		}).escapeHTML(false).withCollector(new DefaultCollector());
	}

	@Override
	public void compile() {
		this.compileMapper(this.domain.getMappingEntity().getName(), this.prepareStatements());
	}

	protected abstract Collection<String> prepareStatements();

	protected String quote(String name) {
		return Identifier.toIdentifier(name, true).render(this.mappingContext.getDialect());
		// return this.mappingContext.getDialect().openQuote() + name +
		// this.mappingContext.getDialect().closeQuote();
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

	protected String render(String name, Map<String, Object> scopes) {
		if (null == scopes) {
			scopes = new HashMap<>();
		}
		scopes.put("domain", this.domain);
		scopes.put("quote", (Lambda) (frag, out) -> out.write(this.quote(frag.execute())));
		scopes.put("testNotNull", lambdaTestNotNull());
		scopes.put("escapeQuotes", (Lambda) (frag, out) -> out.write(frag.execute().replace("\"", "\\\"")));
		try (InputStream is = new ClassPathResource(this.getTemplatePath(name)).getInputStream();
				InputStreamReader source = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			return this.mustache.compile(source).execute(scopes);
		}
		catch (IOException ex) {
			throw new MappingException("Could not render the statement: " + name, ex);
		}
	}

	protected void compileMapper(String namespace, Collection<String> statements) {
		this.compileMapper("simple", namespace, statements);
	}

	protected void compileMapper(String dir, String namespace, Collection<String> statements) {
		if (CollectionUtils.isEmpty(statements)) {
			return;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(LINE_BREAKS);
		builder.append(
				"<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"https://mybatis.org/dtd/mybatis-3-mapper.dtd\">")
				.append(LINE_BREAKS);
		builder.append("<mapper namespace=\"").append(namespace).append("\">").append(LINE_BREAKS);
		statements.stream().forEach(s -> builder.append(s).append(LINE_BREAKS));
		builder.append("</mapper>");
		if (log.isDebugEnabled()) {
			log.debug("\n------------------------\n" + "Generated Mapper XML ("
					+ (dir + "/" + namespace.replace('.', '/') + this.getResourceSuffix())
					+ ")\n------------------------\n" + builder.toString());
		}
		Configuration configuration = this.mappingContext.getSqlSessionTemplate().getConfiguration();
		try (InputStream is = new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8))) {
			new XMLMapperBuilder(is, configuration, dir + "/" + namespace.replace('.', '/') + this.getResourceSuffix(),
					configuration.getSqlFragments()).parse();
		}
		catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			throw new MappingException(ex.getMessage(), ex);
		}
	}

	protected boolean checkSqlFragment(String name) {
		return this.mappingContext.getSqlSessionTemplate().getConfiguration().getSqlFragments()
				.containsKey(this.domain.getMappingEntity().getName() + SqlSessionRepositorySupport.DOT + name);
	}

	protected boolean checkResultMap(String name) {
		return this.mappingContext.getSqlSessionTemplate().getConfiguration()
				.hasResultMap(this.domain.getMappingEntity().getName() + SqlSessionRepositorySupport.DOT + name);
	}

	protected boolean checkStatement(String name) {
		return this.checkStatement(this.domain.getMappingEntity().getName(), name);
	}

	protected boolean checkStatement(String namespace, String name) {
		return this.mappingContext.getSqlSessionTemplate().getConfiguration()
				.hasStatement(namespace + SqlSessionRepositorySupport.DOT + name, false);
	}

	protected String getResourceSuffix() {
		return ".precompiler";
	}

	protected String getTemplatePath(String name) {
		String path = "org/springframework/data/mybatis/repository/query/template/" + name + ".mustache";
		return path;
	}

}
