drop table if exists t_category;
drop table if exists t_goods;
drop table if exists t_shop;
drop table if exists t_shop_goods;
drop table if exists t_customer;

create table t_customer
(
    firstname     varchar(32)  not null,
    lastname      varchar(32)  not null,
    age           int          null,
    gender        int          null,
    constellation varchar(32)  null,
    email_address varchar(128) null,
    version       bigint       null,
    primary key (firstname, lastname)
);

create table t_category
(
    id               bigint       not null identity,
    name             varchar(128) not null,
    created_by       bigint       null,
    last_updated_by  bigint       null,
    creation_date    datetime     null,
    last_update_date datetime     null,
    primary key (id)
);
create table t_goods
(
    id               bigint       not null identity,
    category_id      bigint       null,
    name             varchar(128) not null,
    inventory        int          null,
    brand            varchar(32)  null,
    created_by       bigint       null,
    last_updated_by  bigint       null,
    creation_date    datetime     null,
    last_update_date datetime     null,
    primary key (id)
);
create table t_shop
(
    id               bigint       not null identity,
    name             varchar(128) not null,
    active           bit          null,
    duration         int          null,
    introduce        text         null,
    email_address    varchar(128) null,
    openingTime      bigint       null,
    brand_time       datetime     null,
    country          VARCHAR(64)  NULL,
    city             VARCHAR(64)  NULL,
    street_name      VARCHAR(64)  NULL,
    street_number    VARCHAR(64)  NULL,
    created_by       bigint       null,
    last_updated_by  bigint       null,
    creation_date    datetime     null,
    last_update_date datetime     null,
    version          bigint       null,
    primary key (id)
);
create table t_shop_goods
(
    shop_id  bigint not null,
    goods_id bigint not null,
    primary key (shop_id, goods_id)
);

