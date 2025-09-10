IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = N'shipping_info')
    ALTER TABLE shipping_info DROP CONSTRAINT IF EXISTS fk_shipping_info_buyer;
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = N'purchase')
    ALTER TABLE purchase DROP CONSTRAINT IF EXISTS fk_purchase_shipping_info;
IF EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = N'purchase')
    ALTER TABLE purchase DROP CONSTRAINT IF EXISTS fk_purchase_product;
DROP TABLE IF EXISTS buyer;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS shipping_info;
DROP TABLE IF EXISTS purchase;
DROP TABLE IF EXISTS data_types;
DROP TABLE IF EXISTS a;
DROP TABLE IF EXISTS b;
DROP TABLE IF EXISTS non_round_trip_types;
DROP TABLE IF EXISTS opt_cols;
DROP TABLE IF EXISTS nested;
DROP TABLE IF EXISTS enclosing;
DROP TABLE IF EXISTS otherschema.invoice;
DROP SCHEMA IF EXISTS otherschema;
DROP TABLE IF EXISTS bool_types;
DROP TABLE IF EXISTS [select];

CREATE TABLE buyer (
    id INT PRIMARY KEY IDENTITY(1, 1),
    name VARCHAR(256),
    date_of_birth DATE
);

CREATE TABLE product (
    id INT PRIMARY KEY IDENTITY(1, 1),
    kebab_case_name VARCHAR(256),
    name VARCHAR(256),
    price DECIMAL(20, 2)
);

CREATE TABLE shipping_info (
    id INT PRIMARY KEY IDENTITY(1, 1),
    buyer_id INT,
    shipping_date DATE,
    CONSTRAINT fk_shipping_info_buyer
        FOREIGN KEY(buyer_id) REFERENCES buyer(id)
);

CREATE TABLE purchase (
    id INT PRIMARY KEY IDENTITY(1, 1),
    shipping_info_id INT,
    product_id INT,
    count INT,
    total DECIMAL(20, 2),
    CONSTRAINT fk_purchase_shipping_info
        FOREIGN KEY(shipping_info_id) REFERENCES shipping_info(id),
    CONSTRAINT fk_purchase_product
        FOREIGN KEY(product_id) REFERENCES product(id)
);

CREATE TABLE data_types (
    my_tiny_int TINYINT,
    my_small_int SMALLINT,
    my_int INT,
    my_big_int BIGINT,
    my_double FLOAT(53),
    my_boolean BIT,
    my_local_date DATE,
    my_local_time TIME,
    my_local_date_time DATETIME2,
    my_util_date DATETIME2,
    my_instant DATETIME2,
    my_var_binary VARBINARY(256),
    my_uuid UNIQUEIDENTIFIER,
    my_enum VARCHAR(256)
--     my_offset_time TIME WITH TIME ZONE,

);

CREATE TABLE a (
    id INT,
    b_id INT
);

CREATE TABLE b (
    id INT,
    custom VARCHAR(256)
);

CREATE TABLE non_round_trip_types(
    my_zoned_date_time DATETIMEOFFSET,
    my_offset_date_time DATETIMEOFFSET
);

CREATE TABLE opt_cols(
    my_int INT,
    my_int2 INT
);

CREATE TABLE nested(
    foo_id INT,
    my_boolean BIT
);

CREATE TABLE enclosing(
    bar_id INT,
    my_string VARCHAR(256),
    foo_id INT,
    my_boolean BIT
);

IF (NOT EXISTS (SELECT * FROM sys.schemas WHERE name = 'otherschema'))
BEGIN
    EXEC ('CREATE SCHEMA otherschema')
END;

CREATE TABLE otherschema.invoice(
    id INT PRIMARY KEY IDENTITY(1, 1),
    total DECIMAL(20, 2),
    vendor_name VARCHAR(256)
);

CREATE TABLE [select](
    id INT,
    name VARCHAR(256)
);

CREATE TABLE bool_types (
    nullable BIT,
    non_nullable BIT NOT NULL,
    a INT,
    b INT,
    comment VARCHAR(256)
);
