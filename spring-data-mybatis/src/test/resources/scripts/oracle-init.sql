drop table t_category;
drop table t_goods;
drop table t_shop;
drop table t_shop_goods;
drop table t_customer;
drop sequence SEQ_SPRING_DATA_MYBATIS;

create table t_customer
(
    firstname     varchar(32)  not null,
    lastname      varchar(32)  not null,
    age           number(4)    null,
    gender        number(1)    null,
    constellation varchar(32)  null,
    email_address varchar(128) null,
    binary_data   blob         null,
    version       number(19)   null,
    primary key (firstname, lastname)
);

create table t_category
(
    id               number(19)   not null,
    name             varchar(128) not null,
    created_by       number(19)   null,
    last_updated_by  number(19)   null,
    creation_date    timestamp    null,
    last_update_date timestamp    null,
    primary key (id)
);
create table t_goods
(
    id               number(19)   not null,
    category_id      number(19)   null,
    name             varchar(128) not null,
    inventory        number(8)    null,
    brand            varchar(32)  null,
    shop_id          number(19)   null,
    created_by       number(19)   null,
    last_updated_by  number(19)   null,
    creation_date    timestamp    null,
    last_update_date timestamp    null,
    primary key (id)
);
create table t_shop
(
    id               number(19)   not null,
    name             varchar(128) not null,
    active           number(1)    null,
    duration         number       null,
    introduce        clob         null,
    email_address    varchar(128) null,
    openingTime      number(19)   null,
    brand_time       timestamp    null,
    country          VARCHAR(64)  NULL,
    city             VARCHAR(64)  NULL,
    street_name      VARCHAR(64)  NULL,
    street_number    VARCHAR(64)  NULL,
    created_by       number(19)   null,
    last_updated_by  number(19)   null,
    creation_date    timestamp    null,
    last_update_date timestamp    null,
    version          number(19)   null,
    primary key (id)
);
create table t_customer_goods
(
    customer_firstname varchar(32) not null,
    customer_lastname  varchar(32) not null,
    goods_id           number(19)  not null,
    primary key (customer_firstname, customer_lastname, goods_id)
);

create sequence SEQ_SPRING_DATA_MYBATIS start with 1 increment by 1;
