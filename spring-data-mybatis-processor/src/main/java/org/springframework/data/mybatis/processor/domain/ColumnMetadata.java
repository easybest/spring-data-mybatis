package org.springframework.data.mybatis.processor.domain;

public class ColumnMetadata {

    private String fieldName;

    private String columnName;

    private String firstUpFieldName;

    private String jdbcType;

    private String javaType;

    private boolean primary;

    private boolean useGeneratedKeys;

    private boolean stringType = false;

    private boolean partitionKey = false;

    private String typeHandler;

    public String getFieldName() {
        return fieldName;
    }

    public ColumnMetadata setFieldName(String fieldName) {
        this.fieldName = fieldName;
        if (fieldName != null) {
            if (fieldName.length() == 1) {
                this.firstUpFieldName = fieldName.toUpperCase();
            } else {
                this.firstUpFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            }
        }
        return this;
    }

    public String getColumnName() {
        return columnName;
    }

    public ColumnMetadata setColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public String getJdbcType() {
        return jdbcType;
    }

    public ColumnMetadata setJdbcType(String jdbcType) {
        this.jdbcType = jdbcType;
        return this;
    }

    public String getJavaType() {
        return javaType;
    }

    public ColumnMetadata setJavaType(String javaType) {
        this.javaType = javaType;
        this.stringType = "java.lang.String".equals(javaType);
        return this;
    }

    public boolean isPrimary() {
        return primary;
    }

    public ColumnMetadata setPrimary(boolean primary) {
        this.primary = primary;
        return this;
    }

    public boolean isUseGeneratedKeys() {
        return useGeneratedKeys;
    }

    public ColumnMetadata setUseGeneratedKeys(boolean useGeneratedKeys) {
        this.useGeneratedKeys = useGeneratedKeys;
        return this;
    }

    public String getFirstUpFieldName() {
        return firstUpFieldName;
    }

    public ColumnMetadata setFirstUpFieldName(String firstUpFieldName) {
        this.firstUpFieldName = firstUpFieldName;
        return this;
    }


    public boolean isStringType() {
        return stringType;
    }

    public void setStringType(boolean stringType) {
        this.stringType = stringType;
    }

    public boolean isPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(boolean partitionKey) {
        this.partitionKey = partitionKey;
    }

    public String getTypeHandler() {
        return typeHandler;
    }

    public ColumnMetadata setTypeHandler(String typeHandler) {
        this.typeHandler = typeHandler;
        return this;
    }
}
