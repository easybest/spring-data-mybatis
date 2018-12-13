package org.springframework.data.mybatis.replication.datasource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.core.NamedThreadLocal;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * replication routing datasource.
 *
 * @author JARVIS SONG
 */
@Slf4j
public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

	public static final String MASTER_KEY = "master";

	public static final String SLAVE_PREFIX = "slave_";

	private boolean onlyMaster = false;

	private int slaveSize = 0;

	private int index = 0;

	private static final ThreadLocal<Boolean> currentTransactionReadOnly = new NamedThreadLocal<>(
			"Current transaction read-only status");

	public ReplicationRoutingDataSource(DataSource master, List<DataSource> slaves) {

		Assert.notNull(master, "master datasource can not be null.");

		Map<Object, Object> targetDataSources = new HashMap<>();
		targetDataSources.put(MASTER_KEY, new LazyConnectionDataSourceProxy(master));
		if (!CollectionUtils.isEmpty(slaves)) {
			slaveSize = slaves.size();
			for (int i = 0; i < slaveSize; i++) {
				targetDataSources.put(SLAVE_PREFIX + i,
						new LazyConnectionDataSourceProxy(slaves.get(i)));
			}
		}
		else {
			this.onlyMaster = true;
		}
		setTargetDataSources(targetDataSources);
		setDefaultTargetDataSource(targetDataSources.get(MASTER_KEY));
	}

	@Override
	protected Object determineCurrentLookupKey() {
		Object key = this.doDetermineCurrentLookupKey();
		if (log.isDebugEnabled()) {
			log.debug("determine to use datasource: " + key);
		}
		return key;
	}

	protected Object doDetermineCurrentLookupKey() {
		if (onlyMaster) {
			return MASTER_KEY;
		}

		// determine by transaction

		boolean readOnly = isCurrentTransactionReadOnly();
		if (readOnly && slaveSize > 0) {
			return SLAVE_PREFIX + getSlaveIndex();
		}

		return MASTER_KEY;
	}

	private int getSlaveIndex() {
		if (index > 10000) {
			index = 0;
		}
		return index % slaveSize;
	}

	public static boolean isCurrentTransactionReadOnly() {
		return (currentTransactionReadOnly.get() != null);
	}

	public static void setCurrentTransactionReadOnly(boolean readOnly) {
		currentTransactionReadOnly.set(readOnly ? Boolean.TRUE : null);
	}

	public static void clear() {
		currentTransactionReadOnly.remove();
	}

}
