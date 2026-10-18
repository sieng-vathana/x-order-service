-- Orders can carry a loyalty points discount. Hibernate creates these enums with every value on a new
-- database but never adds values to an existing column, so LOYALTY is appended here (a metadata-only change).
ALTER TABLE order_discounts MODIFY COLUMN method ENUM('CODE', 'MANUAL', 'LOYALTY') NOT NULL;
ALTER TABLE order_item_discounts MODIFY COLUMN method ENUM('CODE', 'MANUAL', 'LOYALTY') NOT NULL;
