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
package org.springframework.data.mybatis.querydsl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import org.springframework.data.mybatis.annotation.Snowflake;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class MybatisAnnotationProcessor extends AbstractProcessor {

	private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.FALSE;

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;

	private static final Collection<Class<? extends Annotation>> ID_ANNOTATIONS;

	private static final Collection<Class<? extends Annotation>> UPDATEABLE_ANNOTATIONS;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);
		annotations.add(ElementCollection.class);

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(Id.class);
		annotations.add(EmbeddedId.class);
		annotations.add(Snowflake.class);

		ID_ANNOTATIONS = Collections.unmodifiableSet(annotations);

		annotations = new HashSet<>();
		annotations.add(javax.persistence.Column.class);
		annotations.add(OrderColumn.class);

		UPDATEABLE_ANNOTATIONS = Collections.unmodifiableSet(annotations);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		if (roundEnv.processingOver() || annotations.size() == 0) {
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		if (roundEnv.getRootElements() == null || roundEnv.getRootElements().isEmpty()) {
			this.logInfo("No sources to process");
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Entity.class);
		if (null == elements || elements.isEmpty()) {
			this.logInfo("No sources to process");
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		Boolean mapUnderscoreToCamelCase = Boolean
				.valueOf(this.processingEnv.getOptions().getOrDefault("mapUnderscoreToCamelCase", "true"));

		Mustache.Compiler mustache = Mustache.compiler().escapeHTML(false);
		ClassLoader classLoader = MybatisAnnotationProcessor.class.getClassLoader();
		Filer filer = this.processingEnv.getFiler();

		for (Element element : elements) {
			this.logInfo("Processing Entity: " + element);

			try {
				Domain domain = new Domain(this.processingEnv, mapUnderscoreToCamelCase, (TypeElement) element);
				Map<String, Object> scopes = new HashMap<>();
				scopes.put("domain", domain);
				JavaFileObject javaFileObject = filer
						.createSourceFile(domain.getPackageName() + '.' + domain.getQueryClassName(), element);
				InputStream is = classLoader.getResourceAsStream("template/Q.mustache");
				try (InputStreamReader in = new InputStreamReader(is, StandardCharsets.UTF_8);
						Writer writer = javaFileObject.openWriter()) {
					Template template = mustache.compile(in);
					template.execute(scopes, writer);
				}
			}
			catch (Exception ex) {
				this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						ex.getMessage() + "\n"
								+ Stream.of(ex.getStackTrace()).map(stackTraceElement -> stackTraceElement.toString())
										.collect(Collectors.joining("\n")));
			}

		}

		return true;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton("javax.persistence.*");
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
	}

	private void logInfo(String message) {
		this.processingEnv.getMessager().printMessage(Kind.NOTE, message);
	}

}
