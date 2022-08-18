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

package io.easybest.mybatis.mapping.precompile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;

import org.springframework.data.mapping.MappingException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

/**
 * .
 *
 * @author Jarvis Song
 */
@Slf4j
public class MybatisMapperBuilder {

	private final Configuration configuration;

	private final String namespace;

	private final List<Segment> segments = new ArrayList<>();

	// private final MapperBuilderAssistant assistant;

	public MybatisMapperBuilder(Configuration configuration, String namespace) {

		this.configuration = configuration;
		this.namespace = namespace;
		// this.assistant = new MapperBuilderAssistant(configuration,
		// this.namespace.replace('.', '/') + ".xml(" + UUID.randomUUID() + ")");
		// this.assistant.setCurrentNamespace(this.namespace);
	}

	public MybatisMapperBuilder add(SqlDefinition segment) {

		if (null == segment) {
			return this;
		}
		String id = this.namespace + '.' + segment.getId();
		if (segment instanceof Fragment && this.configuration.getSqlFragments().containsKey(id)) {
			return this;
		}
		if (segment instanceof ResultMap && this.configuration.hasResultMap(id)) {
			return this;
		}
		if ((segment instanceof Select || segment instanceof Insert || segment instanceof Update
				|| segment instanceof Delete) && this.configuration.hasStatement(id, true)) {
			return this;
		}

		this.segments.add(segment);
		return this;
	}

	public void build() {

		if (CollectionUtils.isEmpty(this.segments)) {
			return;
		}
		String resource = this.namespace.replace('.', '/') + ".xml(" + UUID.randomUUID() + ")";
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<!-- Generated Mapper XML (" + resource
				+ ") -->"
				+ "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"https://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
				+ "<mapper namespace=\"" + this.namespace + "\">"
				+ (this.segments.stream().map(Segment::toString).collect(Collectors.joining())) + "</mapper>";

		if (log.isDebugEnabled()) {
			log.debug(this.format(content));
		}

		try (InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
			XMLMapperBuilder builder = new XMLMapperBuilder(is, this.configuration, resource,
					this.configuration.getSqlFragments());
			builder.parse();
		}
		catch (Exception ex) {
			log.error(content);
			throw new MappingException(ex.getMessage(), ex);
		}
		finally {
			ErrorContext.instance().reset();
		}
	}

	private String format(String xml) {

		StopWatch sw = new StopWatch();
		sw.start();
		try {
			final InputSource src = new InputSource(new StringReader(xml));
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			builder.setEntityResolver((publicId, systemId) -> {
				InputStream is = this.getClass().getResourceAsStream("/META-INF/mybatis-3-mapper.dtd");
				return new InputSource(is);
			});
			final Node document = builder.parse(src).getDocumentElement();
			// final Boolean keepDeclaration = xml.startsWith("<?xml");
			final Boolean keepDeclaration = false;

			final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
			final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
			final LSSerializer writer = impl.createLSSerializer();

			writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
			writer.getDomConfig().setParameter("xml-declaration", keepDeclaration);

			return writer.writeToString(document);
		}
		catch (Exception ex) {
			System.out.println(xml);
			throw new MappingException(ex.getMessage(), ex);
		}
		finally {
			sw.stop();
			System.out.println(sw.getTotalTimeMillis() + "ms cost by format!");
		}

	}

	public static MybatisMapperBuilder create(Configuration configuration, String namespace) {
		return new MybatisMapperBuilder(configuration, namespace);
	}

}
