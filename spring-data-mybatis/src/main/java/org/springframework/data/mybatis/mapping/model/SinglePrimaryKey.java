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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;

import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class SinglePrimaryKey implements PrimaryKey {

	private final Column column;

	private final Model model;

	private final MybatisPersistentProperty property;

	private boolean generatedKeys;

	private String keySql;

	private String executeOrder;

	public SinglePrimaryKey(Model model, MybatisPersistentProperty property) {
		this.model = model;
		this.property = property;

		this.column = new Column(model, property);

		this.processGeneratedPrimaryKey();

	}

	private void processGeneratedPrimaryKey() {
		this.generatedKeys = this.property.isAnnotationPresent(GeneratedValue.class);
		if (!this.generatedKeys) {
			return;
		}

		GeneratedValue gv = this.property.findAnnotation(GeneratedValue.class);
		Dialect dialect = this.model.getMappingContext().getDialect();
		if (gv.strategy() == GenerationType.IDENTITY || (gv.strategy() == GenerationType.AUTO
				&& "identity".equals(dialect.getNativeIdentifierGeneratorStrategy()))) {
			// identity
			this.keySql = dialect.getIdentityColumnSupport().getIdentitySelectString(this.model.getTable().toString(),
					this.column.getName().getCanonicalName(), this.column.getJdbcType().TYPE_CODE);
			this.executeOrder = "AFTER";
		}
		else if (gv.strategy() == GenerationType.SEQUENCE || (gv.strategy() == GenerationType.AUTO
				&& "sequence".equals(dialect.getNativeIdentifierGeneratorStrategy()))) {
			String sequenceName = DEFAULT_SEQUENCE_NAME;
			if (StringUtils.hasText(gv.generator())) {
				// search sequence generator
				Map<String, String> sequenceGenerators = new HashMap<>();
				SequenceGenerators sequenceGeneratorsAnn = this.property
						.findPropertyOrOwnerAnnotation(SequenceGenerators.class);
				if (null != sequenceGeneratorsAnn && sequenceGeneratorsAnn.value().length > 0) {
					sequenceGenerators.putAll(Stream.of(sequenceGeneratorsAnn.value())
							.filter(sg -> StringUtils.hasText(sg.sequenceName()))
							.collect(Collectors.toMap(SequenceGenerator::name, SequenceGenerator::sequenceName)));
				}
				SequenceGenerator sg = this.property.findPropertyOrOwnerAnnotation(SequenceGenerator.class);
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

	@Override
	public boolean isComposited() {
		return false;
	}

	@Override
	public Collection<Column> getColumns() {
		return Arrays.asList(this.column);
	}

	@Override
	public Class<?> getType() {
		return this.property.getType();
	}

	@Override
	public Column findColumn(String name) {
		return name.equals(this.column.getName().getCanonicalName()) ? this.column : null;
	}

	@Override
	public Model getModel() {
		return this.model;
	}

	public boolean isGeneratedKeys() {
		return this.generatedKeys;
	}

	public String getKeySql() {
		return this.keySql;
	}

	public String getExecuteOrder() {
		return this.executeOrder;
	}

	public boolean excludeInsertId() {
		return this.generatedKeys && !"BEFORE".equals(this.executeOrder);
	}

}
