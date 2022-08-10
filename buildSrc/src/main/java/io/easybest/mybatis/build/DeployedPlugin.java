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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.bundling.Jar;

/**
 * A plugin applied to a project that should be deployed.
 *
 * @author Jarvis Song
 */
public class DeployedPlugin implements Plugin<Project> {

	/**
	 * Name of the task that generates the deployed pom file.
	 */
	public static final String GENERATE_POM_TASK_NAME = "generatePomFileForMavenPublication";

	@Override
	public void apply(Project project) {
		project.getPlugins().apply(MavenPublishPlugin.class);
		project.getPlugins().apply(MavenRepositoryPlugin.class);
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		MavenPublication mavenPublication = publishing.getPublications().create("maven", MavenPublication.class);
		project.afterEvaluate(evaluated -> {
			project.getPlugins().withType(JavaPlugin.class).all(javaPlugin -> {
				if (((Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME)).isEnabled()) {
					project.getComponents().matching(component -> component.getName().equals("java"))
							.all(mavenPublication::from);
				}
			});
		});
		project.getPlugins().withType(JavaPlatformPlugin.class).all(javaPlugin -> project.getComponents()
				.matching(component -> component.getName().equals("javaPlatform")).all(mavenPublication::from));
	}

}
