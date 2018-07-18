package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Jarvis Song
 */
public class PartTreeMyBatisQuery extends AbstractMyBatisQuery {

	private final PartTree tree;
	private MyBatisQueryExecution execution;

	public PartTreeMyBatisQuery(SqlSessionTemplate session, MyBatisQueryMethod method) {

		super(session, method);

		try {

			this.tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());

		} catch (Exception e) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s! %s", method, e.getMessage()), e);
		}

		execution = createExecution();
	}

	@Override
	protected MyBatisQueryExecution getExecution() {
		return execution;
	}

	public PartTree getTree() {
		return tree;
	}

	@Override
	protected MyBatisQueryExecution createExecution() {
		if (tree.isDelete()) {
			return new MyBatisQueryExecution.DeleteExecution();
		}
		if (tree.isExistsProjection()) {
			return new MyBatisQueryExecution.ExistsExecution();
		}
		return super.createExecution();
	}
}
