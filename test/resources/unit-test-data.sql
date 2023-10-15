
CREATE TABLE item (
    id INT PRIMARY KEY,
    order_id INT,
    product_id INT,
    quantity INT,
    total DECIMAL(20, 2),
    FOREIGN KEY(order_id) REFERENCES purchase_order(id),
    FOREIGN KEY(product_id) REFERENCES product(id)
);

CREATE TABLE purchase_order (
    id INT PRIMARY KEY,
    customer_id INT,
    order_date DATE,
    FOREIGN KEY(customer_id) REFERENCES customer(id)
);

CREATE TABLE customer (
    id INT PRIMARY KEY,
    name VARCHAR(256),
    birthdate DATE
);

CREATE TABLE product (
    id INT PRIMARY KEY,
    sku VARCHAR(256),
    name VARCHAR(256),
    price DECIMAL(20, 2)
);

INSERT INTO product (id, sku, name, price) VALUES (1, 'face-mask', 'Face Mask', 8.88);
INSERT INTO product (id, sku, name, price) VALUES (2, 'guitar', 'Guitar', 300);
INSERT INTO product (id, sku, name, price) VALUES (3, 'socks', 'Socks', 3.14);
INSERT INTO product (id, sku, name, price) VALUES (4, 'skateboard', 'Skateboard', 123.45);
INSERT INTO product (id, sku, name, price) VALUES (5, 'camera', 'Camera', 1000.00);
INSERT INTO product (id, sku, name, price) VALUES (6, 'cookie', 'Cookie', 1.00);

INSERT INTO customer (id, name, birthdate) VALUES (1, 'James Bond', '2001-02-03');
INSERT INTO customer (id, name, birthdate) VALUES (2, '叉烧包', '1923-11-12');
INSERT INTO customer (id, name, birthdate) VALUES (3, 'Li Haoyi', '1965-08-09');

INSERT INTO purchase_order (id, customer_id, order_date) VALUES (1, 2, '2010-02-03');
INSERT INTO purchase_order (id, customer_id, order_date) VALUES (2, 1, '2012-04-05');
INSERT INTO purchase_order (id, customer_id, order_date) VALUES (3, 2, '2012-05-06');

INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (1, 1, 1, 100, 888);
INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (2, 1, 2, 3, 900);
INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (3, 1, 3, 5, 15.7);
INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (4, 2, 4, 4, 493.8);
INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (5, 2, 5, 10, 10000.00);
INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (6, 3, 1, 5, 44.4);
INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (7, 3, 6, 13, 13.00);