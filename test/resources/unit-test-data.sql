
CREATE TABLE buyer (
    id INT PRIMARY KEY,
    name VARCHAR(256),
    date_of_birth DATE
);

CREATE TABLE product (
    id INT PRIMARY KEY,
    sku VARCHAR(256),
    name VARCHAR(256),
    price DECIMAL(20, 2)
);

CREATE TABLE shipping_info (
    id INT PRIMARY KEY,
    buyer_id INT,
    shipping_date DATE,
    FOREIGN KEY(buyer_id) REFERENCES buyer(id)
);

CREATE TABLE purchase (
    id INT PRIMARY KEY,
    shipping_info_id INT,
    product_id INT,
    count INT,
    total DECIMAL(20, 2),
    FOREIGN KEY(shipping_info_id) REFERENCES shipping_info(id),
    FOREIGN KEY(product_id) REFERENCES product(id)
);

INSERT INTO buyer (id, name, date_of_birth) VALUES (1, 'James Bond', '2001-02-03');
INSERT INTO buyer (id, name, date_of_birth) VALUES (2, '叉烧包', '1923-11-12');
INSERT INTO buyer (id, name, date_of_birth) VALUES (3, 'Li Haoyi', '1965-08-09');

INSERT INTO product (id, sku, name, price) VALUES (1, 'face-mask', 'Face Mask', 8.88);
INSERT INTO product (id, sku, name, price) VALUES (2, 'guitar', 'Guitar', 300);
INSERT INTO product (id, sku, name, price) VALUES (3, 'socks', 'Socks', 3.14);
INSERT INTO product (id, sku, name, price) VALUES (4, 'skate-board', 'Skate Board', 123.45);
INSERT INTO product (id, sku, name, price) VALUES (5, 'camera', 'Camera', 1000.00);
INSERT INTO product (id, sku, name, price) VALUES (6, 'cookie', 'Cookie', 0.10);

INSERT INTO shipping_info (id, buyer_id, shipping_date) VALUES (1, 2, '2010-02-03');
INSERT INTO shipping_info (id, buyer_id, shipping_date) VALUES (2, 1, '2012-04-05');
INSERT INTO shipping_info (id, buyer_id, shipping_date) VALUES (3, 2, '2012-05-06');

INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (1, 1, 1, 100, 888);
INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (2, 1, 2, 3, 900);
INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (3, 1, 3, 5, 15.7);
INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (4, 2, 4, 4, 493.8);
INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (5, 2, 5, 10, 10000.00);
INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (6, 3, 1, 5, 44.4);
INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (7, 3, 6, 13, 1.30);
