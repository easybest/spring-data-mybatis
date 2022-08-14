drop table ROLE;
drop table SD_User;
drop table SD_User_ROLE;
drop table SD_User_SD_User;
drop table User_attributes;

create sequence SEQ_MYBATIS start with 1 increment by 1;


create table ROLE
(
	id   number(11) not null,
	name varchar(255),
	primary key (id)
);

create table SD_User
(
	id           number(11)           not null,
	active       number(1)            not null,
	city         varchar(255),
	country      varchar(255),
	streetName   varchar(255),
	streetNo     varchar(255),
	age          integer              not null,
	binaryData   blob,
	createdAt    timestamp,
	dateOfBirth  date,
	emailAddress varchar(255)         not null,
	firstname    varchar(255),
	lastname     varchar(255),
	manager_id   integer,
	version      number(19) default 0 not null,
	deleted      int        default 0 not null,
	DTYPE        varchar(31)          null,
	primary key (id)
);
create table SD_User_ROLE
(
	User_id  number(11) not null,
	roles_id number(11) not null,
	primary key (User_id, roles_id)
);
create table SD_User_SD_User
(
	User_id       number(11) not null,
	colleagues_id number(11) not null,
	primary key (User_id, colleagues_id)
);

create table User_attributes
(
	User_id    number(11) not null,
	attributes varchar(255)
);

