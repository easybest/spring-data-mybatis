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
package org.springframework.data.mybatis.repository.query;

import java.util.Iterator;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * .
 *
 * @author JARVIS SONG
 */
class TreeQueryCreator extends AbstractQueryCreator<String, StringBuilder> {

	private final ParameterMetadataProvider provider;

	private final ReturnedType returnedType;

	private final PartTree tree;

	private final EscapeCharacter escape;

	TreeQueryCreator(PartTree tree, ReturnedType type, ParameterMetadataProvider provider) {
		super(tree);

		this.tree = tree;
		this.provider = provider;
		this.returnedType = type;
		this.escape = provider.getEscape();
	}

	@Override
	protected StringBuilder create(Part part, Iterator<Object> iterator) {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		return builder;
	}

	@Override
	protected StringBuilder and(Part part, StringBuilder base, Iterator<Object> iterator) {
		return null;
	}

	@Override
	protected StringBuilder or(StringBuilder base, StringBuilder criteria) {
		return null;
	}

	@Override
	protected String complete(StringBuilder criteria, Sort sort) {
		return criteria.toString();
	}

}
