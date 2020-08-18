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
package org.springframework.data.mybatis.processor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

import org.springframework.data.annotation.Transient;
import org.springframework.data.mybatis.annotation.Example;
import org.springframework.data.mybatis.annotation.Snowflake;

/**
 * Mybatis Domains processor.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class MybatisDomainProcessor extends AbstractProcessor {

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

	private final DomainTypeVisitor domainTypeVisitor = new DomainTypeVisitor();

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return Collections.singleton(Example.class.getCanonicalName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Example.class);
		if (null == elements || elements.isEmpty()) {
			return true;
		}
		Mustache.Compiler mustache = Mustache.compiler().escapeHTML(false);
		ClassLoader classLoader = MybatisDomainProcessor.class.getClassLoader();
		Filer filer = this.processingEnv.getFiler();
		for (Element element : elements) {
			this.processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Processing @Example " + element);
			try {
				TableMeta tableMeta = this.loadTableMeta(element);
				JavaFileObject javaFileObject = filer.createSourceFile(tableMeta.getExampleClassName());
				HashMap<String, Object> scopes = new HashMap<>();
				scopes.put("metadata", tableMeta);

				InputStream exampleInputStream = classLoader.getResourceAsStream("template/Example.java.tpl");
				try (InputStreamReader in = new InputStreamReader(exampleInputStream, StandardCharsets.UTF_8);
						Writer writer = javaFileObject.openWriter()) {
					Template template = mustache.compile(in);
					template.execute(scopes, writer);
				}
			}
			catch (Exception ex) {
				this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
						Arrays.toString(ex.getStackTrace()));

			}
		}

		return true;
	}

	private TableMeta loadTableMeta(Element element) {
		TableMeta meta = new TableMeta();
		meta.setDomainClassName(element.toString());
		meta.setExampleClassName(meta.getDomainClassName() + "Example");

		PackageElement packageOf = this.processingEnv.getElementUtils().getPackageOf(element);
		meta.setPackageName(packageOf.toString());

		for (Element member : element.getEnclosedElements()) {
			if (member.getModifiers().contains(Modifier.STATIC) || !member.getKind().isField()
					|| member.getAnnotation(javax.persistence.Transient.class) != null
					|| member.getAnnotation(Transient.class) != null
					|| ASSOCIATION_ANNOTATIONS.stream().anyMatch(ann -> member.getAnnotation(ann) != null)) {
				continue;
			}

			ColumnMeta column = new ColumnMeta();
			column.setPropertyName(member.toString());
			member.asType().accept(this.domainTypeVisitor, column);

			Column columnAnn = member.getAnnotation(Column.class);
			if (null != columnAnn && null != columnAnn.name() && columnAnn.name().trim().length() > 0) {
				column.setName(columnAnn.name());
			}
			if (null == column.getName()) {
				OrderColumn orderColumnAnn = member.getAnnotation(OrderColumn.class);
				if (null != orderColumnAnn && null != orderColumnAnn.name()
						&& orderColumnAnn.name().trim().length() > 0) {
					column.setName(orderColumnAnn.name());
				}
			}
			if (null == column.getName()) {
				column.setName(column.getPropertyName());
			}

			if (DomainTypeVisitor.PACKING.values().stream().anyMatch(t -> t.equals(column.getType()))) {
				meta.addColumn(column);
			}

		}

		return meta;
	}

}
