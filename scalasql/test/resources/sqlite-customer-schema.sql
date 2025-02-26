DROP TABLE IF EXISTS buyer;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS shipping_info;
DROP TABLE IF EXISTS purchase;
DROP TABLE IF EXISTS data_types;
DROP TABLE IF EXISTS a;
DROP TABLE IF EXISTS b;
DROP TABLE IF EXISTS non_round_trip_types;
DROP TABLE IF EXISTS nested;
DROP TABLE IF EXISTS enclosing;
DROP TABLE IF EXISTS opt_cols;

CREATE TABLE buyer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(256),
    date_of_birth DATE
);

CREATE TABLE product (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    kebab_case_name VARCHAR(256),
    name VARCHAR(256),
    price DECIMAL(20, 2)
);

CREATE TABLE shipping_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    buyer_id INT,
    shipping_date DATE,
    FOREIGN KEY(buyer_id) REFERENCES buyer(id)
);

CREATE TABLE purchase (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    shipping_info_id INT,
    product_id INT,
    count INT,
    total DECIMAL(20, 2),
    FOREIGN KEY(shipping_info_id) REFERENCES shipping_info(id),
    FOREIGN KEY(product_id) REFERENCES product(id)
);

CREATE TABLE data_types (
    my_tiny_int SMALLINT,
    my_small_int SMALLINT,
    my_int INTEGER,
    my_big_int BIGINT,
    my_double DOUBLE PRECISION,
    my_boolean BOOLEAN,
    my_local_date DATE,
    my_local_time TIME,
    my_local_date_time TIMESTAMP,
    my_util_date TIMESTAMP,
    my_instant DATETIME,
    my_var_binary VARBINARY,
    my_uuid BINARY(16),
    my_enum VARCHAR(256)
--     my_offset_time TIME WITH TIME ZONE,
);

CREATE TABLE a(
    id INTEGER,
    b_id INTEGER
);

CREATE TABLE b(
    id INTEGER,
    custom VARCHAR(256)
);

CREATE TABLE non_round_trip_types(
    my_zoned_date_time TIMESTAMP,
    my_offset_date_time TIMESTAMP
);

CREATE TABLE opt_cols(
    my_int INTEGER,
    my_int2 INTEGER
);



CREATE TABLE nested(
    foo_id INTEGER,
    my_boolean BOOLEAN
);

CREATE TABLE enclosing(
    bar_id INTEGER,
    my_string VARCHAR(256),
    foo_id INTEGER,
    my_boolean BOOLEAN
);
