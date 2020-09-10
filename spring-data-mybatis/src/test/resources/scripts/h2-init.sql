drop table if exists category;
drop table if exists goods;
drop table if exists shop;
drop table if exists customer_goods;
drop table if exists customer;
drop table if exists user;
drop table if exists person;
drop table if exists user;
drop table if exists person;
drop table if exists role;
drop table if exists user_role;

create table customer
(
    firstname          varchar(32)  not null,
    lastname           varchar(32)  not null,
    age                int(4)       null,
    gender             int          null,
    constellation      varchar(32)  null,
    email_address      varchar(128) null,
    binary_data        blob         null,
    version            bigint       null,
    created_by         bigint       null,
    last_modified_by   bigint       null,
    created_date       datetime     null,
    last_modified_date datetime     null,
    primary key (firstname, lastname)
);

create table category
(
    id                 bigint       not null auto_increment,
    name               varchar(128) not null,
    created_by         bigint       null,
    last_modified_by   bigint       null,
    created_date       datetime     null,
    last_modified_date datetime     null,
    primary key (id)
);
create table goods
(
    id                 bigint       not null auto_increment,
    category_id        bigint       null,
    name               varchar(128) not null,
    inventory          int(8)       null,
    brand              varchar(32)  null,
    shop_id            bigint       null,
    created_by         bigint       null,
    last_modified_by   bigint       null,
    created_date       datetime     null,
    last_modified_date datetime     null,
    primary key (id)
);
create table shop
(
    id                 bigint       not null auto_increment,
    name               varchar(128) not null,
    active             boolean      null,
    duration           int          null,
    introduce          text         null,
    email_address      varchar(128) null,
    openingTime        bigint       null,
    brand_time         datetime     null,
    country            VARCHAR(64)  NULL,
    city               VARCHAR(64)  NULL,
    street_name        VARCHAR(64)  NULL,
    street_number      VARCHAR(64)  NULL,
    created_by         bigint       null,
    last_modified_by   bigint       null,
    created_date       datetime     null,
    last_modified_date datetime     null,
    version            bigint       null,
    primary key (id)
);
create table customer_goods
(
    customer_firstname varchar(32) not null,
    customer_lastname  varchar(32) not null,
    goods_id           bigint      not null,
    primary key (customer_firstname, customer_lastname, goods_id)
);

create table user
(
    id        bigint       not null auto_increment,
    username  varchar(32)  null,
    email     varchar(128) null,
    person_id bigint       null,
    primary key (id)
);
create table person
(
    id                 bigint      not null auto_increment,
    firstname          varchar(32) null,
    lastname           varchar(32) null,
    country            VARCHAR(64) NULL,
    city               VARCHAR(64) NULL,
    street_name        VARCHAR(64) NULL,
    street_number      VARCHAR(64) NULL,
    created_by         bigint      null,
    last_modified_by   bigint      null,
    created_date       datetime    null,
    last_modified_date datetime    null,
    primary key (id)
);
create table role
(
    id   bigint      not null auto_increment,
    name varchar(64) null,
    primary key (id)
);
create table user_role
(
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id)
);
