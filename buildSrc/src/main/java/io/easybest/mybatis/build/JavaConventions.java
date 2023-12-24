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
 *
 */

package io.easybest.mybatis.build;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.spring.javaformat.gradle.FormatTask;
import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;

import io.easybest.mybatis.build.toolchain.ToolchainPlugin;

/**
 * @author Jarvis Song
 */
class JavaConventions {

	private static final String SOURCE_AND_TARGET_COMPATIBILITY = "17";

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, java -> {
			this.configureSpringJavaFormat(project);
			this.configureJavaCompileConventions(project);
			this.configureJavadocConventions(project);
			this.configureJarManifestConventions(project);
			this.configureToolchain(project);
		});
	}

	private void configureJarManifestConventions(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
		Set<String> sourceJarTaskNames = sourceSets.stream().map(SourceSet::getSourcesJarTaskName)
				.collect(Collectors.toSet());
		Set<String> javadocJarTaskNames = sourceSets.stream().map(SourceSet::getJavadocJarTaskName)
				.collect(Collectors.toSet());

		project.getTasks().withType(Jar.class, jar -> project.afterEvaluate(evaluated -> {
			jar.manifest(manifest -> {
				Map<String, Object> attributes = new TreeMap<>();
				attributes.put("Automatic-Module-Name", project.getName().replace("-", "."));
				attributes.put("Build-Jdk-Spec", project.property("sourceCompatibility"));
				attributes.put("Built-By", "EASYBEST.IO");
				attributes.put("Implementation-Title",
						this.determineImplementationTitle(project, sourceJarTaskNames, javadocJarTaskNames, jar));
				attributes.put("Implementation-Version", project.getVersion());
				manifest.attributes(attributes);
			});
		}));
	}

	private String determineImplementationTitle(Project project, Set<String> sourceJarTaskNames,
			Set<String> javadocJarTaskNames, Jar jar) {
		if (sourceJarTaskNames.contains(jar.getName())) {
			return "Source for " + project.getName();
		}
		if (javadocJarTaskNames.contains(jar.getName())) {
			return "Javadoc for " + project.getName();
		}
		return project.getDescription();
	}

	private boolean isCi() {
		return Boolean.parseBoolean(System.getenv("CI"));
	}

	private void configureJavadocConventions(Project project) {
		project.getTasks().withType(Javadoc.class,
				javadoc -> javadoc.getOptions().source(SOURCE_AND_TARGET_COMPATIBILITY).encoding("UTF-8"));
	}

	private void configureJavaCompileConventions(Project project) {
		project.getTasks().withType(JavaCompile.class, compile -> {
			compile.getOptions().setEncoding("UTF-8");
			compile.setSourceCompatibility(SOURCE_AND_TARGET_COMPATIBILITY);
			compile.setTargetCompatibility(SOURCE_AND_TARGET_COMPATIBILITY);
			List<String> args = compile.getOptions().getCompilerArgs();
			if (!args.contains("-parameters")) {
				args.add("-parameters");
			}
			if (this.buildingWithJava8(project)) {
				args.addAll(Arrays.asList("-Werror", "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:rawtypes",
						"-Xlint:varargs"));
			}
		});
	}

	private boolean buildingWithJava8(Project project) {
		return !project.hasProperty("toolchainVersion") && JavaVersion.current() == JavaVersion.VERSION_17;
	}

	private void configureSpringJavaFormat(Project project) {
		project.getPlugins().apply(SpringJavaFormatPlugin.class);
		project.getTasks().withType(FormatTask.class, formatTask -> formatTask.setEncoding("UTF-8"));
		project.getPlugins().apply(CheckstylePlugin.class);
		CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
		checkstyle.setToolVersion("8.43");
		checkstyle.getConfigDirectory().set(project.getRootProject().file("src/checkstyle"));
		String version = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
		DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
		checkstyleDependencies
				.add(project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + version));
	}

	private void configureToolchain(Project project) {
		project.getPlugins().apply(ToolchainPlugin.class);
	}

}
