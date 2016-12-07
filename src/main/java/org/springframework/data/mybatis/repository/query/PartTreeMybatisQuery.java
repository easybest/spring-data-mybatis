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

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mybatis.repository.dialect.Dialect;
import org.springframework.data.mybatis.repository.query.MybatisQueryExecution.DeleteExecution;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.query.parser.PartTree;

import java.util.UUID;

/**
 * part tree query implementation of {@link org.springframework.data.repository.query.RepositoryQuery}.
 *
 * @author Jarvis Song
 */
public class PartTreeMybatisQuery extends AbstractMybatisQuery {
    private transient static final Logger logger = LoggerFactory.getLogger(PartTreeMybatisQuery.class);

    private static final String MAPPER_BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">";
    private static final String MAPPER_END   = "</mapper>";

    private final Dialect           dialect;
    private final EntityMetadata    entityInformation;
    private final Class<?>          domainClass;
    private final PartTree          tree;
    private final MybatisParameters parameters;
//    private final MybatisEntityModel              model;
//    private final MybatisMapperGenerator          generator;

    private final String statementName;

    protected PartTreeMybatisQuery(SqlSessionTemplate sqlSessionTemplate, Dialect dialect, MybatisQueryMethod method) {
        super(sqlSessionTemplate, method);
        this.dialect = dialect;
        this.entityInformation = method.getEntityInformation();
        this.domainClass = this.entityInformation.getJavaType();
        this.tree = new PartTree(method.getName(), domainClass);
        this.parameters = method.getParameters();
//        this.model = this.entityInformation.getModel();
//        this.generator = new MybatisMapperGenerator(model, dialect);
        this.statementName = super.getStatementName() + UUID.randomUUID().toString().replace("-", "");

//        doCreateQueryStatement(method); // prepare mybatis statement.
    }

    private String quota(String alias) {
        return dialect.openQuote() + alias + dialect.closeQuote();
    }

    @Override
    protected MybatisQueryExecution getExecution() {
        if (tree.isDelete()) {
            return new DeleteExecution();
        }
        return super.getExecution();
    }

    @Override
    protected String getStatementName() {
        return statementName;
    }


//    private String buildQueryCondition(boolean basic) {
//
//        StringBuilder builder = new StringBuilder();
//        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
//
//        int c = 0;
//        for (Iterator<PartTree.OrPart> iterator = tree.iterator(); iterator.hasNext(); ) {
//            PartTree.OrPart orPart = iterator.next();
//            builder.append(" or (");
//            builder.append("<trim prefix=\"\" prefixOverrides=\"and |or \">");
//
//            for (Iterator<Part> it = orPart.iterator(); it.hasNext(); ) {
//                String columnName = null;
//                Part part = it.next();
//                MybatisEntityModel column = model.findColumnByPropertyName(part.getProperty().getSegment());
//                if (null != column) {
//                    columnName = quota(model.getName()) + "." + column.getNameInDatabase();
//                } else if (!basic) {
//                    MybatisEntityModel oneToOne = model.findOneToOneByPropertyName(part.getProperty().getSegment());
//                    if (null != oneToOne) {
//                        MybatisEntityModel oneTonOneColumn = oneToOne.findColumnByPropertyName(part.getProperty().getLeafProperty().getSegment());
//                        if (null != oneTonOneColumn) {
//                            columnName = quota(model.getName() + "." + part.getProperty().getSegment()) + "." + oneTonOneColumn.getNameInDatabase();
//                        }
//                    } else {
//                        MybatisEntityModel manyToOne = model.findManyToOneByPropertyName(part.getProperty().getSegment());
//                        if (null != manyToOne) {
//                            MybatisEntityModel manyTonOneColumn = manyToOne.findColumnByPropertyName(part.getProperty().getLeafProperty().getSegment());
//                            if (null != manyTonOneColumn) {
//                                columnName = quota(model.getName() + "." + part.getProperty().getSegment()) + "." + manyTonOneColumn.getNameInDatabase();
//                            }
//                        } else {
//                            MybatisEntityModel manyToMany = model.findManyToManyByPropertyName(part.getProperty().getSegment());
//                            if (null != manyToMany) {
//                                throw new MybatisQueryNotSupportException("now we can not support @ManyToMany query.");
//                            }
//                        }
//                    }
//
//
//                }
//
//                if (null == columnName) {
//                    throw new MybatisQueryException("can not find property: " + part.getProperty().getSegment() + " in " + method.getName());
//                }
//
//                builder.append(" and ");
//
//                IgnoreCaseType ignoreCaseType = part.shouldIgnoreCase();
//                if (ignoreCaseType == IgnoreCaseType.ALWAYS || ignoreCaseType == IgnoreCaseType.WHEN_POSSIBLE) {
//                    builder.append("upper(").append(columnName).append(")");
//                } else {
//                    builder.append(columnName);
//                }
//
//                switch (part.getType()) {
//                    case SIMPLE_PROPERTY:
//                        builder.append("=");
//                        break;
//                    case NEGATING_SIMPLE_PROPERTY:
//                        builder.append("<![CDATA[<>]]>");
//                        break;
//                    case LESS_THAN:
//                    case BEFORE:
//                        builder.append("<![CDATA[<]]>");
//                        break;
//                    case LESS_THAN_EQUAL:
//                        builder.append("<![CDATA[<=]]>");
//                        break;
//                    case GREATER_THAN:
//                    case AFTER:
//                        builder.append("<![CDATA[>]]>");
//                        break;
//                    case GREATER_THAN_EQUAL:
//                        builder.append("<![CDATA[>=]]>");
//                        break;
//
//                    case LIKE:
//                    case NOT_LIKE:
//                    case STARTING_WITH:
//                    case ENDING_WITH:
//                        if (part.getType() == NOT_LIKE) {
//                            builder.append(" not");
//                        }
//                        builder.append(" like ");
//                        break;
//                    case CONTAINING:
//                    case NOT_CONTAINING:
//                        if (part.getType() == NOT_CONTAINING) {
//                            builder.append(" not");
//                        }
//                        builder.append(" like ");
//                        break;
//                    case IN:
//                    case NOT_IN:
//                        if (part.getType() == NOT_IN) {
//                            builder.append(" not");
//                        }
//                        builder.append(" in ");
//                        break;
//                }
//
//                switch (part.getType()) {
//                    case CONTAINING:
//                    case NOT_CONTAINING:
//                        if (ignoreCaseType == IgnoreCaseType.ALWAYS || ignoreCaseType == IgnoreCaseType.WHEN_POSSIBLE) {
//                            builder.append("concat('%',upper(#{p" + c + "}),'%')");
//                        } else {
//                            builder.append("concat('%',#{p" + c + "},'%')");
//                        }
//                        break;
//                    case STARTING_WITH:
//                        if (ignoreCaseType == IgnoreCaseType.ALWAYS || ignoreCaseType == IgnoreCaseType.WHEN_POSSIBLE) {
//                            builder.append("concat(upper(#{p" + c + "}),'%')");
//                        } else {
//                            builder.append("concat(#{p" + c + "},'%')");
//                        }
//                        break;
//                    case ENDING_WITH:
//                        if (ignoreCaseType == IgnoreCaseType.ALWAYS || ignoreCaseType == IgnoreCaseType.WHEN_POSSIBLE) {
//                            builder.append("concat('%',upper(#{p" + c + "}))");
//                        } else {
//                            builder.append("concat('%',#{p" + c + "})");
//                        }
//                        break;
//                    case IN:
//                    case NOT_IN:
//                        builder.append("<foreach item=\"item\" index=\"index\" collection=\"p" + c + "\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
//                        break;
//                    case IS_NOT_NULL:
//                        builder.append(" is not null");
//                        break;
//                    case IS_NULL:
//                        builder.append(" is null");
//                        break;
//
//                    case TRUE:
//                        builder.append("=true");
//                        break;
//                    case FALSE:
//                        builder.append("=false");
//                        break;
//
//                    default:
//                        if (ignoreCaseType == IgnoreCaseType.ALWAYS || ignoreCaseType == IgnoreCaseType.WHEN_POSSIBLE) {
//                            builder.append("upper(#{p" + c + "})");
//                        } else {
//                            builder.append("#{p" + c + "}");
//                        }
//                        break;
//                }
//
//                c += part.getType().getNumberOfArguments();
//            }
//
//            builder.append("</trim>");
//
//            builder.append(" )");
//
//        }
//        builder.append("</trim>");
//        return builder.toString();
//    }
//
//
//    private String doCreateDeleteQueryStatement() {
//        StringBuilder builder = new StringBuilder();
//        builder.append("<delete id=\"" + getStatementName() + "\" lang=\"XML\">");
//        builder.append("delete");
//        if (dialect.supportsDeleteAlias()) {
//            builder.append(" ").append(quota(model.getName()));
//        }
//        builder.append(" from ").append(generator.buildFrom(isBasicQuery())).append(" ");
//        builder.append(buildQueryCondition(isBasicQuery()));
//        builder.append("</delete>");
//
//        if (method.isCollectionQuery()) {
//            // query first, then delete
//            builder.append(doCreateSelectQueryStatement("query_" + getStatementName()));
//        }
//
//        return builder.toString();
//    }
//
//
//    private String doCreateCountQueryStatement(String statementName) {
//        StringBuilder builder = new StringBuilder();
//        builder.append("<select id=\"" + statementName + "\" lang=\"XML\" resultType=\"long\">");
//        builder.append("select count(*) from ");
//        builder.append(generator.buildFrom(isBasicQuery()));
//        builder.append(buildQueryCondition(isBasicQuery()));
//        builder.append("</select>");
//        return builder.toString();
//    }
//
//
//    private String doCreatePageQueryStatement(boolean includeCount) {
//        Class<?> returnedObjectType = method.getReturnedObjectType();
//        if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
//            throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
//        }
//        StringBuilder builder = new StringBuilder();
//        StringBuilder condition = new StringBuilder();
//        condition.append(buildQueryCondition(isBasicQuery()));
//        builder.append("<select id=\"" + statementName + "\" lang=\"XML\" resultMap=\"ResultMap\">");
//        builder.append(dialect.getLimitHandler().processSql(true, generator.buildSelectColumns(isBasicQuery()), " from " + generator.buildFrom(isBasicQuery()), condition.toString(), generator.buildSorts(isBasicQuery(), tree.getSort())));
//        builder.append("</select>");
//
//        if (includeCount) {
//            builder.append(doCreateCountQueryStatement("count_" + getStatementName()));
//        }
//
//        return builder.toString();
//    }
//
//    private String doCreateSelectQueryStatement(String statementName) {
//        StringBuilder builder = new StringBuilder();
//        builder.append("<select id=\"" + statementName + "\" lang=\"XML\" resultMap=\"ResultMap\">");
//        builder.append("select ");
//
//        if (tree.isDistinct()) {
//            builder.append(" distinct ");
//        }
//
//        builder.append(generator.buildSelectColumns(isBasicQuery()));
//        builder.append(" from ");
//        builder.append(generator.buildFrom(isBasicQuery()));
//        // build condition
//        builder.append(buildQueryCondition(isBasicQuery()));
//
//        builder.append(generator.buildSorts(isBasicQuery(), tree.getSort()));
//
//        builder.append("</select>");
//        return builder.toString();
//    }
//
//    private String doCreateCollectionQueryStatement() {
//        Class<?> returnedObjectType = method.getReturnedObjectType();
//
//        if (returnedObjectType != domainClass && !returnedObjectType.isAssignableFrom(domainClass)) {
//            throw new IllegalArgumentException("return object type must be or assignable from " + domainClass);
//        }
//
//        return doCreateSelectQueryStatement(getStatementName());
//    }
//
//
//    private void doCreateQueryStatement(MybatisQueryMethod method) {
//
//        Configuration configuration = sqlSessionTemplate.getConfiguration();
//
//        String statementXML = "";
//        if (tree.isDelete()) {
//            statementXML = doCreateDeleteQueryStatement();
//        } else if (tree.isCountProjection()) {
//            statementXML = doCreateCountQueryStatement(getStatementName());
//        } else if (method.isPageQuery()) {
//            statementXML = doCreatePageQueryStatement(true);
//        } else if (method.isSliceQuery()) {
//            statementXML = doCreatePageQueryStatement(false);
//        } else if (method.isStreamQuery()) {
//        } else if (method.isCollectionQuery()) {
//            statementXML = doCreateCollectionQueryStatement();
//        } else if (method.isQueryForEntity()) {
//            statementXML = doCreateSelectQueryStatement(getStatementName());
//        }
//
//
//        StringBuilder builder = new StringBuilder();
//        builder.append(MAPPER_BEGIN);
//        builder.append("<mapper namespace=\"" + getNamespace() + "\">");
//        builder.append(statementXML);
//        builder.append(MAPPER_END);
//
//
//        String xml = builder.toString();
//
//        if (logger.isDebugEnabled()) {
//            logger.debug("\n******************* Auto Generate MyBatis Mapping XML (" + getStatementId() + ") *******************\n" + xml);
//        }
//        InputStream inputStream = null;
//        try {
//            inputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
//        } catch (UnsupportedEncodingException e) {
//            // ignore
//        }
//        String namespace = getNamespace();
//        String resource = getStatementId() + "_auto_generate.xml";
//        try {
//            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments(), namespace);
//            xmlMapperBuilder.parse();
//        } catch (Exception e) {
//            throw new MappingException("create auto mapping error for " + namespace, e);
//        } finally {
//            try {
//                inputStream.close();
//            } catch (IOException e) {
//                logger.error(e.getMessage(), e);
//            }
//        }
//
//    }


}
