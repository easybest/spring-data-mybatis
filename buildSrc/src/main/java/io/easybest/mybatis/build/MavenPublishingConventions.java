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

import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.attributes.Usage;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.ConfigurationVariantDetails;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.VariantVersionMappingStrategy;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloperSpec;
import org.gradle.api.publish.maven.MavenPomIssueManagement;
import org.gradle.api.publish.maven.MavenPomLicenseSpec;
import org.gradle.api.publish.maven.MavenPomOrganization;
import org.gradle.api.publish.maven.MavenPomScm;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * Conventions that are applied in the presence of the {@link MavenPublishPlugin}. When
 * the plugin is applied:
 * <ul>
 * <li>If the {@code deploymentRepository} property has been set, a
 * {@link MavenArtifactRepository Maven artifact repository} is configured to publish to
 * it.</li>
 * <li>The poms of all {@link MavenPublication Maven publication} are customized to meet
 * Maven Central's requirements.</li>
 * <li>If the {@link JavaPlugin Java Plugin} has also been applied:
 * <ul>
 * <li>Creation of Javadoc and source jars is enabled.</li>
 * <li>Publication metadata (poms and Gradle module metadata) is configured to use
 * resolved versions.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author Jarvis Song
 */
class MavenPublishingConventions {

	void apply(Project project) {

		project.getPlugins().withType(MavenPublishPlugin.class).all(mavenPublish -> {
			PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
			if (project.hasProperty("deploymentRepository")) {
				publishing.getRepositories().maven(mavenRepository -> {
					mavenRepository.setUrl(project.property("deploymentRepository"));
					mavenRepository.setName("deployment");
				});
			}
			else {
				// default repository
				publishing.getRepositories().maven(mavenRepository -> {
					mavenRepository.setUrl(project.getVersion().toString().endsWith("SNAPSHOT")
							? System.getenv("MAVEN_SNAPSHOTS_URL") : System.getenv("MAVEN_RELEASE_URL"));
					mavenRepository.setName("central");
					mavenRepository.getCredentials().setUsername(System.getenv("MAVEN_REPO_USER"));
					mavenRepository.getCredentials().setPassword(System.getenv("MAVEN_REPO_PASS"));
				});
			}
			publishing.getPublications().withType(MavenPublication.class)
					.all(mavenPublication -> this.customizeMavenPublication(mavenPublication, project));
			project.getPlugins().withType(JavaPlugin.class).all(javaPlugin -> {
				JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
				extension.withJavadocJar();
				extension.withSourcesJar();
			});
		});
	}

	private void customizeMavenPublication(MavenPublication publication, Project project) {

		publication.setArtifactId(this.determineProjectName(project));
		this.customizePom(publication.getPom(), project);
		project.getPlugins().withType(JavaPlugin.class)
				.all(javaPlugin -> this.customizeJavaMavenPublication(publication, project));
		this.suppressMavenOptionalFeatureWarnings(publication);


	}

	private void customizePom(MavenPom pom, Project project) {
		pom.getUrl().set("https://github.com/easybest/spring-data-mybatis");
		pom.getName().set(project.provider(() -> this.determineProjectName(project)));
		// pom.getName().set(project.provider(project::getName));
		pom.getDescription().set(project.provider(project::getDescription));
		if (!this.isUserInherited(project)) {
			pom.organization(this::customizeOrganization);
		}
		pom.licenses(this::customizeLicences);
		pom.developers(this::customizeDevelopers);
		pom.scm(scm -> this.customizeScm(scm, project));
		if (!this.isUserInherited(project)) {
			pom.issueManagement(issueManagement -> this.customizeIssueManagement(issueManagement, project));
		}
	}

	private void customizeJavaMavenPublication(MavenPublication publication, Project project) {
		this.addMavenOptionalFeature(project);
		publication.versionMapping(strategy -> strategy.usage(Usage.JAVA_API,
				mappingStrategy -> mappingStrategy.fromResolutionOf(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)));
		publication.versionMapping(
				strategy -> strategy.usage(Usage.JAVA_RUNTIME, VariantVersionMappingStrategy::fromResolutionResult));
	}

	/**
	 * Add a feature that allows maven plugins to declare optional dependencies that
	 * appear in the POM. This is required to make m2e in Eclipse happy.
	 * @param project the project to add the feature to
	 */
	private void addMavenOptionalFeature(Project project) {
		JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
		JavaPluginConvention convention = project.getConvention().getPlugin(JavaPluginConvention.class);
		extension.registerFeature("mavenOptional",
				feature -> feature.usingSourceSet(convention.getSourceSets().getByName("main")));
		AdhocComponentWithVariants javaComponent = (AdhocComponentWithVariants) project.getComponents()
				.findByName("java");
		javaComponent.addVariantsFromConfiguration(
				project.getConfigurations().findByName("mavenOptionalRuntimeElements"),
				ConfigurationVariantDetails::mapToOptional);
	}

	private void suppressMavenOptionalFeatureWarnings(MavenPublication publication) {
		publication.suppressPomMetadataWarningsFor("mavenOptionalApiElements");
		publication.suppressPomMetadataWarningsFor("mavenOptionalRuntimeElements");
	}

	private void customizeOrganization(MavenPomOrganization organization) {
		organization.getName().set("EasyBest");
		organization.getUrl().set("https://easybest.io");
	}

	private void customizeLicences(MavenPomLicenseSpec licences) {
		licences.license(licence -> {
			licence.getName().set("Apache License, Version 2.0");
			licence.getUrl().set("https://www.apache.org/licenses/LICENSE-2.0.txt");
		});
	}

	private void customizeDevelopers(MavenPomDeveloperSpec developers) {
		developers.developer(developer -> {
			developer.getName().set("Jarvis Song");
			developer.getEmail().set("iamjarvissong@gmail.com");
			developer.getOrganization().set("EasyBest");
			developer.getOrganizationUrl().set("https://easybest.io");
		});
	}

	private void customizeScm(MavenPomScm scm, Project project) {
		scm.getConnection().set("scm:git:git://github.com/easybest/spring-data-mybatis.git");
		scm.getDeveloperConnection().set("scm:git:ssh://git@github.com/easybest/spring-data-mybatis.git");
		scm.getUrl().set("https://github.com/easybest/spring-data-mybatis");
	}

	private void customizeIssueManagement(MavenPomIssueManagement issueManagement, Project project) {
		issueManagement.getSystem().set("GitHub");
		issueManagement.getUrl().set("https://github.com/easybest/spring-data-mybatis/issues");
	}

	private boolean isUserInherited(Project project) {
		return "parent".equals(project.getName()) || project.getName().endsWith("-parent")
				|| project.getName().endsWith("-dependencies");
	}

	private String determineProjectName(Project project) {
		String rootName = project.getRootProject().getName().replace("-build", "");
		if (project.getName().startsWith(rootName)) {
			return project.getName();
		}
		if ("main".equals(project.getName())) {
			return rootName;
		}
		if ("starter".equals(project.getName())) {
			return rootName + "-boot-starter";
		}
		return rootName + '-' + project.getName();
	}

}
