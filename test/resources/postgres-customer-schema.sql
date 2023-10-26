DROP TABLE IF EXISTS buyer CASCADE;
DROP TABLE IF EXISTS product CASCADE;
DROP TABLE IF EXISTS shipping_info CASCADE;
DROP TABLE IF EXISTS purchase CASCADE;
DROP TABLE IF EXISTS data_types CASCADE;

CREATE TABLE buyer (
    id SERIAL PRIMARY KEY,
    name VARCHAR(256),
    date_of_birth DATE
);

CREATE TABLE product (
    id SERIAL PRIMARY KEY,
    kebab_case_name VARCHAR(256),
    name VARCHAR(256),
    price DECIMAL(20, 2)
);

CREATE TABLE shipping_info (
    id SERIAL PRIMARY KEY,
    buyer_id INT,
    shipping_date DATE,
    FOREIGN KEY(buyer_id) REFERENCES buyer(id)
);

CREATE TABLE purchase (
    id SERIAL PRIMARY KEY,
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
--     my_zoned_date_time TIMESTAMP WITH TIME ZONE,
    my_instant TIMESTAMP WITH TIME ZONE
--     my_offset_time TIME WITH TIME ZONE,
--     my_offset_date_time TIMESTAMP WITH TIME ZONE
)