package org.springframework.data.mybatis.repository.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mybatis.replication.datasource.MultipleRoutingDataSource;
import org.springframework.data.mybatis.repository.DataSource;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class MultipleDataSourceAspect {

	@Before("@annotation(ds)")
	public void determineDataSource(JoinPoint point, DataSource ds) {

		MultipleRoutingDataSource.setCurrentDataSource(ds.value());

	}

	@After("@annotation(ds)")
	public void clear(DataSource ds) {
		MultipleRoutingDataSource.clear();
	}

}
