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

package org.springframework.data.mybatis.repository.query;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.mybatis.scripting.beetl.BeetlFacade;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mybatis.repository.support.MybatisEntityInformationSupport;
import org.springframework.data.mybatis.repository.support.MybatisEntityModel;
import org.springframework.data.mybatis.repository.support.MybatisQueryException;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * part tree query implementation of {@link org.springframework.data.repository.query.RepositoryQuery}.
 *
 * @author Jarvis Song
 */
public class PartTreeMybatisQuery extends AbstractMybatisQuery {
    private transient static final Logger logger = LoggerFactory.getLogger(PartTreeMybatisQuery.class);

    private static final String MAPPER_BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
            "<mapper namespace=\"#namespace#\">\n@var QUOTA='<include refid=\"_PUBLIC.ALIAS_QUOTA\"/>';\n";
    private static final String MAPPER_END   = "</mapper>";

    private final MybatisEntityInformationSupport entityInformation;
    private final Class<?>                        domainClass;
    private final PartTree                        tree;
    private final MybatisParameters               parameters;

    private String statementName;

    protected PartTreeMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method) {
        super(sqlSessionTemplate, method);

        this.entityInformation = method.getEntityInformation();
        this.domainClass = this.entityInformation.getJavaType();
        this.tree = new PartTree(method.getName(), domainClass);
        this.parameters = method.getParameters();

        statementName = super.getStatementName() + UUID.randomUUID().toString();

        doCreateQueryStatement(method); // prepare mybatis statement.
    }

    @Override
    protected String getStatementName() {
        return statementName;
    }

    private String doCreatePageQueryStatementForSqlServer() {
        Class<?> returnedObjectType = method.getReturnedObjectType();
        if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
            throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<select id=\"" + getStatementName() + "\" lang=\"#lang#\" resultMap=\"ResultMap\">");
        builder.append("<include refid=\"_PUBLIC.PAGER_BEFORE\" />\n");
        if (isBasicQuery()) {
            builder.append("<include refid=\"SELECT_BASIC_PRE\"/>");
        } else {
            builder.append("<include refid=\"SELECT_PRE\"/>");
        }

        builder.append("\n<include refid=\"_PUBLIC.ROW_NUMBER_OVER\" />\n");
        builder.append(createQuerySort(true));
        builder.append("\n<include refid=\"_PUBLIC.AS_ROW_NUM\" />\n");

        builder.append("\n FROM #model.nameInDatabase# #QUOTA+model.name+QUOTA# \n");

        if (!isBasicQuery()) {
            builder.append("@for(entry in model.manyToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n" +
                    "            @for(entry in model.oneToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n");
        }
        builder.append(createQueryCondition());
        builder.append("<include refid=\"_PUBLIC.PAGER_AFTER\" />");
        builder.append("</select>\n");

        builder.append(doCreateCountQueryStatement("count_" + getStatementName()));
        return builder.toString();

    }

    private String doCreateCountQueryStatement(String statementName) {
        StringBuilder builder = new StringBuilder();
        builder.append("<select id=\"" + statementName + "\" lang=\"#lang#\" resultType=\"long\">");

        builder.append("SELECT COUNT(*) FROM #model.nameInDatabase# #QUOTA+model.name+QUOTA# \n");

        if (!isBasicQuery()) {
            builder.append("@for(entry in model.manyToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n" +
                    "            @for(entry in model.oneToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n");
        }
        builder.append(createQueryCondition());

        builder.append("</select>\n");
        return builder.toString();
    }

    private String doCreatePageQueryStatement() {
        Class<?> returnedObjectType = method.getReturnedObjectType();
        if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
            throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
        }

        StringBuilder builder = new StringBuilder();

        builder.append("<select id=\"" + getStatementName() + "\" lang=\"#lang#\" resultMap=\"ResultMap\">");
        builder.append("<include refid=\"_PUBLIC.PAGER_BEFORE\" />\n");
        if (isBasicQuery()) {
            builder.append("<include refid=\"SELECT_BASIC_PRE\"/>");
        } else {
            builder.append("<include refid=\"SELECT_PRE\"/>");
        }
        builder.append("\n FROM #model.nameInDatabase# #QUOTA+model.name+QUOTA# \n");

        if (!isBasicQuery()) {
            builder.append("@for(entry in model.manyToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n" +
                    "            @for(entry in model.oneToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n");
        }
        builder.append(createQueryCondition());
        builder.append(createQuerySort(true));
        builder.append("<include refid=\"_PUBLIC.PAGER_AFTER\" />");
        builder.append("</select>\n");

        builder.append(doCreateCountQueryStatement("count_" + getStatementName()));


        return builder.toString();
    }

    private String doCreateCollectionQueryStatement() {
        Class<?> returnedObjectType = method.getReturnedObjectType();

        if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
            throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
        }

        StringBuilder builder = new StringBuilder();

        builder.append("<select id=\"" + getStatementName() + "\" lang=\"#lang#\" resultMap=\"ResultMap\">");
        if (isBasicQuery()) {
            builder.append("<include refid=\"SELECT_BASIC_PRE\"/>");
        } else {
            builder.append("<include refid=\"SELECT_PRE\"/>");
        }
        builder.append("\n FROM #model.nameInDatabase# #QUOTA+model.name+QUOTA# \n");

        if (!isBasicQuery()) {
            builder.append("@for(entry in model.manyToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n" +
                    "            @for(entry in model.oneToOnes){\n" +
                    "                LEFT OUTER JOIN #entry.value.nameInDatabase# #QUOTA+model.name+'.'+entry.key+QUOTA# ON #QUOTA+model.name+QUOTA#.#entry.value.joinColumnName#=#QUOTA+model.name+'.'+entry.key+QUOTA#.#entry.value.joinReferencedColumnName#\n" +
                    "            @}\n");
        }

        // build condition
        builder.append(createQueryCondition());

        builder.append(createQuerySort(false));
        builder.append("</select>");
        return builder.toString();
    }

    private String createQuerySort(boolean must) {
        Sort sort = tree.getSort();
        StringBuilder sortSQL = new StringBuilder();
        if (null != sort) {
            sortSQL.append(" ORDER BY ");

            for (Iterator<Order> iterator = sort.iterator(); iterator.hasNext(); ) {
                Order order = iterator.next();
                sortSQL.append("<include refid=\"_PUBLIC.ALIAS_QUOTA\"/>" + order.getProperty() + "<include refid=\"_PUBLIC.ALIAS_QUOTA\"/> " + order.getDirection().name() + ",");
            }
            sortSQL.deleteCharAt(sortSQL.length() - 1);
        }

        if (must && sortSQL.length() == 0 && entityInformation.getModel().getPrimaryKeys().size() > 0) {
            sortSQL.append(" ORDER BY <include refid=\"_PUBLIC.ALIAS_QUOTA\"/>" + entityInformation.getModel().getPrimaryKeys().get(0).getName() + "<include refid=\"_PUBLIC.ALIAS_QUOTA\"/> DESC");
        }

        if (must || parameters.hasSortParameter()) {
            StringBuilder builder = new StringBuilder();
            builder.append("\n\\#orderBy(_parameter.sorts,' " + sortSQL.toString() + "')\\#\n");
            return builder.toString();
        }


        return sortSQL.toString();

    }

    private String createQueryCondition() {

        MybatisParameters bindableParameters = parameters.getBindableParameters();
        int bindableParametersNum = bindableParameters.getNumberOfParameters();
        if (bindableParametersNum == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("@trim({prefix:\" WHERE \",prefixOverrides:\" AND| OR\"}){\n");

        int c = 0;
        for (Iterator<PartTree.OrPart> iterator = tree.iterator(); iterator.hasNext(); ) {
            PartTree.OrPart orPart = iterator.next();
            builder.append(" OR (\n");
            builder.append("@trim({prefix:\"\",prefixOverrides:\" AND| OR\"}){\n");
            for (Iterator<Part> it = orPart.iterator(); it.hasNext(); ) {
                Part part = it.next();
                MybatisEntityModel column = entityInformation.getModel().findColumnByPropertyName(part.getProperty().getSegment());
                if (null == column) {
                    throw new MybatisQueryException("can not find property: " + part.getProperty().getSegment() + " in " + method.getName());
                }
                builder.append(" AND ");
                builder.append("#QUOTA+model.name+QUOTA#." + column.getNameInDatabase());


                switch (part.getType()) {
                    case SIMPLE_PROPERTY:
                        builder.append("=");
                        break;
                }

                builder.append("\\#_parameter.p" + c + "\\#");


                c += part.getType().getNumberOfArguments();
            }

            builder.append("@}\n");

            builder.append(")\n");

        }
        builder.append("@}\n");
        return builder.toString();
    }


    private void doCreateQueryStatement(MybatisQueryMethod method) {

        Configuration configuration = sqlSessionTemplate.getConfiguration();

        if (configuration.hasStatement(getStatementId())) {
            statementName = super.getStatementName() + UUID.randomUUID().toString(); // FIXME need do it?
        }

        String statementXML = "";

        if (method.isCollectionQuery()) {
            statementXML = doCreateCollectionQueryStatement();
        } else if (method.isPageQuery()) {
            if ("sqlserver".equals(configuration.getDatabaseId())) {
                statementXML = doCreatePageQueryStatementForSqlServer();
            } else {
                statementXML = doCreatePageQueryStatement();
            }
        } else if (tree.isCountProjection()) {
            statementXML = doCreateCountQueryStatement(getStatementName());
        }


        StringBuilder builder = new StringBuilder();
        builder.append(MAPPER_BEGIN);
        builder.append(statementXML);
        builder.append(MAPPER_END);

        Map<String, Object> context = new HashMap<String, Object>();
        context.put("_mybatis_auto_mapping", "true");
        context.put("lang", LANG_BEETL);
        context.put("namespace", getNamespace());
        context.put("tableName", entityInformation.getModel().getNameInDatabase());
        context.put("model", entityInformation.getModel());
        String xml = BeetlFacade.apply(builder.toString(), context);


        if (logger.isDebugEnabled()) {
            logger.debug("\n******************* Auto Generate MyBatis Mapping XML (" + getStatementId() + ") *******************\n" + xml);
        }
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        String namespace = getNamespace();
        String resource = method.getName() + "_auto_generate.xml";
        try {
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments(), namespace);
            xmlMapperBuilder.parse();
        } catch (Exception e) {
            throw new MappingException("create auto mapping error for " + namespace, e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

    }


}
