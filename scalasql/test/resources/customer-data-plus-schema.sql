
INSERT INTO buyer (name, date_of_birth) VALUES
('James Bond', '2001-02-03'),
('叉烧包', '1923-11-12'),
('Li Haoyi', '1965-08-09');

INSERT INTO product (kebab_case_name, name, price) VALUES
('face-mask', 'Face Mask', 8.88),
('guitar', 'Guitar', 300),
('socks', 'Socks', 3.14),
('skate-board', 'Skate Board', 123.45),
('camera', 'Camera', 1000.00),
('cookie', 'Cookie', 0.10);

INSERT INTO shipping_info (buyer_id, shipping_date) VALUES
(2, '2010-02-03'),
(1, '2012-04-05'),
(2, '2012-05-06');

INSERT INTO purchase (shipping_info_id, product_id, count, total) VALUES
(1, 1, 100, 888),
(1, 2, 3, 900),
(1, 3, 5, 15.7),
(2, 4, 4, 493.8),
(2, 5, 10, 10000.00),
(3, 1, 5, 44.4),
(3, 6, 13, 1.30);

INSERT INTO otherschema.invoice (total, vendor_name) VALUES
(150.4, 'Siemens'),
(213.3, 'Samsung'),
(407.2, 'Shell');
