package org.springframework.data.mybatis.session;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by songjiawei on 2016/11/13.
 */
public class MybatisConfiguration extends Configuration {

    private transient static final Logger logger = LoggerFactory.getLogger(MybatisConfiguration.class);

    public MybatisConfiguration(Environment environment) {
        super(environment);
    }

    public MybatisConfiguration() {
        super();
        logger.info("spring data mybatis configuration initialization succeeded...");
    }

    @Override
    public void addMappedStatement(MappedStatement ms) {
        super.addMappedStatement(ms);
    }

}
