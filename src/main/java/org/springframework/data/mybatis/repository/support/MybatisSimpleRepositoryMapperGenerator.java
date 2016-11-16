package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.mybatis.scripting.beetl.BeetlFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * generate basic mapper for simple repository automatic.
 * <p>
 * Created by songjiawei on 2016/11/14.
 */
public class MybatisSimpleRepositoryMapperGenerator {
    private transient static final Logger logger = LoggerFactory.getLogger(MybatisSimpleRepositoryMapperGenerator.class);

    private final Configuration      configuration; // mybatis's configuration
    private final MybatisEntityModel model; // entity's mapper model
    private final String             lang;  // the script language
    private final String             namespace; // namespace


    public MybatisSimpleRepositoryMapperGenerator(Configuration configuration, MybatisEntityModel model, String namespace, String lang) {
        this.configuration = configuration;
        this.model = model;
        this.namespace = namespace;
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

        context.put("model", model);
        context.put("_this", this);

        InputStream is = this.getClass().getResourceAsStream("/mybatis-auto-mapper-" + lang + ".btl");
        String script = StreamUtils.copyToString(is, Charset.forName("UTF-8"));
        String result = BeetlFacade.apply(script, context);
        return result;
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
