drop table if exists ROLE;
drop table if exists SD_User;
drop table if exists SD_User_ROLE;
drop table if exists SD_User_SD_User;
drop table if exists User_attributes;

create table ROLE
(
	id   serial not null,
	name varchar(255),
	primary key (id)
);

create table SD_User
(
	id           serial           not null,
	active       boolean          not null,
	city         varchar(255),
	country      varchar(255),
	streetName   varchar(255),
	streetNo     varchar(255),
	age          integer          not null,
	binaryData   bytea,
	createdAt    timestamp,
	dateOfBirth  date,
	emailAddress varchar(255)     not null,
	firstname    varchar(255),
	lastname     varchar(255),
	manager_id   integer,
	version      bigint default 0 not null,
	deleted      int    default 0 not null,
	DTYPE        varchar(31)      null,
	primary key (id)
);
create table SD_User_ROLE
(
	User_id  serial not null,
	roles_id serial not null,
	primary key (User_id, roles_id)
);
create table SD_User_SD_User
(
	User_id       serial not null,
	colleagues_id serial not null,
	primary key (User_id, colleagues_id)
);

create table User_attributes
(
	User_id    serial not null,
	attributes varchar(255)
);

