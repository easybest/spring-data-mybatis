create table if not exists ds_user (
  id            int(11)      not null auto_increment,
  firstname     varchar(32)  null,
  lastname      varchar(32)  null,
  age           int(3)       null,
  active        int(1)       null,
  created_at    timestamp    null,
  email_address varchar(128) null,
  manager_id    int(11)      null,
  binary_data   blob         null,
  date_of_birth date         null,
  country       varchar(64)  null,
  city          varchar(64)  null,
  street_name   varchar(64)  null,
  street_no     varchar(64)  null,
  primary key (id)
);
create table if not exists ds_role (
  id       int(11)     not null auto_increment,
  name     varchar(32) null,
  group_id int(11)     null,
  primary key (id)
);
create table if not exists ds_group (
  id   int(11)     not null auto_increment,
  name varchar(32) null,
  code varchar(32) null,
  primary key (id)
);
create table if not exists department (
  id                 int(11)     not null auto_increment,
  name               varchar(32) null,
  version            int(11)     null,
  created_date       timestamp   null,
  last_modified_date timestamp   null,
  creator            int(11)     null,
  modifier           int(11)     null,

  primary key (id)
);

create table if not exists ds_user_ds_user (
  ds_user_id    int(11) not null,
  colleagues_id int(11) not null,
  primary key (ds_user_id, colleagues_id)
);
create table if not exists ds_user_ds_role (
  ds_user_id int(11) not null,
  ds_role_id int(11) not null,
  primary key (ds_user_id, ds_role_id)
);

create table if not exists ds_booking (
  id            int(11)     not null auto_increment,
  serial_number varchar(32) null,
  amount        int(11)     null,
  user_id       int(11)     null,
  primary key (id)
);