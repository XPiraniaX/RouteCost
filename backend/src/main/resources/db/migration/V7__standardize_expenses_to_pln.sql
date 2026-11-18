-- Currency values are now interpreted as PLN. Preserve all numeric amounts.
UPDATE expenses SET currency = 'PLN';

ALTER TABLE expenses DROP CONSTRAINT chk_expenses_currency;
ALTER TABLE expenses DROP COLUMN currency;
