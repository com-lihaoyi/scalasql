DROP TABLE IF EXISTS buyer CASCADE;
DROP TABLE IF EXISTS product CASCADE;
DROP TABLE IF EXISTS shipping_info CASCADE;
DROP TABLE IF EXISTS purchase CASCADE;

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
