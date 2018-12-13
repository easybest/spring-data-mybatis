package org.springframework.data.mybatis.replication.transaction;

import org.springframework.data.mybatis.replication.datasource.ReplicationRoutingDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;

public class ReplicationDataSourceTransactionManager
		extends DataSourceTransactionManager {

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {

		ReplicationRoutingDataSource
				.setCurrentTransactionReadOnly(definition.isReadOnly());

		try {
			super.doBegin(transaction, definition);
		}
		finally {
			ReplicationRoutingDataSource.clear();
		}
	}

}
