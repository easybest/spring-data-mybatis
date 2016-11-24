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

package org.springframework.data.mybatis.config.sample;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Jarvis Song
 */
@Configuration
@EnableJpaRepositories("org.springframework.data.mybatis.repository.sample")
public class TestJPAConfig {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();

//        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:oracle:thin:@10.10.100.35:1521:testdb", "tchw2_xc", "tchw2_xc");
//        ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");

//        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:jtds:sqlserver://10.10.100.212:1433;DatabaseName=bj301", "sa", "supconit");
//        ds.setDriverClassName("net.sourceforge.jtds.jdbc.Driver");

//        DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:mysql://10.10.100.199:3306/cq_test?characterEncoding=utf8", "root", "supconit");
//        ds.setDriverClassName("com.mysql.jdbc.Driver");

//        return ds;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
//        jpaVendorAdapter.setDatabase(Database.H2);
        jpaVendorAdapter.setGenerateDdl(true);
        jpaVendorAdapter.setShowSql(true);
        return jpaVendorAdapter;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean lemfb = new LocalContainerEntityManagerFactoryBean();
        lemfb.setDataSource(dataSource());
        lemfb.setJpaVendorAdapter(jpaVendorAdapter());
        Properties properties = new Properties();
        properties.put("hibernate.format_sql", true);
//        properties.put("hibernate.use_sql_comments", true);
        lemfb.setJpaProperties(properties);
        lemfb.setPackagesToScan("org.springframework.data.mybatis.domain");
        return lemfb;
    }

}
