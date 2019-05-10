package {{metadata.packageName}};

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;

import {{metadata.domainClazzName}};

public class {{metadata.exampleClazzSimpleName}} implements Serializable {

    private static final long serialVersionUID = {{metadata.randomId}}L;

    public static final String TABLE_NAME = "{{metadata.tableName}}";

    private List<String> columns;

    private List<Integer> limit;

    private String orderByClause;

    private boolean distinct;

    private List<Criteria> oredCriteria;

    private Criteria currentCriteria;

    private Integer page;

    private Integer size;

    private static final String DESC = " DESC";

    private static final String ASC = " ASC";

    private String table = TABLE_NAME;

    {{#metadata.partitionKey}}
    private static final String partitionKey = "{{fieldName}}";

    private static final Integer shard = {{metadata.shard}};

    private static final List<String> shardTables = getShardTables();

    {{/metadata.partitionKey}}
    private {{metadata.domainClazzSimpleName}} record;

    private List<{{metadata.domainClazzSimpleName}}> records;


    public {{metadata.exampleClazzSimpleName}}() {}

    public static {{metadata.exampleClazzSimpleName}} create(){
        return new {{metadata.exampleClazzSimpleName}}();
    }

    public {{metadata.exampleClazzSimpleName}} limit(int offset, int limit) {
        this.limit = Arrays.asList(offset,limit);
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} limit(int limit) {
        this.limit = Arrays.asList(limit);
        return this;
    }


    public {{metadata.exampleClazzSimpleName}} page(int page, int size) {
        if(page<=0 ||size <=0){
            throw new RuntimeException("page or size for condition must greate 0");
        }
        this.page = page;
        this.size = size;
        this.limit = Arrays.asList((page - 1) * size, size);
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} columns(String... columns) {
        columns(Arrays.asList(columns));
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} columns(List<String> columns) {
        if(this.columns != null) {
            this.columns.addAll(columns);
        } else {
            this.columns = columns;
        }
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} ignoreColumns(String... columns) {
        ignoreColumns(Arrays.asList(columns));
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} ignoreColumns(List<String> columns) {
        if(this.columns==null || this.columns.isEmpty()){
            this.columns = allColumns();
        }
        this.columns.removeAll(columns);
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} orderByClause(String orderByClause) {
        setOrderByClause(orderByClause);
        return this;
    }

    private {{metadata.exampleClazzSimpleName}} criteria(Criteria criteria) {
        getOredCriteria().add(criteria);
        this.currentCriteria = criteria;
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} distinct(boolean distinct) {
        setDistinct(distinct);
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} asc(String... columns){
        String orderBy = String.join(",",columns) + ASC;
        orderByClause(this.orderByClause==null ? orderBy : (this.orderByClause +","+ orderBy));
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} desc(String... columns){
        String orderBy = String.join(",",columns) + DESC;
        orderByClause(this.orderByClause==null ? orderBy : (this.orderByClause +","+ orderBy));
        return this;
    }

    public {{metadata.domainClazzSimpleName}} getRecord() {
        return this.record;
    }

    public List<{{metadata.domainClazzSimpleName}}> getRecords() {
        return this.records;
    }

    public {{metadata.exampleClazzSimpleName}} record({{metadata.domainClazzSimpleName}} record) {
        this.record = record;
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} records(List<{{metadata.domainClazzSimpleName}}> records) {
        this.records = records;
        return this;
    }

    public String getTable() {
        return this.table;
    }

    public {{metadata.exampleClazzSimpleName}} table(String table) {
        this.table = table;
        return this;
    }

    private void checkCriteria(){
        if(this.currentCriteria == null){
            throw new RuntimeException("criteria for condition cannot be null");
        }
    }

    private void checkTable(){
        {{#metadata.partitionKey}}
        if(table == null){
            throw new RuntimeException("{{fieldName}} must have a value");
        }
        {{/metadata.partitionKey}}
    }


    public void setOrderByClause(String orderByClause) {
        this.orderByClause = orderByClause;
    }

    protected String getOrderByClause() {
        return orderByClause;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public List<Criteria> getOredCriteria() {
        if(this.oredCriteria == null){
            this.oredCriteria = new ArrayList<>();
        }
        return this.oredCriteria;
    }

    private {{metadata.exampleClazzSimpleName}} or(Criteria criteria) {
        getOredCriteria().add(criteria);
        this.currentCriteria = criteria;
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} or() {
        return or(createCriteriaInternal());
    }

    protected Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        List<Criteria> cs = getOredCriteria();
        if (cs.size() == 0) {
            cs.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        Criteria criteria = new Criteria();
        return criteria;
    }

    public void clear() {
        this.oredCriteria.clear();
        this.orderByClause = null;
        this.distinct = false;
        this.limit = null;
        this.columns = null;
        this.page = null;
        this.size = null;
        this.table = null;
        this.currentCriteria = null;
        this.table = null;
    }

{{#metadata.columnMetadataList}}
    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}IsNull() {
        getCriteria().and{{firstUpFieldName}}IsNull();
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}IsNotNull() {
        getCriteria().and{{firstUpFieldName}}IsNotNull();
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}EqualTo({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}EqualTo({{fieldName}});
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}NotEqualTo({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}NotEqualTo({{fieldName}});
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}In(List<{{javaType}}> {{fieldName}}) {
        {{#partitionKey}}
        if(!{{fieldName}}.isEmpty()){
            shardTable({{fieldName}}.get(0));
        }
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}In({{fieldName}});
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}NotIn(List<{{javaType}}> {{fieldName}}) {
        {{#partitionKey}}
        if(!{{fieldName}}.isEmpty()){
            shardTable({{fieldName}}.get(0));
        }
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}NotIn({{fieldName}});
        return this;
    }

    {{^stringType}}
    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}Between({{javaType}} {{fieldName}}1, {{javaType}} {{fieldName}}2) {
        {{#partitionKey}}
        shardTable({{fieldName}}1);
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}Between({{fieldName}}1, {{fieldName}}2);
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}NotBetween({{javaType}} {{fieldName}}1, {{javaType}} {{fieldName}}2) {
        {{#partitionKey}}
        shardTable({{fieldName}}1);
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}NotBetween({{fieldName}}1, {{fieldName}}2);
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}GreaterThan({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}GreaterThan({{fieldName}});
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}GreaterThanOrEqualTo({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}GreaterThanOrEqualTo({{fieldName}});
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}LessThan({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}LessThan({{fieldName}});
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}LessThanOrEqualTo({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}LessThanOrEqualTo({{fieldName}});
        return this;
    }
    {{/stringType}}

    {{#stringType}}
    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}Like({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}Like({{fieldName}});
        return this;
    }

    public {{metadata.exampleClazzSimpleName}} and{{firstUpFieldName}}NotLike({{javaType}} {{fieldName}}) {
        {{#partitionKey}}
        shardTable({{fieldName}});
        {{/partitionKey}}
        getCriteria().and{{firstUpFieldName}}NotLike({{fieldName}});
        return this;
    }
    {{/stringType}}
{{/metadata.columnMetadataList}}


    private Criteria getCriteria(){
        if(this.currentCriteria == null){
            this.currentCriteria = new Criteria();
            getOredCriteria().add(this.currentCriteria);
        }
        return this.currentCriteria;
    }

    protected abstract static class GeneratedCriteria implements Serializable {

        private static final long serialVersionUID = {{metadata.randomId}}L;

        protected List<Criterion> criteria;

        protected GeneratedCriteria() {
            super();
            criteria = new ArrayList<Criterion>();
        }

        public boolean isValid() {
            return criteria.size() > 0;
        }

        public List<Criterion> getAllCriteria() {
            return criteria;
        }

        public List<Criterion> getCriteria() {
            return criteria;
        }

        protected void addCriterion(String condition) {
            if (condition == null) {
                throw new RuntimeException("Value for condition cannot be null");
            }
            criteria.add(new Criterion(condition));
        }

        protected void addCriterion(String condition, Object value, String property) {
            if (value == null) {
                throw new RuntimeException("Value for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value));
        }

        protected void addCriterion(String condition, Object value1, Object value2, String property) {
            if (value1 == null || value2 == null) {
                throw new RuntimeException("Between values for " + property + " cannot be null");
            }
            criteria.add(new Criterion(condition, value1, value2));
        }
{{#metadata.columnMetadataList}}
        public Criteria and{{firstUpFieldName}}IsNull() {
            addCriterion("{{columnName}} is null");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}IsNotNull() {
            addCriterion("{{columnName}} is not null");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}EqualTo({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} =", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}NotEqualTo({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} <>", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}In(List<{{javaType}}> {{fieldName}}) {
            addCriterion("{{columnName}} in", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}NotIn(List<{{javaType}}> {{fieldName}}) {
            addCriterion("{{columnName}} not in", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        {{^stringType}}
        public Criteria and{{firstUpFieldName}}Between({{javaType}} {{fieldName}}1, {{javaType}} {{fieldName}}2) {
            addCriterion("{{columnName}} between", {{fieldName}}1, {{fieldName}}2, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}NotBetween({{javaType}} {{fieldName}}1, {{javaType}} {{fieldName}}2) {
            addCriterion("{{columnName}} not between", {{fieldName}}1, {{fieldName}}2, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}GreaterThan({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} >", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}GreaterThanOrEqualTo({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} >=", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}LessThan({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} <", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}LessThanOrEqualTo({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} <=", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }
        {{/stringType}}

        {{#stringType}}
        public Criteria and{{firstUpFieldName}}Like({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} like", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }

        public Criteria and{{firstUpFieldName}}NotLike({{javaType}} {{fieldName}}) {
            addCriterion("{{columnName}} not like", {{fieldName}}, "{{fieldName}}");
            return (Criteria) this;
        }
        {{/stringType}}
{{/metadata.columnMetadataList}}

    }

    public static class Criteria extends GeneratedCriteria implements Serializable {

        private static final long serialVersionUID = {{metadata.randomId}}L;


        protected Criteria() {
            super();
        }
    }

    public static class Criterion implements Serializable {

        private static final long serialVersionUID = {{metadata.randomId}}L;

        private String condition;

        private Object value;

        private Object secondValue;

        private boolean noValue;

        private boolean singleValue;

        private boolean betweenValue;

        private boolean listValue;

        private String typeHandler;

        public String getCondition() {
            return condition;
        }

        public Object getValue() {
            return value;
        }

        public Object getSecondValue() {
            return secondValue;
        }

        public boolean isNoValue() {
            return noValue;
        }

        public boolean isSingleValue() {
            return singleValue;
        }

        public boolean isBetweenValue() {
            return betweenValue;
        }

        public boolean isListValue() {
            return listValue;
        }

        public String getTypeHandler() {
            return typeHandler;
        }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            if (value instanceof List<?>) {
                this.listValue = true;
            } else {
                this.singleValue = true;
            }
        }

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }
    }


    public static interface Column {

        {{#metadata.columnMetadataList}}
        public static final String {{fieldName}} = "{{columnName}}";
        {{/metadata.columnMetadataList}}
    }

    protected static List<String> allColumns(){
        List<String> columns = new ArrayList<String>();
        {{#metadata.columnMetadataList}}
        columns.add("{{columnName}}");
        {{/metadata.columnMetadataList}}
        return columns;
    }

    {{#metadata.partitionKey}}
    protected static List<String> getShardTables(){
        List<String> tables = new ArrayList<String>();
        {{#metadata.shardTables}}
        tables.add("{{.}}");
        {{/metadata.shardTables}}
        return tables;
    }
    {{/metadata.partitionKey}}


    public List<Integer> getLimit(){
        return this.limit;
    }
}
