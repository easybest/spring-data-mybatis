/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.mybatis.scripting.beetl;

import org.beetl.core.GroupTemplate;
import org.beetl.core.Resource;
import org.beetl.core.engine.DefaultTemplateEngine;
import org.beetl.core.engine.StatementParser;
import org.beetl.core.statement.PlaceholderST;
import org.beetl.core.statement.Program;
import org.beetl.core.statement.Statement;

import java.io.Reader;
import java.util.Map;

/**
 * Template engine for mybatis's sql analysis.
 *
 */
public class SQLTemplateEngine extends DefaultTemplateEngine {

    @Override
    public Program createProgram(Resource resource, Reader reader, Map<Integer, String> textMap, String cr, GroupTemplate gt) {
        Program program = super.createProgram(resource, reader, textMap, cr, gt);

        Statement[] statements = program.metaData.statements;
        StatementParser parser = new StatementParser(statements, gt, resource.getId());

        parser.addListener(PlaceholderST.class, new PlaceHolderListener());
//        parser.addListener(VarRef.class, new PlaceHolderListener());

        parser.parse();

        return program;
    }
}
