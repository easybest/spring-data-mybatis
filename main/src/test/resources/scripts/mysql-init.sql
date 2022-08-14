drop table if exists ROLE;
drop table if exists SD_User;
drop table if exists SD_User_ROLE;
drop table if exists SD_User_SD_User;
drop table if exists User_attributes;

create table ROLE
(
	id   integer not null auto_increment,
	name varchar(255),
	primary key (id)
);

create table SD_User
(
	id           integer          not null auto_increment,
	active       boolean          not null,
	city         varchar(255),
	country      varchar(255),
	streetName   varchar(255),
	streetNo     varchar(255),
	age          integer          not null,
	binaryData   blob(255),
	createdAt    datetime,
	dateOfBirth  date,
	emailAddress varchar(255)     not null,
	firstname    varchar(255) binary,
	lastname     varchar(255) binary,
	manager_id   integer,
	version      bigint default 0 not null,
	deleted      int    default 0 not null,
	DTYPE        varchar(31)      null,
	primary key (id),
	unique key (emailAddress)
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

create table User_attributes
(
	User_id    integer not null,
	attributes varchar(255)
);

