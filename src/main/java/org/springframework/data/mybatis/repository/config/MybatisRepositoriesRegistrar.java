package org.springframework.data.mybatis.repository.config;

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

import java.lang.annotation.Annotation;

/**
 * Created by songjiawei on 2016/11/9.
 */
class MybatisRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableMybatisRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new MybatisRepositoryConfigExtension();
    }

}
