-- ============================================================
-- Migration: Fix credit card balance calculation
-- Problema: movement_effect() aplica -ABS(amount) para expense
-- sin importar el tipo de cuenta. En tarjetas de crédito, el
-- balance representa lo que se debe, por lo que un gasto debe
-- SUMAR al balance (+ABS), no restar.
--
-- Efecto deseado:
--   Débito/Efectivo: expense resta del balance (dinero disponible)
--   Crédito: expense suma al balance (aumenta la deuda)
-- ============================================================

-- 1. Eliminar trigger existente
DROP TRIGGER IF EXISTS trg_movements_balance ON public.movements;

-- 2. Reemplazar función apply_movement_balance con versión
--    que consulta account_type para determinar el signo
CREATE OR REPLACE FUNCTION public.apply_movement_balance() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  source_delta numeric(18,2);
  dest_delta numeric(18,2);
  acc_type text;
BEGIN
  -- Obtener el tipo de cuenta para determinar cómo calcular el delta
  SELECT a.account_type INTO acc_type FROM public.accounts a WHERE a.id = NEW.account_id;
  IF acc_type IS NULL THEN
    acc_type := 'debit';
  END IF;

  -- Calcula el delta según movement_type y account_type
  -- Crédito: expense suma (aumenta deuda), todo lo demás igual
  -- Débito/Efectivo: expense resta (dinero que sale)

  -- Para INSERT
  IF TG_OP = 'INSERT' THEN
    IF NEW.deleted_at IS NOT NULL THEN
      RETURN NEW;
    END IF;

    source_delta := CASE
      WHEN NEW.movement_type = 'income' THEN ABS(NEW.amount)
      WHEN NEW.movement_type = 'expense' AND acc_type = 'credit' THEN ABS(NEW.amount)
      WHEN NEW.movement_type IN ('expense', 'payment', 'transfer', 'withdrawal') THEN -ABS(NEW.amount)
      WHEN NEW.movement_type = 'adjustment' THEN NEW.amount
      ELSE 0
    END;

    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = NEW.account_id;

    IF NEW.movement_type = 'transfer' THEN
      dest_delta := ABS(NEW.amount);
      UPDATE public.accounts
      SET current_balance = current_balance + dest_delta
      WHERE id = NEW.transfer_account_id;
    END IF;

    RETURN NEW;

  -- Para soft delete (revertir el efecto)
  ELSIF TG_OP = 'UPDATE' AND OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
    SELECT a.account_type INTO acc_type FROM public.accounts a WHERE a.id = OLD.account_id;
    IF acc_type IS NULL THEN acc_type := 'debit'; END IF;

    source_delta := CASE
      WHEN OLD.movement_type = 'income' THEN ABS(OLD.amount)
      WHEN OLD.movement_type = 'expense' AND acc_type = 'credit' THEN ABS(OLD.amount)
      WHEN OLD.movement_type IN ('expense', 'payment', 'transfer', 'withdrawal') THEN -ABS(OLD.amount)
      WHEN OLD.movement_type = 'adjustment' THEN OLD.amount
      ELSE 0
    END * -1;

    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = OLD.account_id;

    IF OLD.movement_type = 'transfer' THEN
      UPDATE public.accounts
      SET current_balance = current_balance - ABS(OLD.amount)
      WHERE id = OLD.transfer_account_id;
    END IF;

    RETURN NEW;

  -- Para restore (undelete)
  ELSIF TG_OP = 'UPDATE' AND OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
    SELECT a.account_type INTO acc_type FROM public.accounts a WHERE a.id = NEW.account_id;
    IF acc_type IS NULL THEN acc_type := 'debit'; END IF;

    source_delta := CASE
      WHEN NEW.movement_type = 'income' THEN ABS(NEW.amount)
      WHEN NEW.movement_type = 'expense' AND acc_type = 'credit' THEN ABS(NEW.amount)
      WHEN NEW.movement_type IN ('expense', 'payment', 'transfer', 'withdrawal') THEN -ABS(NEW.amount)
      WHEN NEW.movement_type = 'adjustment' THEN NEW.amount
      ELSE 0
    END;

    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = NEW.account_id;

    IF NEW.movement_type = 'transfer' THEN
      UPDATE public.accounts
      SET current_balance = current_balance + ABS(NEW.amount)
      WHERE id = NEW.transfer_account_id;
    END IF;

    RETURN NEW;

  -- Para UPDATE normal
  ELSIF TG_OP = 'UPDATE' THEN
    -- Revertir efecto anterior
    SELECT a.account_type INTO acc_type FROM public.accounts a WHERE a.id = OLD.account_id;
    IF acc_type IS NULL THEN acc_type := 'debit'; END IF;

    source_delta := CASE
      WHEN OLD.movement_type = 'income' THEN ABS(OLD.amount)
      WHEN OLD.movement_type = 'expense' AND acc_type = 'credit' THEN ABS(OLD.amount)
      WHEN OLD.movement_type IN ('expense', 'payment', 'transfer', 'withdrawal') THEN -ABS(OLD.amount)
      WHEN OLD.movement_type = 'adjustment' THEN OLD.amount
      ELSE 0
    END * -1;

    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = OLD.account_id;

    IF OLD.movement_type = 'transfer' THEN
      UPDATE public.accounts
      SET current_balance = current_balance - ABS(OLD.amount)
      WHERE id = OLD.transfer_account_id;
    END IF;

    -- Aplicar nuevo efecto
    SELECT a.account_type INTO acc_type FROM public.accounts a WHERE a.id = NEW.account_id;
    IF acc_type IS NULL THEN acc_type := 'debit'; END IF;

    source_delta := CASE
      WHEN NEW.movement_type = 'income' THEN ABS(NEW.amount)
      WHEN NEW.movement_type = 'expense' AND acc_type = 'credit' THEN ABS(NEW.amount)
      WHEN NEW.movement_type IN ('expense', 'payment', 'transfer', 'withdrawal') THEN -ABS(NEW.amount)
      WHEN NEW.movement_type = 'adjustment' THEN NEW.amount
      ELSE 0
    END;

    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = NEW.account_id;

    IF NEW.movement_type = 'transfer' THEN
      UPDATE public.accounts
      SET current_balance = current_balance + ABS(NEW.amount)
      WHERE id = NEW.transfer_account_id;
    END IF;

    RETURN NEW;

  -- Para DELETE real
  ELSE
    SELECT a.account_type INTO acc_type FROM public.accounts a WHERE a.id = OLD.account_id;
    IF acc_type IS NULL THEN acc_type := 'debit'; END IF;

    source_delta := CASE
      WHEN OLD.movement_type = 'income' THEN ABS(OLD.amount)
      WHEN OLD.movement_type = 'expense' AND acc_type = 'credit' THEN ABS(OLD.amount)
      WHEN OLD.movement_type IN ('expense', 'payment', 'transfer', 'withdrawal') THEN -ABS(OLD.amount)
      WHEN OLD.movement_type = 'adjustment' THEN OLD.amount
      ELSE 0
    END * -1;

    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = OLD.account_id;

    IF OLD.movement_type = 'transfer' THEN
      UPDATE public.accounts
      SET current_balance = current_balance - ABS(OLD.amount)
      WHERE id = OLD.transfer_account_id;
    END IF;

    RETURN OLD;
  END IF;
END;
$$;

-- 3. Re-crear trigger
CREATE TRIGGER trg_movements_balance AFTER INSERT OR DELETE OR UPDATE ON public.movements 
FOR EACH ROW EXECUTE FUNCTION public.apply_movement_balance();

-- 4. Opcional: Migrar datos existentes para tarjetas de crédito
--    Invierte el signo del balance en cuentas de crédito para que
--    lo debido sea positivo en vez de negativo
--    (comentar si no se desea modificar datos históricos)
UPDATE public.accounts
SET current_balance = ABS(current_balance)
WHERE account_type = 'credit' AND current_balance < 0;
