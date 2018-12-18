package org.springframework.data.mybatis.autoconfigure;

import static org.springframework.data.mybatis.autoconfigure.SpringDataMybatisProperties.PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties(PREFIX)
@Data
public class SpringDataMybatisProperties {

	public static final String PREFIX = "spring.data.mybatis.repositories";

	private boolean enabled;

	private Snowflake snowflake;

	@Data
	public static class Snowflake {

		private boolean enabled = false;

		private long datacenterId;

		private long workerId;

	}

}
