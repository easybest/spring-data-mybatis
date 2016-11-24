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
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mybatis.repository.localism.Localism;
import org.springframework.data.mybatis.repository.localism.identity.IdentityColumnSupport;

import java.io.*;
import java.util.HashMap;
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

    private final Configuration      configuration; // mybatis's configuration
    private final MybatisEntityModel model; // entity's mapper model
    private final Localism           localism;
    private final String             lang;  // the script language
    private final String             namespace; // namespace


    public MybatisSimpleRepositoryMapperGenerator(Configuration configuration, MybatisEntityModel model, Localism localism, String lang) {
        this.configuration = configuration;
        this.model = model;
        this.namespace = model.getClz().getName();
        this.localism = localism;
        this.lang = lang;
    }

    public void generate() {
        String xml;
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
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("_mybatis_auto_mapping", "true");
        context.put("lang", lang);
        context.put("namespace", namespace);
        context.put("tableName", model.getNameInDatabase());
        context.put("domainType", model.getClz().getName());
        context.put("sequence", "SEQ_" + model.getNameInDatabase()); //FIXME 如果超过30个字 截取.
        context.put("_databaseId", configuration.getDatabaseId());
        context.put("localism", localism);
        context.put("model", model);
        context.put("_this", this);


        StringBuilder builder = new StringBuilder();
        builder.append(MAPPER_BEGIN);

        builder.append("<mapper namespace=\"" + namespace + "\">");

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

        if (!isFragmentExist("SELECT_BASIC_COLUMNS")) {
            buildBasicColumns(builder);
        }
        if (!isFragmentExist("SELECT_COLUMNS")) {
            buildColumns(builder, true);
        }

        if (!isStatementExist("_insert")) {
            buildInsertSQL(builder);
        }
        if (!isStatementExist("_getById")) {
            buildGetById(builder);
        }
        if (!isStatementExist("_findAll")) {
            buildFindAll(builder);
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
        if (!isStatementExist("_countByCondition")) {
            buildCountByCondition(builder);
        }


        builder.append(MAPPER_END);
        String result = builder.toString();
//        result = BeetlFacade.apply(result, context);

        Document doc = null;
        try {
            doc = DocumentHelper.parseText(result);
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setExpandEmptyElements(true);
        format.setSuppressDeclaration(true);
        OutputStream outputStream = new ByteArrayOutputStream();
        XMLWriter writer = new XMLWriter(outputStream, format);
        writer.write(doc);
        writer.close();
        System.out.println(outputStream.toString());

        return result;
    }


    private void buildFindByPager(StringBuilder builder) {
        builder.append("<select id=\"_findByPager\" resultMap=\"ResultMap\" lang=\"XML\">");
        StringBuilder columns = new StringBuilder();
        StringBuilder condition = new StringBuilder();
        StringBuilder sorts = new StringBuilder();

        buildColumns(columns, false);

        condition.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");
        condition.append("<if test=\"condition != null\">");
        condition.append("</if>");
        condition.append("</trim>");

        sorts.append("<if test=\"_sorts != null\">");
        sorts.append("order by ");
        sorts.append("<foreach item=\"item\" index=\"idx\" collection=\"_sorts\" open=\"\" separator=\",\" close=\"\">");
        sorts.append("${item.property} ${item.direction}");
        sorts.append("</foreach>");
        sorts.append("</if>");

        builder.append(localism.getLimitHandler().processSql(true, columns.toString(), " from " + model.getNameInDatabase() + " " + quota(model.getName()) + buildLeftOuterJionSQL(), condition.toString(), sorts.toString()));

        builder.append("</select>");
    }

    private String buildLeftOuterJionSQL() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, MybatisEntityModel> entry : model.getOneToOnes().entrySet()) {
            builder.append(" left outer join ").append(entry.getValue().getNameInDatabase()).append(" ").append(quota(model.getName() + "." + entry.getKey()))
                    .append(" on ").append(quota(model.getName())).append(".").append(entry.getValue().getJoinColumnName())
                    .append("=").append(quota(model.getName() + "." + entry.getKey())).append(".").append(entry.getValue().getJoinReferencedColumnName());
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getManyToOnes().entrySet()) {
            builder.append(" left outer join ").append(entry.getValue().getNameInDatabase()).append(" ").append(quota(model.getName() + "." + entry.getKey()))
                    .append(" on ").append(quota(model.getName())).append(".").append(entry.getValue().getJoinColumnName())
                    .append("=").append(quota(model.getName() + "." + entry.getKey())).append(".").append(entry.getValue().getJoinReferencedColumnName());
        }
        return builder.toString();
    }

    private void buildDeleteAll(StringBuilder builder) {
        builder.append("<delete id=\"_deleteAll\">truncate table " + model.getNameInDatabase() + " </delete>");
    }

    private void buildDeleteById(StringBuilder builder) {
        builder.append("<delete id=\"_deleteById\" parameterType=\"" + model.getPrimaryKey().getClz().getName() + "\" lang=\"XML\">");
        builder.append("delete from ").append(model.getNameInDatabase());
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
        builder.append(model.getNameInDatabase()).append(" ").append(quota(model.getName()));


        builder.append("</select>");

    }

    private void buildCountByCondition(StringBuilder builder) {
        builder.append("<select id=\"_countByCondition\" resultType=\"long\" lang=\"XML\">");

        builder.append("select count(*) from ");
        builder.append(model.getNameInDatabase()).append(" ").append(quota(model.getName()));
        builder.append(buildLeftOuterJionSQL());

        builder.append("<if test=\"_condition != null\">");

        builder.append("</if>");

        builder.append("</select>");
    }


    private void buildFindAll(StringBuilder builder) {
        builder.append("<select id=\"_findAll\" resultMap=\"ResultMap\" lang=\"XML\">");
        builder.append(buildSelectSQL());

        builder.append("<if test=\"_condition != null\">");

        builder.append("</if>");

        builder.append("<if test=\"_sorts != null\">");
        builder.append("order by ");
        builder.append("<foreach item=\"item\" index=\"idx\" collection=\"_sorts\" open=\"\" separator=\",\" close=\"\">");
        builder.append(quota("${item.property}")).append(" ${item.direction}");
        builder.append("</foreach>");
        builder.append("</if>");

        builder.append("<if test=\"_ids != null\">");
        if (model.isCompositeId()) {
            //TODO
        } else {
            builder.append(" where ").append(quota(model.getName())).append(".").append(model.getPrimaryKey().getNameInDatabase()).append(" in ");
            builder.append("<foreach item=\"item\" index=\"index\" collection=\"_ids\" open=\"(\" separator=\",\" close=\")\">#{item}</foreach>");
        }
        builder.append("</if>");

        builder.append("</select>");
    }

    private void buildGetById(StringBuilder builder) {
        builder.append("<select id=\"_getById\" parameterType=\"" + model.getPrimaryKey().getClz().getName() + "\" resultMap=\"ResultMap\" lang=\"XML\">");
        builder.append(buildSelectSQL());

        builder.append("<trim prefix=\" where \" prefixOverrides=\"and |or \">");

        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append("and ").append(quota(model.getName())).append(".").append(entry.getValue().getNameInDatabase()).append("=");
            builder.append("#{" + entry.getValue().getName() + "}");
        }

        builder.append("</trim>");

        builder.append("</select>");
    }

    private String buildSelectSQL() {
        StringBuilder builder = new StringBuilder();
        builder.append("select <include refid=\"SELECT_COLUMNS\"/> from ");
        builder.append(model.getNameInDatabase()).append(" ").append(quota(model.getName()));

        builder.append(buildLeftOuterJionSQL());
        return builder.toString();
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


    private void buildBasicColumnsNormal(StringBuilder builder) {
        for (Map.Entry<String, MybatisEntityModel> entry : model.getPrimaryKeys().entrySet()) {
            builder.append(quota(model.getName()) + "." + entry.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getValue().getName())).append(",");
        }
        for (Map.Entry<String, MybatisEntityModel> entry : model.getColumns().entrySet()) {
            builder.append(quota(model.getName()) + "." + entry.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getValue().getName())).append(",");
        }
    }

    private void buildBasicColumns(StringBuilder builder) {
        builder.append("<sql id=\"SELECT_BASIC_COLUMNS\">");

        buildBasicColumnsNormal(builder);
        for (Map.Entry<String, MybatisEntityModel> entry : model.getJoinColumns().entrySet()) {
            builder.append(quota(model.getName()) + "." + entry.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getValue().getName())).append(",");
        }

        builder.append("</sql>");
    }

    private void buildColumnsAssociations(StringBuilder builder, Map<String, MybatisEntityModel> associations) {
        for (Map.Entry<String, MybatisEntityModel> entry : associations.entrySet()) {
            for (Map.Entry<String, MybatisEntityModel> ent : entry.getValue().getPrimaryKeys().entrySet()) {
                builder.append(quota(model.getName() + "." + entry.getKey()) + "." + ent.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getKey() + "." + ent.getValue().getName())).append(",");
            }
            for (Map.Entry<String, MybatisEntityModel> ent : entry.getValue().getColumns().entrySet()) {
                builder.append(quota(model.getName() + "." + entry.getKey()) + "." + ent.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getKey() + "." + ent.getValue().getName())).append(",");
            }
            for (Map.Entry<String, MybatisEntityModel> ent : entry.getValue().getJoinColumns().entrySet()) {
                builder.append(quota(model.getName() + "." + entry.getKey()) + "." + ent.getValue().getNameInDatabase()).append(" as ").append(quota(entry.getKey() + "." + ent.getValue().getName())).append(",");
            }
        }


    }

    private void buildColumns(StringBuilder builder, boolean containWrap) {
        if (containWrap) {
            builder.append("<sql id=\"SELECT_COLUMNS\">");
        }
        buildBasicColumnsNormal(builder);

        buildColumnsAssociations(builder, model.getOneToOnes());
        buildColumnsAssociations(builder, model.getManyToOnes());


        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }
        if (containWrap) {
            builder.append("</sql>");
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
        return configuration.hasResultMap(namespace + "." + name);
    }

    /**
     * Is Fragment exists.
     */
    public boolean isFragmentExist(String fragment) {
        if (null == configuration) {
            return false;
        }
        return configuration.getSqlFragments().containsKey(namespace + "." + fragment);
    }

    /**
     * is statement exists.
     */
    public boolean isStatementExist(String id) {
        if (null == configuration) {
            return false;
        }
        return configuration.hasStatement(namespace + "." + id);
    }
}
