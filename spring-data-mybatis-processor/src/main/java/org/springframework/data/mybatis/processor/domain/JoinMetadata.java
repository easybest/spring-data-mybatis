package org.springframework.data.mybatis.processor.domain;

public class JoinMetadata {

    private String fieldName;

    private String columnName;

    private String mappedBy;

    private String fetchType = "eager";

    public String getFieldName() {
        return fieldName;
    }

    public JoinMetadata setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getColumnName() {
        return columnName;
    }

    public JoinMetadata setColumnName(String columnName) {
        this.columnName = columnName;
        return this;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public JoinMetadata setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
        return this;
    }

    public String getFetchType() {
        return fetchType;
    }

    public JoinMetadata setFetchType(String fetchType) {
        this.fetchType = fetchType;
        return this;
    }
}
