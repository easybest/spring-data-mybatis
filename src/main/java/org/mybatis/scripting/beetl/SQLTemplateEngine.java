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
 * @author jarvis@ifrabbit.com
 * @date 16/3/9
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
