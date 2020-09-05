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
package org.springframework.data.mybatis.querydsl;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLSerializer;
import com.querydsl.sql.SchemaAndTable;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class MybatisSQLSerializer extends SQLSerializer {

	public MybatisSQLSerializer(Configuration conf) {
		super(conf);
	}

	public MybatisSQLSerializer(Configuration conf, boolean dml) {
		super(conf, dml);
	}

	@Override
	public Void visit(Path<?> path, Void context) {
		if (this.dml) {
			if (path.equals(this.entity) && path instanceof RelationalPath<?>) {
				SchemaAndTable schemaAndTable = this.getSchemaAndTable((RelationalPath<?>) path);
				boolean precededByDot;
				if (this.dmlWithSchema && this.templates.isPrintSchema()) {
					this.appendSchemaName(schemaAndTable.getSchema());
					this.append(".");
					precededByDot = true;
				}
				else {
					precededByDot = false;
				}
				this.appendTableName(schemaAndTable.getTable(), precededByDot);
				return null;
			}
			else if (this.entity.equals(path.getMetadata().getParent()) && this.skipParent) {
				this.appendAsColumnName(path, false);
				return null;
			}
		}
		final PathMetadata metadata = path.getMetadata();
		boolean precededByDot;
		if (metadata.getParent() != null && (!this.skipParent || this.dml)) {
			this.visit(metadata.getParent(), context);
			this.append(".");
			precededByDot = true;
		}
		else {
			precededByDot = false;
		}
		this.appendAsColumnName(path, precededByDot);
		return null;
	}

	protected void appendAsColumnName(Path<?> path, boolean precededByDot) {

		String column = ColumnMetadata.getName(path);
		if (path.getMetadata().getParent() instanceof RelationalPath) {
			RelationalPath<?> parent = (RelationalPath<?>) path.getMetadata().getParent();
			column = this.configuration.getColumnOverride(parent.getSchemaAndTable(), column);
		}
		this.append(this.templates.quoteIdentifier(column, precededByDot));
	}

}
