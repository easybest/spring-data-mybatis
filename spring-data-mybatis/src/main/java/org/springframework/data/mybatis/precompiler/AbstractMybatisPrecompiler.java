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
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2005LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2012LimitHandler;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.model.Identifier;
import org.springframework.data.mybatis.mapping.model.Model;
import org.springframework.data.mybatis.repository.query.DefaultCollector;
import org.springframework.data.mybatis.repository.support.SqlSessionRepositorySupport;
import org.springframework.util.CollectionUtils;

/**
 * Abstract mybatis mapper precompiler.
 *
 * @author JARVIS SONG
 */
@Slf4j
abstract class AbstractMybatisPrecompiler implements MybatisPrecompiler {

	protected static final String SCOPE_STATEMENT_NAME = "statementName";

	protected static final String SCOPE_RESULT_MAP = "resultMap";

	protected final MybatisMappingContext mappingContext;

	protected final Model domain;

	protected final Mustache.Compiler mustache;

	protected AbstractMybatisPrecompiler(MybatisMappingContext mappingContext, Model domain) {
		this.mappingContext = mappingContext;
		this.domain = domain;
		this.mustache = Mustache.compiler().withLoader(name -> {
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(this.getTemplatePath(name));
			return new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		}).escapeHTML(false).withCollector(new DefaultCollector());
	}

	@Override
	public void compile() {
		this.compileMapper(this.domain.getMappingEntity().getName(), this.prepareStatements());
	}

	protected abstract Collection<String> prepareStatements();

	protected String render(String name, Map<String, Object> scopes) {
		if (null == scopes) {
			scopes = new HashMap<>();
		}
		scopes.put("domain", this.domain);
		scopes.put("quote", (Lambda) (frag, out) -> out.write(this.quote(frag.execute())));
		scopes.put("testNotNull", this.lambdaTestNotNull());
		scopes.put("escapeQuotes", (Lambda) (frag, out) -> out.write(frag.execute().replace("\"", "\\\"")));
		scopes.put("lowercaseFunction", this.mappingContext.getDialect().getLowercaseFunction());
		scopes.put("SQLServer2005",
				this.mappingContext.getDialect().getLimitHandler().getClass() == SQLServer2005LimitHandler.class);
		scopes.put("SQLServer2012",
				this.mappingContext.getDialect().getLimitHandler().getClass() == SQLServer2012LimitHandler.class);
		try (InputStream is = new ClassPathResource(this.getTemplatePath(name)).getInputStream();
				InputStreamReader source = new InputStreamReader(is, StandardCharsets.UTF_8)) {
			return this.mustache.compile(source).execute(scopes);
		}
		catch (IOException ex) {
			throw new MappingException("Could not render the statement: " + name, ex);
		}
	}

	protected void compileMapper(String namespace, Collection<String> statements) {
		this.compileMapper(this.getResource("basic", namespace), namespace, statements);
	}

	protected void compileMapper(String resource, String namespace, Collection<String> statements) {
		if (CollectionUtils.isEmpty(statements)) {
			return;
		}
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("namespace", namespace);
		scopes.put("statements", statements);
		scopes.put("resource", resource + this.getResourceSuffix());
		String mapper = this.render("Mapper", scopes);

		if (log.isDebugEnabled()) {
			log.debug(mapper);
		}
		Configuration configuration = this.mappingContext.getSqlSessionTemplate().getConfiguration();
		try (InputStream is = new ByteArrayInputStream(mapper.getBytes(StandardCharsets.UTF_8))) {
			XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(is, configuration,
					resource + this.getResourceSuffix(), configuration.getSqlFragments());
			xmlMapperBuilder.parse();
		}
		catch (Exception ex) {
			throw new MappingException(ex.getMessage(), ex);
		}
		finally {
			ErrorContext.instance().reset();
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

	protected String getResource(String dir, String namespace) {
		return dir + '/' + namespace.replace('.', '/');
	}

	protected String getResourceSuffix() {
		return ".xml";
	}

	protected String getTemplatePath(String name) {
		return "org/springframework/data/mybatis/repository/query/template/" + name + ".mustache";
	}

	protected String quote(String name) {
		return Identifier.toIdentifier(name, true).render(this.mappingContext.getDialect());
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

}
