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

package io.easybest.mybatis.repository.support;

import io.easybest.mybatis.repository.query.EscapeCharacter;

import org.springframework.data.spel.spi.EvaluationContextExtension;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisEvaluationContextExtension implements EvaluationContextExtension {

	private final MybatisRootObject root;

	public MybatisEvaluationContextExtension(char escapeCharacter) {

		this.root = MybatisRootObject.of(EscapeCharacter.of(escapeCharacter));
	}

	@Override
	public String getExtensionId() {
		return "mybatis";
	}

	@Override
	public Object getRootObject() {
		return this.root;
	}

	public static class MybatisRootObject {

		private final EscapeCharacter character;

		public MybatisRootObject(EscapeCharacter character) {
			this.character = character;
		}

		public static MybatisRootObject of(EscapeCharacter character) {
			return new MybatisRootObject(character);
		}

		public String escape(String source) {
			return this.character.escape(source);
		}

		public char escapeCharacter() {
			return this.character.getEscapeCharacter();
		}

	}

}
