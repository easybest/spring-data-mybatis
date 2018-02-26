package org.springframework.data.mybatis.repository.support;

/**
 * @author Jarvis Song
 */
public interface MyBatisMapperBuilderAssistant {

	/**
	 * Prepare MyBatis Mapper for repository before real query.
	 */
	void prepare();
}
