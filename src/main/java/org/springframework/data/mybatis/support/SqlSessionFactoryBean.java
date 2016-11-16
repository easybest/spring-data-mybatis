package org.springframework.data.mybatis.support;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.mybatis.scripting.beetl.BeetlDriver;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by songjiawei on 2016/11/14.
 */
public class SqlSessionFactoryBean extends org.mybatis.spring.SqlSessionFactoryBean implements ApplicationContextAware {
    private Class<?>[]         typeAliases;
    private Resource[]         mapperLocations;
    private ApplicationContext applicationContext;


    @Override
    public void afterPropertiesSet() throws Exception {

        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties p = new Properties();
        p.setProperty("SQL Server", "sqlserver");
        p.setProperty("Oracle", "oracle");
        p.setProperty("DB2", "db2");
        p.setProperty("MySQL", "mysql");
        p.setProperty("H2", "h2");
        databaseIdProvider.setProperties(p);
        setDatabaseIdProvider(databaseIdProvider);

        if (null == typeAliases) {
            typeAliases = new Class<?>[0];
        }
        Class<?>[] newTypeAliases = new Class<?>[typeAliases.length + 1];
        System.arraycopy(typeAliases, 0, newTypeAliases, 0, typeAliases.length);
        newTypeAliases[newTypeAliases.length - 1] = BeetlDriver.class;
        super.setTypeAliases(newTypeAliases);

        Set<Resource> resourceSet = new HashSet<Resource>();
        if (null != mapperLocations && mapperLocations.length > 0) {
            for (Resource r : mapperLocations) {
                resourceSet.add(r);
            }
        }
        resourceSet.add(applicationContext.getResource("classpath:org/springframework/data/mybatis/PublicMapper.xml"));
        super.setMapperLocations(resourceSet.toArray(new Resource[resourceSet.size()]));

        super.afterPropertiesSet();
    }

    @Override
    public void setMapperLocations(Resource[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    @Override
    public void setTypeAliases(Class<?>[] typeAliases) {
        this.typeAliases = typeAliases;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
