from django.db import migrations

SQL_UP = """
CREATE OR REPLACE FUNCTION check_ledger_balance() RETURNS TRIGGER AS $$
DECLARE
  total_debit BIGINT;
  total_credit BIGINT;
BEGIN
  SELECT COALESCE(SUM(amount_paise), 0) INTO total_debit FROM wallets_ledgerentry WHERE transaction_id = NEW.transaction_id AND direction = 'DEBIT';
  SELECT COALESCE(SUM(amount_paise), 0) INTO total_credit FROM wallets_ledgerentry WHERE transaction_id = NEW.transaction_id AND direction = 'CREDIT';
  IF total_debit != total_credit THEN
    RAISE EXCEPTION 'Ledger transaction % does not balance. DEBIT: %, CREDIT: %', NEW.transaction_id, total_debit, total_credit;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER ensure_transaction_balance
AFTER INSERT OR UPDATE ON wallets_ledgerentry
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION check_ledger_balance();
"""

SQL_DOWN = """
DROP TRIGGER IF EXISTS ensure_transaction_balance ON wallets_ledgerentry;
DROP FUNCTION IF EXISTS check_ledger_balance();
"""

def create_pg_trigger(apps, schema_editor):
    if schema_editor.connection.vendor == 'postgresql':
        schema_editor.execute(SQL_UP)

def drop_pg_trigger(apps, schema_editor):
    if schema_editor.connection.vendor == 'postgresql':
        schema_editor.execute(SQL_DOWN)

class Migration(migrations.Migration):

    dependencies = [
        ('wallets', '0001_initial'),
    ]

    operations = [
        migrations.RunPython(create_pg_trigger, drop_pg_trigger),
    ]
