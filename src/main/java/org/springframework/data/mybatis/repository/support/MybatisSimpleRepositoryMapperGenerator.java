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

package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mybatis.repository.localism.Localism;
import org.springframework.data.mybatis.repository.localism.identity.IdentityColumnSupport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * generate basic mapper for simple repository automatic.
 *
 * @author Jarvis Song
 */
public class MybatisSimpleRepositoryMapperGenerator {
    private transient static final Logger logger = LoggerFactory.getLogger(MybatisSimpleRepositoryMapperGenerator.class);

    private static final String MAPPER_BEGIN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">";
    private static final String MAPPER_END   = "</mapper>";

    private final Configuration          configuration; // mybatis's configuration
    private final MybatisEntityModel     model; // entity's mapper model
    private final Localism               localism;
    private final MybatisMapperGenerator generator;


    public MybatisSimpleRepositoryMapperGenerator(Configuration configuration, MybatisEntityModel model, Localism localism) {
        this.configuration = configuration;
        this.model = model;
        this.localism = localism;
        generator = new MybatisMapperGenerator(model, localism);
    }

    public void generate() {
        String xml;
        String namespace = model.getClz().getName();
        try {
            xml = render();
        } catch (IOException e) {
            throw new MappingException("create auto mapping error for " + namespace, e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("\n******************* Auto Generate MyBatis Mapping XML (" + namespace + ") *******************\n" + xml);
        }
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        String resource = namespace + "_auto_generate.xml";
        try {
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments(), namespace);
            xmlMapperBuilder.parse();
        } catch (Exception e) {
            logger.warn(xml);
            throw new MappingException("create auto mapping error for " + namespace, e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private String render() throws IOException {


        StringBuilder builder = new StringBuilder();
        builder.append(MAPPER_BEGIN);

        builder.append("<mapper namespace=\"" + model.getClz().getName() + "\">");

        if (!isFragmentExist("TABLE_NAME")) {
            builder.append("<sql id=\"TABLE_NAME\">" + model.getNameInDatabase() + "</sql>");
        }
        if (localism.supportsSequences()) {
            if (!isFragmentExist("SEQUENCE")) {
                builder.append("<sql id=\"SEQUENCE\">" + localism.getSequenceNextValString(model.getSequenceName()) + "</sql>");
            }
        }
        if (!isFragmentExist("SELECT_CONDITION_INNER")) {
            builder.append("<sql id=\"SELECT_CONDITION_INNER\"></sql>");
        }
        if (!isResultMapExist("ResultMap")) {
            buildResultMap(builder);
        }
        if (!isStatementExist("_insert")) {
            buildInsertSQL(builder);
        }
        if (!isStatementExist("_update")) {
            buildUpdateSQL(builder);
        }
        if (!isStatementExist("_getById")) {
            buildGetById(builder);
        }
        if (!isStatementExist("_findAll")) {
            buildFindAll(builder);
        }
        if (!isStatementExist("_findBasicAll")) {
            buildFindBasicAll(builder);
        }
        if (!isStatementExist("_count")) {
            buildCount(builder);
        }
        if (!isStatementExist("_deleteById")) {
            buildDeleteById(builder);
        }
        if (!isStatementExist("_deleteAll")) {
            buildDeleteAll(builder);
        }
        if (!isStatementExist("_findByPager")) {
            buildFindByPager(builder);
        }
        if (!isStatementExist("_findBasicByPager")) {
            buildFindBasicByPager(builder);
        }
        if (!isStatementExist("_countByCondition")) {
            buildCountByCondition(builder);
        }
        if (!isStatementExist("_countBasicByCondition")) {
            buildCountBasicByCondition(builder);
        }
        if (!isStatementExist("_getBasicById")) {
            buildGetBasicById(builder);
        }
        if (!isStatementExist("_deleteByCondition")) {
            buildDeleteByCondition(builder);
        }
        builder.append(MAPPER_END);
        String result = builder.toString();


        return result;
    }

    private void buildUpdateSQL(StringBuilder builder) {
        builder.append("<update id=\"_update\" parameterType=\"" + model.getClz().getName() + "\" lang=\"XML\">");
        builder.append("update ").append(model.getNameInDatabase());
        builder.append("<set>");
        for (Map.Entry<String, MybatisEntityModel> entry : model.getColumns().entrySet()) {
            builder.append("<if test=\"" + entry.getValue().getName() + " != null\">")
                    .append(entry.getValue().getNameInDatabase()).append("=#{").append(entry.getValue().getName()).append("}").append(",</if>");
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getJoinColumns().entrySet()) {
            builder.append("<if test=\"" + entry.getValue().getName().split("\\.")[0] + " !=null and " + entry.getValue().getName() + " != null\">")
                    .append(entry.getValue().getNameInDatabase()).append("=#{").append(entry.getValue().getName()).append("}").append(",</if>");
        }
        builder.append("</set>");
        builder.append("<trim prefix=\"where\" prefixOverrides=\"and |or \">");
        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append("and ").append(entry.getValue().getNameInDatabase()).append("=");
            builder.append("#{").append(entry.getValue().getName()).append("}");
        }
        builder.append("</trim>");
        builder.append("</update>");
    }

    private void buildDeleteByCondition(StringBuilder builder) {
        builder.append("<delete id=\"_deleteByCondition\" lang=\"XML\">");
        builder.append("delete");
        if (localism.supportsDeleteAlias()) {
            builder.append(" ").append(quota(model.getName()));
        }
        builder.append(" from ").append(generator.buildFrom(true));

        builder.append("<if test=\"_condition != null\">");
        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        builder.append(buildCondition());
        builder.append("</trim>");
        builder.append("</if>");
        builder.append("</delete>");
    }


    private void buildGetBasicById(StringBuilder builder) {

        builder.append("<select id=\"_getBasicById\" parameterType=\"" + model.getPrimaryKey().getClz().getName() + "\" resultMap=\"ResultMap\" lang=\"XML\">");
        builder.append("select ").append(generator.buildSelectColumns(true)).append(" from ").append(generator.buildFrom(true));
        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append("and ").append(quota(model.getName())).append(".").append(entry.getValue().getNameInDatabase()).append("=");
            builder.append("#{" + entry.getValue().getName() + "}");
        }
        builder.append("</trim>");
        builder.append("</select>");
    }

    private void buildFindBasicByPager(StringBuilder builder) {

        builder.append("<select id=\"_findBasicByPager\" resultMap=\"ResultMap\" lang=\"XML\">");
        StringBuilder condition = new StringBuilder();
        condition.append("<if test=\"_condition != null\">");
        condition.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        condition.append(buildCondition());
        condition.append("</trim>");
        condition.append("</if>");

        builder.append(localism.getLimitHandler().processSql(true, generator.buildSelectColumns(true), " from " + generator.buildFrom(true), condition.toString(), generator.buildSorts(true, null)));

        builder.append("</select>");
    }


    private void buildFindByPager(StringBuilder builder) {
        builder.append("<select id=\"_findByPager\" resultMap=\"ResultMap\" lang=\"XML\">");
        StringBuilder condition = new StringBuilder();
        condition.append("<if test=\"_condition != null\">");
        condition.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        condition.append(buildCondition());
        condition.append("</trim>");
        condition.append("</if>");

        builder.append(localism.getLimitHandler().processSql(true, generator.buildSelectColumns(false), " from " + generator.buildFrom(false), condition.toString(), generator.buildSorts(false, null)));

        builder.append("</select>");
    }

    private void buildDeleteAll(StringBuilder builder) {
        builder.append("<delete id=\"_deleteAll\">truncate table " + model.getNameInDatabase() + " </delete>");
    }

    private void buildDeleteById(StringBuilder builder) {
        builder.append("<delete id=\"_deleteById\" parameterType=\"" + model.getPrimaryKey().getClz().getName() + "\" lang=\"XML\">");

        builder.append("delete");
        if (localism.supportsDeleteAlias()) {
            builder.append(" ").append(quota(model.getName()));
        }
        builder.append(" from ").append(generator.buildFrom(true));

        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append("and ").append(entry.getValue().getNameInDatabase()).append("=#{" + entry.getValue().getName() + "}");
        }

        builder.append("</trim>");
        builder.append("</delete>");
    }

    private void buildCount(StringBuilder builder) {
        builder.append("<select id=\"_count\" resultType=\"long\" lang=\"XML\">");
        builder.append("select count(*) from ");
        builder.append(generator.buildFrom(true));
        builder.append("</select>");

    }

    private void buildCountByCondition(StringBuilder builder) {
        builder.append("<select id=\"_countByCondition\" resultType=\"long\" lang=\"XML\">");

        builder.append("select count(*) from ").append(generator.buildFrom(false));

        builder.append("<if test=\"_condition != null\">");
        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        builder.append(buildCondition());
        builder.append("</trim>");
        builder.append("</if>");

        builder.append("</select>");
    }

    private void buildCountBasicByCondition(StringBuilder builder) {
        builder.append("<select id=\"_countBasicByCondition\" resultType=\"long\" lang=\"XML\">");

        builder.append("select count(*) from ").append(generator.buildFrom(true));

        builder.append("<if test=\"_condition != null\">");
        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        builder.append(buildCondition());
        builder.append("</trim>");
        builder.append("</if>");

        builder.append("</select>");
    }


    private String buildCondition() {
        StringBuilder builder = new StringBuilder();
        for (MybatisEntityModel.SearchModel sm : model.getSearchModels()) {
            builder.append("<if test=\"_condition." + sm.getPropertyName() + "\">");
            builder.append(" and ").append(quota(null == sm.getAlias() ? model.getName() : sm.getAlias())).append(".").append(sm.getColumnName());
            builder.append("<![CDATA[").append(sm.getOper()).append("]]>");
            if ("IN".equalsIgnoreCase(sm.getOperate()) || "NOTIN".equalsIgnoreCase(sm.getOperate())) {
                builder.append("<foreach item=\"item\" index=\"index\" collection=\"_condition." + sm.getPropertyName() + "\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
                builder.append("#{item}");
                builder.append("</foreach>");
            } else {
                builder.append("#{_condition." + sm.getPropertyName() + "}");
            }
            builder.append("</if>");
        }

        return builder.toString();

    }

    private void buildFindBasicAll(StringBuilder builder) {
        builder.append("<select id=\"_findBasicAll\" resultMap=\"ResultMap\" lang=\"XML\">");

        builder.append("select ").append(generator.buildSelectColumns(true)).append(" from ").append(generator.buildFrom(true));


        builder.append("<if test=\"_condition != null\">");
        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        builder.append(buildCondition());
        builder.append("</trim>");
        builder.append("</if>");

        builder.append("<if test=\"_ids != null\">");
        if (model.isCompositeId()) {
            //TODO
        } else {
            builder.append(" where ").append(quota(model.getName())).append(".").append(model.getPrimaryKey().getNameInDatabase()).append(" in ");
            builder.append("<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
        }
        builder.append("</if>");

        builder.append(generator.buildSorts(true, null));

        builder.append("</select>");
    }

    private void buildFindAll(StringBuilder builder) {
        builder.append("<select id=\"_findAll\" resultMap=\"ResultMap\" lang=\"XML\">");
        builder.append("select ").append(generator.buildSelectColumns(false)).append(" from ").append(generator.buildFrom(false));

        builder.append("<if test=\"_condition != null\">");
        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        builder.append(buildCondition());
        builder.append("</trim>");
        builder.append("</if>");

        builder.append("<if test=\"_ids != null\">");
        if (model.isCompositeId()) {
            //TODO
        } else {
            builder.append(" where ").append(quota(model.getName())).append(".").append(model.getPrimaryKey().getNameInDatabase()).append(" in ");
            builder.append("<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
        }
        builder.append("</if>");

        builder.append(generator.buildSorts(false, null));

        builder.append("</select>");
    }

    private void buildGetById(StringBuilder builder) {
        builder.append("<select id=\"_getById\" parameterType=\"" + model.getPrimaryKey().getClz().getName() + "\" resultMap=\"ResultMap\" lang=\"XML\">");
        builder.append("select ").append(generator.buildSelectColumns(false)).append(" from ").append(generator.buildFrom(false));

        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append("and ").append(quota(model.getName())).append(".").append(entry.getValue().getNameInDatabase()).append("=");
            builder.append("#{" + entry.getValue().getName() + "}");
        }
        builder.append("</trim>");

        builder.append("</select>");
    }

    private void buildInsertSQL(StringBuilder builder) {
        builder.append("<insert id=\"_insert\" parameterType=\"" + model.getClz().getName() + "\" lang=\"XML\"");

        if (!model.isCompositeId() && null != model.getPrimaryKey() && model.getPrimaryKey().isGeneratedValue()) {
            builder.append(" keyProperty=\"" + model.getPrimaryKey().getName() + "\" keyColumn=\"" + model.getPrimaryKey().getNameInDatabase() + "\"");
        }
        builder.append(">");
        IdentityColumnSupport identityColumnSupport = localism.getIdentityColumnSupport();

        if (!model.isCompositeId() && null != model.getPrimaryKey() && model.getPrimaryKey().isGeneratedValue()) {
            if (identityColumnSupport.supportsIdentityColumns()) {
                builder.append("<selectKey keyProperty=\"" + model.getPrimaryKey().getName() + "\" resultType=\"" + model.getPrimaryKey().getClz().getName() + "\" order=\"AFTER\">");
                builder.append(identityColumnSupport.getIdentitySelectString(model.getNameInDatabase(), model.getPrimaryKey().getNameInDatabase(), model.getPrimaryKey().getTypes()));
                builder.append("</selectKey>");
            } else if (localism.supportsSequences()) {
                builder.append("<selectKey keyProperty=\"" + model.getPrimaryKey().getName() + "\" resultType=\"" + model.getPrimaryKey().getClz().getName() + "\" order=\"BEFORE\">");
                builder.append("<include refid=\"SEQUENCE\" />");
                builder.append("</selectKey>");
            }
        }

        builder.append("<![CDATA[");

        builder.append("insert into ").append(model.getNameInDatabase()).append("(");

        // first, process primary key

        if (!model.isCompositeId() && null != model.getPrimaryKey() && model.getPrimaryKey().isGeneratedValue()) {

            if (!identityColumnSupport.supportsIdentityColumns()) {
                builder.append(model.getPrimaryKey().getNameInDatabase()).append(",");
            }

        } else {
            for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
                builder.append(entry.getValue().getNameInDatabase()).append(",");
            }
        }
        // normal columns
        for (Map.Entry<String, MybatisEntityModel> entry : model.getColumns().entrySet()) {
            builder.append(entry.getValue().getNameInDatabase()).append(",");
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getJoinColumns().entrySet()) {
            builder.append(entry.getValue().getNameInDatabase()).append(",");
        }

        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        builder.append(") values (");

        if (!model.isCompositeId() && null != model.getPrimaryKey() && model.getPrimaryKey().isGeneratedValue()) {
            if (!identityColumnSupport.supportsIdentityColumns()) {
                builder.append("#{" + model.getPrimaryKey().getName() + ",jdbcType=" + model.getPrimaryKey().getJdbcType() + "},");
            }
        } else {
            for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
                builder.append("#{" + entry.getValue().getName() + ",jdbcType=" + entry.getValue().getJdbcType() + "},");
            }
        }

        for (Map.Entry<String, MybatisEntityModel> entry : model.getColumns().entrySet()) {
            builder.append("#{" + entry.getValue().getName() + ",jdbcType=" + entry.getValue().getJdbcType() + "},");
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getJoinColumns().entrySet()) {
            builder.append("#{" + entry.getValue().getName() + ",jdbcType=" + entry.getValue().getJdbcType() + "},");
        }
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(")]]>");


        builder.append("</insert>");
    }

    private void buildResultMap(StringBuilder builder) {
        builder.append("<resultMap id=\"ResultMap\" type=\"" + model.getClz().getName() + "\">");
        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append(String.format("<id property=\"%s\" column=\"%s\" javaType=\"%s\" jdbcType=\"%s\"/>",
                    entry.getValue().getName(),
                    alias(entry.getValue().getName()),
                    entry.getValue().getClz().getName(),
                    entry.getValue().getJdbcType()
            ));
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getColumns().entrySet()) {
            builder.append(String.format("<result property=\"%s\" column=\"%s\" javaType=\"%s\" jdbcType=\"%s\"/>",
                    entry.getValue().getName(),
                    alias(entry.getValue().getName()),
                    entry.getValue().getClz().getName(),
                    entry.getValue().getJdbcType()
            ));
        }
        buildAssociationResultMap(builder, model.getOneToOnes());
        buildAssociationResultMap(builder, model.getManyToOnes());

        for (Map.Entry<String, MybatisEntityModel> entry : model.getOneToManys().entrySet()) {
            builder.append(String.format("<collection property=\"%s\" ofType=\"%s\">",
                    entry.getKey(),
                    entry.getValue().getClz().getName()
            ));
            builder.append("</collection>");
        }
        builder.append("</resultMap>");
    }

    private void buildAssociationResultMap(StringBuilder builder, Map<String, MybatisEntityModel> associations) {
        for (Map.Entry<String, MybatisEntityModel> entry : associations.entrySet()) {
            builder.append(String.format("<association property=\"%s\" javaType=\"%s\">",
                    entry.getKey(),
                    entry.getValue().getClz().getName()
            ));
            for (Map.Entry<String, MybatisEntityModel> subEntry : entry.getValue().getPrimaryKeys().entrySet()) {
                builder.append(String.format("<id property=\"%s\" column=\"%s\" javaType=\"%s\" jdbcType=\"%s\"/>",
                        subEntry.getValue().getName(),
                        alias(entry.getKey() + "." + subEntry.getValue().getName()),
                        subEntry.getValue().getClz().getName(),
                        subEntry.getValue().getJdbcType()
                ));
            }
            for (Map.Entry<String, MybatisEntityModel> subEntry : entry.getValue().getColumns().entrySet()) {
                builder.append(String.format("<result property=\"%s\" column=\"%s\" javaType=\"%s\" jdbcType=\"%s\"/>",
                        subEntry.getValue().getName(),
                        alias(entry.getKey() + "." + subEntry.getValue().getName()),
                        subEntry.getValue().getClz().getName(),
                        subEntry.getValue().getJdbcType()
                ));
            }
            for (Map.Entry<String, MybatisEntityModel> subEntry : entry.getValue().getJoinColumns().entrySet()) {
                builder.append(String.format("<association property=\"%s\" javaType=\"%s\">",
                        subEntry.getValue().getParent().getName(),
                        subEntry.getValue().getParent().getClz().getName()
                ));
                builder.append(String.format("<result property=\"%s\" column=\"%s\" javaType=\"%s\" jdbcType=\"%s\"/>",
                        subEntry.getValue().getName().replace(subEntry.getValue().getParent().getName() + ".", ""),
                        alias(entry.getValue().getName() + "." + subEntry.getValue().getName()),
                        subEntry.getValue().getClz().getName(),
                        subEntry.getValue().getJdbcType()
                ));
                builder.append("</association>");
            }

            builder.append("</association>");
        }
    }


    private String alias(String column) {
        return column;
    }

    private String quota(String alias) {
        return localism.openQuote() + alias + localism.closeQuote();
    }

    /**
     * is ResultMap exists.
     *
     * @param name no need namespace.
     */
    public boolean isResultMapExist(String name) {
        if (null == configuration) {
            return false;
        }
        return configuration.hasResultMap(model.getClz().getName() + "." + name);
    }

    /**
     * Is Fragment exists.
     */
    public boolean isFragmentExist(String fragment) {
        if (null == configuration) {
            return false;
        }
        return configuration.getSqlFragments().containsKey(model.getClz().getName() + "." + fragment);
    }

    /**
     * is statement exists.
     */
    public boolean isStatementExist(String id) {
        if (null == configuration) {
            return false;
        }
        return configuration.hasStatement(model.getClz().getName() + "." + id);
    }
}
