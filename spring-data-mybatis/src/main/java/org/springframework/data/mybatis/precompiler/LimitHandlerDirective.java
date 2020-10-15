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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class LimitHandlerDirective extends Directive {

	@Override
	public String getName() {
		return "LimitHandler";
	}

	@Override
	public int getType() {
		return BLOCK;
	}

	@Override
	public boolean render(InternalContextAdapter context, Writer writer, Node node)
			throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {

		Node selectionNode = node.jjtGetChild(0);
		RowSelection selection = (RowSelection) selectionNode.value(context);

		Node bodyNode = node.jjtGetChild(node.jjtGetNumChildren() - 1);
		StringWriter sw = new StringWriter();
		bodyNode.render(context, sw);

		Dialect dialect = (Dialect) context.get("dialect");
		String sql = dialect.getLimitHandler().processSql(sw.toString(), selection);
		writer.write(sql);
		return true;
	}

}
