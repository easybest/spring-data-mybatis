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

package org.springframework.data.mybatis.repository.config;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.mybatis.repository.localism.LocalismFactoryBean;
import org.springframework.data.mybatis.repository.support.MybatisRepository;
import org.springframework.data.mybatis.repository.support.MybatisRepositoryFactoryBean;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 * Mybatis repository configuration extension for spring data.
 *
 * @author Jarvis Song
 */
public class MybatisRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {
    private static final String DEFAULT_TRANSACTION_MANAGER_BEAN_NAME = TxUtils.DEFAULT_TRANSACTION_MANAGER;
    private static final String DEFAULT_SQL_SESSION_FACTORY_BEAN_NAME = "sqlSessionFactory";
    private static final String ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE = "enableDefaultTransactions";
    private static final String SQL_SESSION_TEMPLATE_BEAN_NAME_SUFFIX = "_Template";
    private static final String LOCALISM_BEAN_NAME_SUFFIX             = "_Localism";
    private static final String PAGINATION_INTERCEPTOR_SUFFIX         = "_PaginationInterceptor";

    @Override
    public String getModuleName() {
        return "mybatis";
    }

    @Override
    protected String getModulePrefix() {
        return getModuleName().toLowerCase(Locale.US);
    }

    @Override
    public String getRepositoryFactoryClassName() {
        return MybatisRepositoryFactoryBean.class.getName();
    }

    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Arrays.asList(Entity.class, MappedSuperclass.class);

    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.<Class<?>>singleton(MybatisRepository.class);
    }

    @Override
    public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {
        super.registerBeansForRoot(registry, config);

        Object source = config.getSource();
        String sqlSessionFactoryRef = config.getAttribute("sqlSessionFactoryRef");
        sqlSessionFactoryRef = null == sqlSessionFactoryRef ? DEFAULT_SQL_SESSION_FACTORY_BEAN_NAME : sqlSessionFactoryRef;

        // create database localism
        BeanDefinitionBuilder localismBuilder = BeanDefinitionBuilder.rootBeanDefinition(LocalismFactoryBean.class);
        localismBuilder.addPropertyReference("sqlSessionFactory", sqlSessionFactoryRef);
        registerIfNotAlreadyRegistered(localismBuilder.getBeanDefinition(), registry, sqlSessionFactoryRef.concat(LOCALISM_BEAN_NAME_SUFFIX), source);


        // create sqlSessionTemplate bean.
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(SqlSessionTemplate.class);
        builder.addConstructorArgReference(sqlSessionFactoryRef);
        registerIfNotAlreadyRegistered(builder.getBeanDefinition(), registry, sqlSessionFactoryRef.concat(SQL_SESSION_TEMPLATE_BEAN_NAME_SUFFIX), source);


    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
        String transactionManagerRef = source.getAttribute("transactionManagerRef");
        builder.addPropertyValue("transactionManager", null == transactionManagerRef ? DEFAULT_TRANSACTION_MANAGER_BEAN_NAME : transactionManagerRef);

        String sqlSessionFactoryRef = source.getAttribute("sqlSessionFactoryRef");
        sqlSessionFactoryRef = null == sqlSessionFactoryRef ? DEFAULT_SQL_SESSION_FACTORY_BEAN_NAME : sqlSessionFactoryRef;
        builder.addPropertyReference("sqlSessionTemplate", sqlSessionFactoryRef.concat(SQL_SESSION_TEMPLATE_BEAN_NAME_SUFFIX));
        builder.addPropertyReference("localism", sqlSessionFactoryRef.concat(LOCALISM_BEAN_NAME_SUFFIX));

    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {
        AnnotationAttributes attributes = config.getAttributes();
        builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE, attributes.getBoolean(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE));
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {
        String enableDefaultTransactions = config.getAttribute(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE);
        if (StringUtils.hasText(enableDefaultTransactions)) {
            builder.addPropertyValue(ENABLE_DEFAULT_TRANSACTIONS_ATTRIBUTE, enableDefaultTransactions);
        }
    }


}
