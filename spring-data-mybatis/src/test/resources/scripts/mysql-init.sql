DROP TABLE IF EXISTS ds_role;
DROP TABLE IF EXISTS ds_user;
DROP TABLE IF EXISTS ds_group;
DROP TABLE IF EXISTS department;
DROP TABLE IF EXISTS ds_booking;
CREATE TABLE ds_user (
  id            INT(11)      NOT NULL AUTO_INCREMENT,
  firstname     VARCHAR(32)  NULL,
  lastname      VARCHAR(32)  NULL,
  age           INT(3)       NULL,
  active        INT(1)       NULL,
  created_at    TIMESTAMP    NULL,
  email_address VARCHAR(128) NULL,
  manager_id    INT(11)      NULL,
  binary_data   BLOB         NULL,
  date_of_birth DATE         NULL,
  country       VARCHAR(64)  NULL,
  city          VARCHAR(64)  NULL,
  street_name   VARCHAR(64)  NULL,
  street_no     VARCHAR(64)  NULL,
  PRIMARY KEY (id)
);
CREATE TABLE ds_role (
  id       INT(11)     NOT NULL AUTO_INCREMENT,
  name     VARCHAR(32) NULL,
  group_id INT(11)     NULL,
  PRIMARY KEY (id)
);
CREATE TABLE ds_group (
  id   INT(11)     NOT NULL AUTO_INCREMENT,
  name VARCHAR(32) NULL,
  code VARCHAR(32) NULL,
  PRIMARY KEY (id)
);
CREATE TABLE department (
  id                 INT(11)     NOT NULL AUTO_INCREMENT,
  name               VARCHAR(32) NULL,
  version            INT(11)     NULL,
  created_date       TIMESTAMP   NULL,
  last_modified_date TIMESTAMP   NULL,
  creator            INT(11)     NULL,
  modifier           INT(11)     NULL,
  PRIMARY KEY (id)
);

CREATE TABLE ds_booking (
  id            INT(11)     NOT NULL AUTO_INCREMENT,
  serial_number VARCHAR(32) NULL,
  amount        INT(11)     NULL,
  user_id       INT(11)     NULL,
  PRIMARY KEY (id)
);