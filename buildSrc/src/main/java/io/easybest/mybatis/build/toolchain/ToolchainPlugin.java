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

package io.easybest.mybatis.build.toolchain;

import java.util.Arrays;
import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

/**
 * {@link Plugin} for customizing Gradle's toolchain support.
 *
 * @author Jarvis Song
 */
public class ToolchainPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		this.configureToolchain(project);
	}

	private void configureToolchain(Project project) {
		ToolchainExtension toolchain = project.getExtensions().create("toolchain", ToolchainExtension.class, project);
		JavaLanguageVersion toolchainVersion = toolchain.getJavaVersion();
		if (null != toolchainVersion) {
			project.afterEvaluate(evaluated -> this.configure(evaluated, toolchain));
		}
	}

	private void configure(Project project, ToolchainExtension toolchain) {
		if (!this.isJavaVersionSupported(toolchain, toolchain.getJavaVersion())) {
			this.disableToolchainTasks(project);
		}
		else {
			JavaToolchainSpec toolchainSpec = project.getExtensions().getByType(JavaPluginExtension.class)
					.getToolchain();
			toolchainSpec.getLanguageVersion().set(toolchain.getJavaVersion());
			this.configureJavaCompileToolchain(project);
			this.configureTestToolchain(project);
		}
	}

	private boolean isJavaVersionSupported(ToolchainExtension toolchain, JavaLanguageVersion toolchainVersion) {
		return toolchain.getMaximumCompatibleJavaVersion().map(version -> version.canCompileOrRun(toolchainVersion))
				.getOrElse(true);
	}

	private void disableToolchainTasks(Project project) {
		project.getTasks().withType(JavaCompile.class, task -> task.setEnabled(false));
		project.getTasks().withType(Javadoc.class, task -> task.setEnabled(false));
		project.getTasks().withType(Test.class, task -> task.setEnabled(false));
	}

	private void configureJavaCompileToolchain(Project project) {
		project.getTasks().withType(JavaCompile.class, compile -> {
			compile.getOptions().setFork(true);
			// See https://github.com/gradle/gradle/issues/15538
			List<String> forkArgs = Arrays.asList("--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED");
			compile.getOptions().getForkOptions().getJvmArgs().addAll(forkArgs);
		});
	}

	private void configureTestToolchain(Project project) {
		project.getTasks().withType(Test.class, test -> {
			// See https://github.com/spring-projects/spring-ldap/issues/570
			List<String> arguments = Arrays.asList("--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED",
					"--illegal-access=warn");
			test.jvmArgs(arguments);
		});
	}

}
