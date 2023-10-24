DROP TABLE IF EXISTS buyer;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS shipping_info;
DROP TABLE IF EXISTS purchase;

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
