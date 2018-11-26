package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.repository.query.MybatisQueryExecution.DeleteExecution;
import org.springframework.data.mybatis.repository.query.MybatisQueryExecution.ExistsExecution;
import org.springframework.data.repository.query.parser.PartTree;

public class PartTreeMybatisQuery extends AbstractMybatisQuery {

	private final PartTree tree;

	private MybatisQueryExecution execution;

	private final MybatisParameters parameters;

	PartTreeMybatisQuery(MybatisQueryMethod method,
			SqlSessionTemplate sqlSessionTemplate) {

		super(method, sqlSessionTemplate);

		Class<?> domainClass = method.getEntityInformation().getJavaType();
		this.parameters = method.getParameters();

		boolean recreationRequired = parameters.hasDynamicProjection()
				|| parameters.potentiallySortsDynamically();

		try {

			this.tree = new PartTree(method.getName(), domainClass);

		}
		catch (Exception o_O) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s! %s", method,
							o_O.getMessage()),
					o_O);
		}

		this.execution = createExecution();

	}

	@Override
	protected MybatisQueryExecution getExecution() {
		return this.execution;
	}

	@Override
	protected MybatisQueryExecution createExecution() {
		if (tree.isDelete()) {
			return new DeleteExecution();
		}
		else if (tree.isExistsProjection()) {
			return new ExistsExecution();
		}

		return super.createExecution();
	}

	public PartTree getTree() {
		return tree;
	}

}
