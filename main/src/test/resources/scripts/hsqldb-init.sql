create
	sequence SEQ_MYBATIS start
	with 1 increment by 1;

create table Account
(
	id bigint not null,
	primary key (id)
);
create table AnnotatedAuditableUser
(
	id                bigint not null,
	createAt          timestamp,
	lastModifiedAt    timestamp,
	createdBy_id      integer,
	lastModifiedBy_id integer,
	primary key (id)
);
create table AuditableRole
(
	id                bigint not null,
	createdDate       timestamp,
	lastModifiedDate  timestamp,
	name              varchar(255),
	createdBy_id      integer,
	lastModifiedBy_id integer,
	primary key (id)
);
create table AuditableUser
(
	id                integer not null,
	createdDate       timestamp,
	lastModifiedDate  timestamp,
	firstname         varchar(255),
	createdBy_id      integer,
	lastModifiedBy_id integer,
	primary key (id)
);
create table AuditableUser_AuditableRole
(
	AuditableUser_id integer not null,
	roles_id         bigint  not null,
	primary key (AuditableUser_id, roles_id)
);
create table Category
(
	id         bigint not null,
	product_id bigint,
	primary key (id)
);
create table Child
(
	id bigint not null,
	primary key (id)
);
create table ConcreteType1
(
	id         bigint not null,
	attribute1 varchar(255),
	version    bigint,
	primary key (id)
);
create table ConcreteType2
(
	id         bigint not null,
	attribute1 varchar(255),
	version    bigint,
	primary key (id)
);
create table customAbstractPersistable
(
	id bigint not null,
	primary key (id)
);
create table Customer
(
	id   bigint not null,
	name varchar(255),
	primary key (id)
);
create table EmbeddedIdExampleDepartment
(
	departmentId bigint not null,
	name         varchar(255),
	primary key (departmentId)
);
create table EmbeddedIdExampleEmployee
(
	employeeId              bigint not null,
	name                    varchar(255),
	department_departmentId bigint not null,
	primary key (department_departmentId, employeeId)
);
create table EntityWithAssignedId
(
	id binary(255) not null,
	primary key (id)
);
create table IdClassExampleDepartment
(
	departmentId bigint not null,
	name         varchar(255),
	primary key (departmentId)
);
create table IdClassExampleEmployee
(
	empId                   bigint not null,
	name                    varchar(255),
	department_departmentId bigint not null,
	primary key (department_departmentId, empId)
);
create table INVOICE_ITEMS
(
	id         bigint not null,
	invoice_id bigint not null,
	primary key (id)
);
create table INVOICES
(
	id          bigint not null,
	customer_id bigint not null,
	order_id    bigint,
	primary key (id)
);
create table Item
(
	id             INT     not null,
	manufacturerId integer not null,
	primary key (id, manufacturerId)
);
create table ItemSite
(
	site_id             integer not null,
	item_id             INT     not null,
	item_manufacturerId integer not null,
	primary key (item_id, item_manufacturerId, site_id)
);
create table MailMessage
(
	id            bigint not null,
	content       varchar(255),
	mailSender_id bigint,
	primary key (id)
);
create table MailSender
(
	id          bigint not null,
	name        varchar(255),
	mailUser_id bigint,
	primary key (id)
);
create table MailUser
(
	id   bigint not null,
	name varchar(255),
	primary key (id)
);
create table ORDERS
(
	id          bigint not null,
	customer_id bigint not null,
	primary key (id)
);
create table OrmXmlEntity
(
	id        bigint not null,
	property1 varchar(255),
	primary key (id)
);
create table Parent
(
	id bigint not null,
	primary key (id)
);
create table Parent_Child
(
	parents_id  bigint not null,
	children_id bigint not null,
	primary key (parents_id, children_id)
);
create table PersistableWithIdClass
(
	first  bigint  not null,
	second bigint  not null,
	isNew  boolean not null,
	primary key (first, second)
);
create table PrimitiveVersionProperty
(
	id        bigint not null,
	someValue varchar(255),
	version   bigint not null,
	primary key (id)
);
create table Product
(
	id bigint not null,
	primary key (id)
);
create table ROLE
(
	id   integer not null,
	name varchar(255),
	tenant_id bigint not null,
	primary key (id)
);
create table SampleEntity
(
	first  varchar(255) not null,
	second varchar(255) not null,
	primary key (first, second)
);
create table SampleWithIdClass
(
	first  bigint not null,
	second bigint not null,
	primary key (first, second)
);
create table SampleWithIdClassIncludingEntity
(
	first          bigint not null,
	second_otherId bigint not null,
	primary key (first, second_otherId)
);
create table "SampleWithIdClassIncludingEntity$OtherEntity"
(
	otherId bigint not null,
	primary key (otherId)
);
create table SampleWithPrimitiveId
(
	id bigint not null,
	primary key (id)
);
create table SampleWithTimestampVersion
(
	id      bigint not null,
	version timestamp,
	primary key (id)
);
create table SD_User
(
	id           integer          not null,
	active       boolean          not null,
	city         varchar(255),
	country      varchar(255),
	streetName   varchar(255),
	streetNo     varchar(255),
	age          integer          not null,
	binaryData   blob(255),
	createdAt    timestamp,
	dateOfBirth  date,
	emailAddress varchar(255)     not null,
	firstname    varchar(255),
	lastname     varchar(255),
	manager_id   integer,
	version      bigint default 0 not null,
	deleted      int    default 0 not null,
	DTYPE        varchar(31) null,
	primary key (id)
);
create table SD_User_ROLE
(
	User_id  integer not null,
	roles_id integer not null,
	primary key (User_id, roles_id)
);
create table SD_User_SD_User
(
	User_id       integer not null,
	colleagues_id integer not null,
	primary key (User_id, colleagues_id)
);
create table Site
(
	id integer not null,
	primary key (id)
);
create table User_attributes
(
	User_id    integer not null,
	attributes varchar(255)
);
create table VersionedUser
(
	id      bigint not null,
	version bigint,
	primary key (id)
);

alter table SD_User
	add constraint SD_User_emailAddress unique (emailAddress);

