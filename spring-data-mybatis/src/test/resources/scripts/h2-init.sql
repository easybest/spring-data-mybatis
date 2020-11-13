drop table if exists employee;
drop table if exists user;
drop table if exists department;
drop table if exists company;
drop table if exists role;
drop table if exists user_role;

create table employee
(
    firstname          varchar(32)  not null,
    lastname           varchar(32)  not null,
    email_address      varchar(128) not null,
    age                int(4)       null,
    gender             int(2)       null,
    constellation      varchar(32)  null,
    binary_data        blob         null,
    country            varchar(32)  null,
    city               varchar(32)  null,
    street_name        varchar(64)  null,
    street_number      varchar(32)  null,
    version            bigint       null,
    dept_id            bigint       null,
    created_by         bigint       null,
    created_date       timestamp    null,
    last_modified_by   bigint       null,
    last_modified_date timestamp    null,
    primary key (firstname, lastname)
);

create table user
(
    id                 bigint      not null auto_increment,
    name               varchar(32) not null,
    password           varchar(64) null,
    employee_firstname varchar(32) null,
    employee_lastname  varchar(32) null,
    primary key (id)
);

create table department
(
    id          bigint      not null auto_increment,
    name        varchar(32) not null,
    company_id  bigint      null,
    superior_id bigint      null,
    primary key (id)
);

create table company
(
    name        varchar(128) not null,
    country     varchar(32)  null,
    city        varchar(32)  null,
    street_name varchar(64)  null,
    street_num  varchar(32)  null,
    primary key (name)
);

create table role
(
    id              bigint      not null auto_increment,
    name            varchar(32) not null,
    person_lastname varchar(32) not null,
    hobby_id        bigint      not null,
    primary key (id)
);

create table user_role
(
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id)
);
