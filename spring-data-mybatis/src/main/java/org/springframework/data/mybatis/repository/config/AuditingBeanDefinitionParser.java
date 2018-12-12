package org.springframework.data.mybatis.repository.config;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.data.mybatis.repository.config.MybatisRepositoryConfigExtension.DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.data.config.ParsingUtils;

import org.w3c.dom.Element;

public class AuditingBeanDefinitionParser implements BeanDefinitionParser {

	static final String AUDITING_ENTITY_LISTENER_CLASS_NAME = "org.springframework.data.mybatis.domain.support.AuditingEntityListener";

	private final MybatisAuditingHandlerBeanDefinitionParser auditingHandlerParser = new MybatisAuditingHandlerBeanDefinitionParser(
			BeanDefinitionNames.MYBATIS_MAPPING_CONTEXT_BEAN_NAME);

	@Override
	public BeanDefinition parse(Element element, ParserContext parser) {
		auditingHandlerParser.parse(element, parser);

		Object source = parser.getReaderContext().extractSource(element);

		BeanDefinitionBuilder builder = rootBeanDefinition(
				AUDITING_ENTITY_LISTENER_CLASS_NAME);
		builder.addPropertyValue("auditingHandler",
				ParsingUtils.getObjectFactoryBeanDefinition(
						auditingHandlerParser.getResolvedBeanName(), source));
		builder.addPropertyReference("sqlSessionTemplate",
				DEFAULT_SQL_SESSION_TEMPLATE_BEAN_NAME);
		// builder.setScope("prototype");

		registerInfrastructureBeanWithId(builder.getRawBeanDefinition(),
				AUDITING_ENTITY_LISTENER_CLASS_NAME, parser, element);

		return null;
	}

	private void registerInfrastructureBeanWithId(AbstractBeanDefinition def, String id,
			ParserContext context, Element element) {

		def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		def.setSource(context.extractSource(element));
		context.registerBeanComponent(new BeanComponentDefinition(def, id));
	}

}
