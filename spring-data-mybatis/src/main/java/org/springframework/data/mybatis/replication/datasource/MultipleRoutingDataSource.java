package org.springframework.data.mybatis.replication.datasource;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.core.NamedThreadLocal;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;

/**
 * Multiple routing data source.
 *
 * @author JARVIS SONG
 */
@Slf4j
public class MultipleRoutingDataSource extends AbstractRoutingDataSource {

	private static final ThreadLocal<String> currentDataSource = new NamedThreadLocal<>(
			"Current datasource key");

	private final String primary;

	public MultipleRoutingDataSource(Map<String, DataSource> dataSources,
			String primary) {
		Assert.notEmpty(dataSources, "Must has at least one datasource.");

		if (!dataSources.containsKey(primary)) {
			throw new IllegalArgumentException(
					"primary datasource: " + primary + " not exist in datasources.");
		}

		this.primary = primary;

		setTargetDataSources(dataSources.entrySet().stream().collect(Collectors.toMap(
				e -> e.getKey(), e -> new LazyConnectionDataSourceProxy(e.getValue()))));
		setDefaultTargetDataSource(dataSources.get(primary));
	}

	@Override
	protected Object determineCurrentLookupKey() {

		String key = getCurrentDataSource().orElse(primary);
		if (log.isDebugEnabled()) {
			log.debug("determine to use datasource: " + key);
		}
		return key;
	}

	public static Optional<String> getCurrentDataSource() {
		return Optional.ofNullable(currentDataSource.get());
	}

	public static void setCurrentDataSource(String key) {
		currentDataSource.set(key);
	}

	public static void clear() {
		currentDataSource.remove();
		if (log.isDebugEnabled()) {
			log.debug("clear threadlocal...");
		}
	}

}
