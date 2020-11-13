/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.mapping.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class PrimaryKey implements Serializable {

	private static final long serialVersionUID = 6336791850441266255L;

	private static final String DEFAULT_SEQUENCE_NAME = "seq_spring_data_mybatis";

	private final Map<Identifier, Column> columns = new LinkedHashMap<>();

	private final Class<?> type;

	private final boolean generatedKeys;

	private String keySql;

	private String executeOrder;

	private boolean embeddable = false;

	public PrimaryKey(Domain domain) {
		MybatisPersistentEntity<?> entity = domain.getEntity();
		MybatisMappingContext mappingContext = domain.getMappingContext();
		if (entity.isCompositePrimaryKey()) {
			this.type = entity.getIdClass();
			this.generatedKeys = false;
			StreamUtils.createStreamFromIterator(entity.iterator()).filter(PersistentProperty::isIdProperty)
					.forEach(p -> {
						if (p.isEmbeddable()) {
							this.embeddable = true;
							MybatisPersistentEntity<?> embeddedEntity = mappingContext
									.getRequiredPersistentEntity(p.getType());
							embeddedEntity.forEach(ep -> {
								Column column = new Column(domain, ep, p.getName() + '.' + ep.getName());
								column.setPrimaryKey(true);
								this.columns.put(column.getName(), column);
							});
						}
						else {
							Column column = new Column(domain, p);
							column.setPrimaryKey(true);
							this.columns.put(column.getName(), column);
						}
					});
		}
		else {
			MybatisPersistentProperty idProperty = entity.getRequiredIdProperty();
			this.type = idProperty.getType();
			this.generatedKeys = idProperty.isAnnotationPresent(GeneratedValue.class);
			Column column = new Column(domain, idProperty);
			column.setPrimaryKey(true);
			this.columns.put(column.getName(), column);
			if (this.isGeneratedKeys()) {
				this.processGeneratedPrimaryKey(mappingContext, idProperty, domain.getTable(), column);
			}
		}
	}

	public boolean isEmbeddable() {
		return this.embeddable;
	}

	public boolean isComposited() {
		return this.columns.size() > 1;
	}

	public Class<?> getType() {
		return this.type;
	}

	public boolean isGeneratedKeys() {
		return this.generatedKeys;
	}

	private void processGeneratedPrimaryKey(MybatisMappingContext mappingContext, MybatisPersistentProperty property,
			Table table, Column column) {
		GeneratedValue gv = property.findAnnotation(GeneratedValue.class);
		if (null == gv) {
			return;
		}
		Dialect dialect = mappingContext.getDialect();
		if (gv.strategy() == GenerationType.IDENTITY || (gv.strategy() == GenerationType.AUTO
				&& "identity".equals(dialect.getNativeIdentifierGeneratorStrategy()))) {
			// identity
			this.keySql = dialect.getIdentityColumnSupport().getIdentitySelectString(table.toString(),
					column.getName().getCanonicalName(), column.getJdbcType().TYPE_CODE);
			this.executeOrder = "AFTER";
		}
		else if (gv.strategy() == GenerationType.SEQUENCE || (gv.strategy() == GenerationType.AUTO
				&& "sequence".equals(dialect.getNativeIdentifierGeneratorStrategy()))) {
			String sequenceName = DEFAULT_SEQUENCE_NAME;
			if (StringUtils.hasText(gv.generator())) {
				// search sequence generator
				Map<String, String> sequenceGenerators = new HashMap<>();
				SequenceGenerators sequenceGeneratorsAnn = property
						.findPropertyOrOwnerAnnotation(SequenceGenerators.class);
				if (null != sequenceGeneratorsAnn && sequenceGeneratorsAnn.value().length > 0) {
					sequenceGenerators.putAll(Stream.of(sequenceGeneratorsAnn.value())
							.filter(sg -> StringUtils.hasText(sg.sequenceName()))
							.collect(Collectors.toMap(SequenceGenerator::name, SequenceGenerator::sequenceName)));
				}
				SequenceGenerator sg = property.findPropertyOrOwnerAnnotation(SequenceGenerator.class);
				if (null != sg && StringUtils.hasText(sg.sequenceName())) {
					sequenceGenerators.put(sg.name(), sg.sequenceName());
				}

				String sn = sequenceGenerators.get(gv.generator());
				if (StringUtils.hasText(sn)) {
					sequenceName = sn;
				}
			}
			this.keySql = dialect.getSequenceNextValString(sequenceName);
			this.executeOrder = "BEFORE";
		}
		else {
			throw new UnsupportedOperationException("unsupported generated value id strategy: " + gv.strategy());
		}
	}

	public Map<Identifier, Column> getColumns() {
		return this.columns;
	}

	public String getKeySql() {
		return this.keySql;
	}

	public String getExecuteOrder() {
		return this.executeOrder;
	}

}
