--
-- PostgreSQL database dump
--

\restrict uehkTi1FaIyLbgIErxGDf4NqtgbUxBzdtr6BEKe3rveBvqZKLm5rsYjvOAzAFGo

-- Dumped from database version 17.6
-- Dumped by pg_dump version 18.4

-- Started on 2026-06-04 17:41:54

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 4180 (class 1262 OID 5)
-- Name: postgres; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE postgres WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = icu LOCALE = 'en_US.UTF-8' ICU_LOCALE = 'en-US';


ALTER DATABASE postgres OWNER TO postgres;

\unrestrict uehkTi1FaIyLbgIErxGDf4NqtgbUxBzdtr6BEKe3rveBvqZKLm5rsYjvOAzAFGo
\connect postgres
\restrict uehkTi1FaIyLbgIErxGDf4NqtgbUxBzdtr6BEKe3rveBvqZKLm5rsYjvOAzAFGo

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 4181 (class 0 OID 0)
-- Dependencies: 4180
-- Name: DATABASE postgres; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE postgres IS 'default administrative connection database';


--
-- TOC entry 4183 (class 0 OID 0)
-- Name: postgres; Type: DATABASE PROPERTIES; Schema: -; Owner: postgres
--

ALTER DATABASE postgres SET "app.settings.jwt_exp" TO '3600';


\unrestrict uehkTi1FaIyLbgIErxGDf4NqtgbUxBzdtr6BEKe3rveBvqZKLm5rsYjvOAzAFGo
\connect postgres
\restrict uehkTi1FaIyLbgIErxGDf4NqtgbUxBzdtr6BEKe3rveBvqZKLm5rsYjvOAzAFGo

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 37 (class 2615 OID 2200)
-- Name: public; Type: SCHEMA; Schema: -; Owner: pg_database_owner
--

CREATE SCHEMA public;


ALTER SCHEMA public OWNER TO pg_database_owner;

--
-- TOC entry 4184 (class 0 OID 0)
-- Dependencies: 37
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: pg_database_owner
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- TOC entry 1252 (class 1247 OID 17657)
-- Name: account_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.account_type AS ENUM (
    'debit',
    'credit',
    'savings',
    'cash',
    'investment',
    'loan'
);


ALTER TYPE public.account_type OWNER TO postgres;

--
-- TOC entry 1267 (class 1247 OID 17716)
-- Name: budget_period; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.budget_period AS ENUM (
    'weekly',
    'biweekly',
    'monthly',
    'custom'
);


ALTER TYPE public.budget_period OWNER TO postgres;

--
-- TOC entry 1255 (class 1247 OID 17670)
-- Name: category_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.category_type AS ENUM (
    'income',
    'expense',
    'transfer',
    'both'
);


ALTER TYPE public.category_type OWNER TO postgres;

--
-- TOC entry 1261 (class 1247 OID 17692)
-- Name: debt_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.debt_type AS ENUM (
    'loan',
    'credit_card',
    'msi',
    'financing'
);


ALTER TYPE public.debt_type OWNER TO postgres;

--
-- TOC entry 1270 (class 1247 OID 17726)
-- Name: goal_status; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.goal_status AS ENUM (
    'active',
    'paused',
    'achieved',
    'cancelled'
);


ALTER TYPE public.goal_status OWNER TO postgres;

--
-- TOC entry 1258 (class 1247 OID 17680)
-- Name: movement_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.movement_type AS ENUM (
    'income',
    'expense',
    'transfer',
    'payment',
    'adjustment',
    'withdrawal'
);


ALTER TYPE public.movement_type OWNER TO postgres;

--
-- TOC entry 1273 (class 1247 OID 17736)
-- Name: notification_type; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.notification_type AS ENUM (
    'info',
    'warning',
    'success',
    'overdue',
    'budget_alert',
    'goal_alert',
    'debt_alert'
);


ALTER TYPE public.notification_type OWNER TO postgres;

--
-- TOC entry 1264 (class 1247 OID 17702)
-- Name: payment_frequency; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.payment_frequency AS ENUM (
    'weekly',
    'biweekly',
    'monthly',
    'quarterly',
    'yearly',
    'custom'
);


ALTER TYPE public.payment_frequency OWNER TO postgres;

--
-- TOC entry 510 (class 1255 OID 17758)
-- Name: apply_movement_balance(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.apply_movement_balance() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  source_delta numeric(18,2);
  dest_delta numeric(18,2);
BEGIN
  -- Para INSERT
  IF TG_OP = 'INSERT' THEN
    IF NEW.deleted_at IS NOT NULL THEN
      RETURN NEW;
    END IF;
    
    source_delta := public.movement_effect(NEW.movement_type, NEW.amount);
    
    -- Actualizar la cuenta origen
    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = NEW.account_id;
    
    -- Si es transferencia, actualizar cuenta destino
    IF NEW.movement_type = 'transfer' THEN
      dest_delta := ABS(NEW.amount);  -- La cuenta destino recibe +
      UPDATE public.accounts
      SET current_balance = current_balance + dest_delta
      WHERE id = NEW.transfer_account_id;
    END IF;
    
    RETURN NEW;
  
  -- Para soft delete
  ELSIF TG_OP = 'UPDATE' AND OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
    -- Revertir el efecto
    source_delta := public.movement_effect(OLD.movement_type, OLD.amount) * -1;
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
    source_delta := public.movement_effect(NEW.movement_type, NEW.amount);
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
    source_delta := public.movement_effect(OLD.movement_type, OLD.amount) * -1;
    UPDATE public.accounts
    SET current_balance = current_balance + source_delta
    WHERE id = OLD.account_id;
    
    IF OLD.movement_type = 'transfer' THEN
      UPDATE public.accounts
      SET current_balance = current_balance - ABS(OLD.amount)
      WHERE id = OLD.transfer_account_id;
    END IF;
    
    -- Aplicar nuevo efecto
    source_delta := public.movement_effect(NEW.movement_type, NEW.amount);
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
    source_delta := public.movement_effect(OLD.movement_type, OLD.amount) * -1;
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


ALTER FUNCTION public.apply_movement_balance() OWNER TO postgres;

--
-- TOC entry 521 (class 1255 OID 18232)
-- Name: generate_installments_for_debt(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.generate_installments_for_debt() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
DECLARE
  v_installment_number INTEGER;
  v_installment_amount DECIMAL(18,2);
  v_due_date DATE;
BEGIN
  -- Solo para deudas con partialidades
  IF NEW.total_installments IS NOT NULL AND NEW.total_installments > 0 THEN
    v_installment_amount := NEW.original_amount / NEW.total_installments;
    v_due_date := NEW.start_date;
    
    FOR v_installment_number IN 1..NEW.total_installments LOOP
      -- Calcular fecha de vencimiento (mismo día del mes)
      v_due_date := v_due_date + INTERVAL '1 month';
      
      INSERT INTO public.installments (
        debt_id,
        user_id,
        number,
        amount,
        due_date,
        paid
      ) VALUES (
        NEW.id,
        NEW.user_id,
        v_installment_number,
        v_installment_amount,
        v_due_date,
        false
      );
    END LOOP;
  END IF;
  
  RETURN NEW;
END;
$$;


ALTER FUNCTION public.generate_installments_for_debt() OWNER TO postgres;

--
-- TOC entry 512 (class 1255 OID 17760)
-- Name: handle_new_user(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.handle_new_user() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'public'
    AS $$
declare
  v_monthly_income numeric := coalesce((new.raw_user_meta_data ->> 'monthly_income')::numeric, 0);
begin
  insert into public.profiles (
    user_id,
    full_name,
    avatar_url,
    locale,
    timezone,
    currency_code,
    settings,
    preferences
  )
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name', ''),
    new.raw_user_meta_data ->> 'avatar_url',
    coalesce(new.raw_user_meta_data ->> 'locale', 'es-MX'),
    coalesce(new.raw_user_meta_data ->> 'timezone', 'America/Mexico_City'),
    coalesce(new.raw_user_meta_data ->> 'currency_code', 'MXN'),
    jsonb_build_object(
      'monthlyIncome', v_monthly_income,
      'payCycle', coalesce(new.raw_user_meta_data ->> 'payCycle', 'monthly'),
      'payDays', coalesce(new.raw_user_meta_data ->> 'payDays', '[]')::jsonb
    ),
    '{}'::jsonb
  )
  on conflict (user_id) do nothing;

  return new;
end;
$$;


ALTER FUNCTION public.handle_new_user() OWNER TO postgres;

--
-- TOC entry 525 (class 1255 OID 18459)
-- Name: handle_new_user_cash_account(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.handle_new_user_cash_account() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
  INSERT INTO public.accounts (user_id, name, account_type, current_balance, is_active)
  VALUES (new.id, 'Efectivo', 'cash', 0, true)
  ON CONFLICT DO NOTHING;
  RETURN new;
END;
$$;


ALTER FUNCTION public.handle_new_user_cash_account() OWNER TO postgres;

--
-- TOC entry 504 (class 1255 OID 17752)
-- Name: is_owner(uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.is_owner(p_user_id uuid) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
  select auth.uid() = p_user_id;
$$;


ALTER FUNCTION public.is_owner(p_user_id uuid) OWNER TO postgres;

--
-- TOC entry 518 (class 1255 OID 18217)
-- Name: migrate_from_indexeddb(jsonb, jsonb, jsonb, jsonb, jsonb); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.migrate_from_indexeddb(p_categories jsonb, p_accounts jsonb, p_movements jsonb, p_debts jsonb, p_scheduled_payments jsonb) RETURNS jsonb
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'public'
    AS $$
declare
  v_user_id uuid := auth.uid();
  v_categories_created int := 0;
  v_accounts_created int := 0;
  v_movements_created int := 0;
  v_debts_created int := 0;
  v_scheduled_created int := 0;
  v_category_record record;
  v_account_record record;
  v_movement_record record;
  v_debt_record record;
  v_scheduled_record record;
  v_category_map jsonb := '{}'::jsonb;
  v_account_map jsonb := '{}'::jsonb;
  v_debt_map jsonb := '{}'::jsonb;
begin
  -- Migrar categorías
  for v_category_record in select * from jsonb_to_recordset(p_categories) as x(
    id text, name text, icon text, color text, type text
  )
  loop
    insert into public.categories (user_id, name, icon, color, category_type, is_system)
    values (v_user_id, v_category_record.name, v_category_record.icon, v_category_record.color, v_category_record.type::public.category_type, false)
    on conflict (user_id, category_type, name) where deleted_at is null do nothing
    returning id into v_category_map;
    v_categories_created := v_categories_created + 1;
  end loop;

  -- Migrar cuentas
  for v_account_record in select * from jsonb_to_recordset(p_accounts) as x(
    id text, name text, type text, bank_name text, current_balance numeric, credit_limit numeric,
    statement_close_day int, payment_due_day int
  )
  loop
    insert into public.accounts (
      user_id, name, account_type, bank_name, current_balance, credit_limit,
      statement_close_day, payment_due_day, is_active
    ) values (
      v_user_id, v_account_record.name, v_account_record.type::public.account_type,
      v_account_record.bank_name, coalesce(v_account_record.current_balance, 0),
      v_account_record.credit_limit, v_account_record.statement_close_day,
      v_account_record.payment_due_day, true
    )
    returning id into v_account_map;
    v_accounts_created := v_accounts_created + 1;
  end loop;

  -- Migrar deudas
  for v_debt_record in select * from jsonb_to_recordset(p_debts) as x(
    id text, name text, debt_type text, original_amount numeric, remaining_balance numeric,
    interest_rate numeric, fixed_payment numeric, minimum_payment numeric
  )
  loop
    insert into public.debts (
      user_id, name, debt_type, original_amount, remaining_balance,
      interest_rate, fixed_payment, minimum_payment
    ) values (
      v_user_id, v_debt_record.name, v_debt_record.debt_type::public.debt_type,
      v_debt_record.original_amount, v_debt_record.remaining_balance,
      coalesce(v_debt_record.interest_rate, 0), v_debt_record.fixed_payment,
      v_debt_record.minimum_payment
    )
    returning id into v_debt_map;
    v_debts_created := v_debts_created + 1;
  end loop;

  -- Migrar movimientos
  for v_movement_record in select * from jsonb_to_recordset(p_movements) as x(
    id text, account_id text, category_id text, debt_id text, movement_type text,
    amount numeric, description text, movement_date date, tags text[]
  )
  loop
    insert into public.movements (
      user_id, account_id, category_id, debt_id, movement_type, amount,
      description, movement_date, tags
    ) values (
      v_user_id,
      (v_account_map->v_movement_record.account_id)::uuid,
      (v_category_map->v_movement_record.category_id)::uuid,
      (v_debt_map->v_movement_record.debt_id)::uuid,
      v_movement_record.movement_type::public.movement_type,
      v_movement_record.amount, v_movement_record.description,
      v_movement_record.movement_date, coalesce(v_movement_record.tags, '{}')
    );
    v_movements_created := v_movements_created + 1;
  end loop;

  -- Migrar pagos programados
  for v_scheduled_record in select * from jsonb_to_recordset(p_scheduled_payments) as x(
    id text, name text, amount numeric, frequency text, next_date date, category_id text
  )
  loop
    insert into public.scheduled_payments (
      user_id, name, amount, frequency, next_date, category_id, active, mandatory
    ) values (
      v_user_id, v_scheduled_record.name, v_scheduled_record.amount,
      v_scheduled_record.frequency::public.payment_frequency,
      v_scheduled_record.next_date,
      (v_category_map->v_scheduled_record.category_id)::uuid,
      true, false
    );
    v_scheduled_created := v_scheduled_created + 1;
  end loop;

  return jsonb_build_object(
    'categories_created', v_categories_created,
    'accounts_created', v_accounts_created,
    'movements_created', v_movements_created,
    'debts_created', v_debts_created,
    'scheduled_created', v_scheduled_created
  );
end;
$$;


ALTER FUNCTION public.migrate_from_indexeddb(p_categories jsonb, p_accounts jsonb, p_movements jsonb, p_debts jsonb, p_scheduled_payments jsonb) OWNER TO postgres;

--
-- TOC entry 505 (class 1255 OID 17753)
-- Name: movement_effect(public.movement_type, numeric); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.movement_effect(p_type public.movement_type, p_amount numeric) RETURNS numeric
    LANGUAGE sql IMMUTABLE
    AS $$
  SELECT CASE
    WHEN p_type = 'income' THEN ABS(p_amount)  -- + para ingresos
    WHEN p_type = 'expense' THEN -ABS(p_amount)  -- - para gastos
    WHEN p_type = 'payment' THEN -ABS(p_amount)  -- - para pagos (también resta)
    WHEN p_type = 'transfer' THEN -ABS(p_amount)  -- - para transferencias salientes
    WHEN p_type = 'withdrawal' THEN -ABS(p_amount)  -- - para retiros de efectivo
    WHEN p_type = 'adjustment' THEN p_amount
  END;
$$;


ALTER FUNCTION public.movement_effect(p_type public.movement_type, p_amount numeric) OWNER TO postgres;

--
-- TOC entry 513 (class 1255 OID 18015)
-- Name: owns_account(uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.owns_account(p_account_id uuid) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
  select exists (
    select 1
    from public.accounts a
    where a.id = p_account_id
      and a.user_id = auth.uid()
      and a.deleted_at is null
  );
$$;


ALTER FUNCTION public.owns_account(p_account_id uuid) OWNER TO postgres;

--
-- TOC entry 514 (class 1255 OID 18016)
-- Name: owns_category(uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.owns_category(p_category_id uuid) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
  select exists (
    select 1
    from public.categories c
    where c.id = p_category_id
      and c.user_id = auth.uid()
      and c.deleted_at is null
  );
$$;


ALTER FUNCTION public.owns_category(p_category_id uuid) OWNER TO postgres;

--
-- TOC entry 515 (class 1255 OID 18017)
-- Name: owns_debt(uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.owns_debt(p_debt_id uuid) RETURNS boolean
    LANGUAGE sql STABLE
    AS $$
  select exists (
    select 1
    from public.debts d
    where d.id = p_debt_id
      and d.user_id = auth.uid()
      and d.deleted_at is null
  );
$$;


ALTER FUNCTION public.owns_debt(p_debt_id uuid) OWNER TO postgres;

--
-- TOC entry 516 (class 1255 OID 18122)
-- Name: register_debt_payment(uuid, uuid, numeric, date, text, uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.register_debt_payment(p_debt_id uuid, p_account_id uuid, p_amount numeric, p_movement_date date DEFAULT CURRENT_DATE, p_description text DEFAULT NULL::text, p_installment_id uuid DEFAULT NULL::uuid) RETURNS uuid
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'public'
    AS $$
declare
  v_user_id uuid := auth.uid();
  v_movement_id uuid;
begin
  if not public.owns_debt(p_debt_id) then
    raise exception 'debt ownership mismatch';
  end if;

  if not public.owns_account(p_account_id) then
    raise exception 'account ownership mismatch';
  end if;

  insert into public.movements (
    user_id, account_id, debt_id, movement_type, amount, description, movement_date
  )
  values (
    v_user_id, p_account_id, p_debt_id, 'payment', p_amount, p_description, p_movement_date
  )
  returning id into v_movement_id;

  update public.debts
  set remaining_balance = greatest(0, remaining_balance - p_amount)
  where id = p_debt_id;

  if p_installment_id is not null then
    update public.installments
    set paid = true,
        paid_at = now(),
        payment_movement_id = v_movement_id
    where id = p_installment_id
      and debt_id = p_debt_id;
  end if;

  return v_movement_id;
end;
$$;


ALTER FUNCTION public.register_debt_payment(p_debt_id uuid, p_account_id uuid, p_amount numeric, p_movement_date date, p_description text, p_installment_id uuid) OWNER TO postgres;

--
-- TOC entry 517 (class 1255 OID 18123)
-- Name: soft_delete_entity(text, uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.soft_delete_entity(p_table text, p_id uuid) RETURNS void
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'public'
    AS $$
begin
  if p_table = 'accounts' then
    update public.accounts set deleted_at = now() where id = p_id and user_id = auth.uid();
  elsif p_table = 'categories' then
    update public.categories set deleted_at = now() where id = p_id and user_id = auth.uid();
  elsif p_table = 'movements' then
    update public.movements set deleted_at = now() where id = p_id and user_id = auth.uid();
  elsif p_table = 'debts' then
    update public.debts set deleted_at = now() where id = p_id and user_id = auth.uid();
  elsif p_table = 'installments' then
    update public.installments i
    set deleted_at = now()
    from public.debts d
    where i.id = p_id
      and i.debt_id = d.id
      and d.user_id = auth.uid();
  elsif p_table = 'scheduled_payments' then
    update public.scheduled_payments set deleted_at = now() where id = p_id and user_id = auth.uid();
  elsif p_table = 'financial_goals' then
    update public.financial_goals set deleted_at = now() where id = p_id and user_id = auth.uid();
  elsif p_table = 'budgets' then
    update public.budgets set deleted_at = now() where id = p_id and user_id = auth.uid();
  else
    raise exception 'unsupported table for soft delete';
  end if;
end;
$$;


ALTER FUNCTION public.soft_delete_entity(p_table text, p_id uuid) OWNER TO postgres;

--
-- TOC entry 511 (class 1255 OID 17759)
-- Name: sync_installment_payment(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.sync_installment_payment() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if new.paid = true and (tg_op = 'INSERT' or old.paid is distinct from true) then
    new.paid_at = coalesce(new.paid_at, now());
  elsif new.paid = false then
    new.paid_at = null;
  end if;
  return new;
end;
$$;


ALTER FUNCTION public.sync_installment_payment() OWNER TO postgres;

--
-- TOC entry 503 (class 1255 OID 17751)
-- Name: touch_updated_at(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.touch_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  new.updated_at = now();
  if coalesce(new.row_version, 0) < 1 then
    new.row_version = 1;
  else
    new.row_version = old.row_version + 1;
  end if;
  return new;
end;
$$;


ALTER FUNCTION public.touch_updated_at() OWNER TO postgres;

--
-- TOC entry 519 (class 1255 OID 18218)
-- Name: update_debt_balance_on_payment(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_debt_balance_on_payment() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if new.debt_id is not null and new.movement_type = 'payment' then
    update public.debts
    set remaining_balance = greatest(0, remaining_balance - new.amount)
    where id = new.debt_id;
  end if;
  return new;
end;
$$;


ALTER FUNCTION public.update_debt_balance_on_payment() OWNER TO postgres;

--
-- TOC entry 522 (class 1255 OID 18234)
-- Name: update_debt_remaining_installments(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_debt_remaining_installments() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  IF NEW.paid = true AND (TG_OP = 'INSERT' OR OLD.paid = false) THEN
    UPDATE public.debts
    SET remaining_installments = remaining_installments - 1
    WHERE id = NEW.debt_id;
  ELSIF NEW.paid = false AND OLD.paid = true THEN
    UPDATE public.debts
    SET remaining_installments = remaining_installments + 1
    WHERE id = NEW.debt_id;
  END IF;
  RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_debt_remaining_installments() OWNER TO postgres;

--
-- TOC entry 520 (class 1255 OID 18220)
-- Name: update_goal_progress(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_goal_progress() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  -- No-op: progreso de metas solo se actualiza manualmente desde el frontend
  -- (el auto-increment creaba progreso ficticio sin que el usuario ahorrara realmente)
  return new;
end;
$$;


ALTER FUNCTION public.update_goal_progress() OWNER TO postgres;

--
-- TOC entry 506 (class 1255 OID 17754)
-- Name: validate_account_payload(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_account_payload() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if auth.uid() is not null and new.user_id is distinct from auth.uid() then
    raise exception 'user_id must match auth.uid()';
  end if;

  if new.account_type = 'credit' then
    if new.credit_limit is null or new.credit_limit <= 0 then
      raise exception 'credit accounts require a positive credit_limit';
    end if;
  end if;

  return new;
end;
$$;


ALTER FUNCTION public.validate_account_payload() OWNER TO postgres;

--
-- TOC entry 507 (class 1255 OID 17755)
-- Name: validate_category_payload(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_category_payload() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if auth.uid() is not null and new.user_id is distinct from auth.uid() then
    raise exception 'user_id must match auth.uid()';
  end if;
  return new;
end;
$$;


ALTER FUNCTION public.validate_category_payload() OWNER TO postgres;

--
-- TOC entry 508 (class 1255 OID 17756)
-- Name: validate_debt_payload(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_debt_payload() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
begin
  if auth.uid() is not null and new.user_id is distinct from auth.uid() then
    raise exception 'user_id must match auth.uid()';
  end if;

  if new.remaining_balance > new.original_amount then
    raise exception 'remaining_balance cannot exceed original_amount';
  end if;

  return new;
end;
$$;


ALTER FUNCTION public.validate_debt_payload() OWNER TO postgres;

--
-- TOC entry 509 (class 1255 OID 17757)
-- Name: validate_movement_payload(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.validate_movement_payload() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  -- Si es una actualización y solo está cambiando deleted_at (soft delete), omitir validaciones
  IF TG_OP = 'UPDATE' AND OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
    RETURN NEW;
  END IF;

  -- Restaurar movimiento (undelete)
  IF TG_OP = 'UPDATE' AND OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
    IF NOT public.owns_account(NEW.account_id) THEN
      RAISE EXCEPTION 'account ownership mismatch';
    END IF;
    RETURN NEW;
  END IF;

  -- Para INSERT, NO validar ownership con auth.uid()
  -- Solo verificar que la cuenta existe y está activa
  IF TG_OP = 'INSERT' THEN
    -- Verificar que la cuenta existe
    IF NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.account_id AND deleted_at IS NULL) THEN
      RAISE EXCEPTION 'account does not exist';
    END IF;
    
    -- Verificar categoría si existe
    IF NEW.category_id IS NOT NULL THEN
      IF NOT EXISTS (SELECT 1 FROM categories WHERE id = NEW.category_id AND deleted_at IS NULL) THEN
        RAISE EXCEPTION 'category does not exist';
      END IF;
    END IF;
    
    -- Verificar deuda si existe
    IF NEW.debt_id IS NOT NULL THEN
      IF NOT EXISTS (SELECT 1 FROM debts WHERE id = NEW.debt_id AND deleted_at IS NULL) THEN
        RAISE EXCEPTION 'debt does not exist';
      END IF;
    END IF;
    
    -- Validar transferencia
    IF NEW.movement_type = 'transfer' THEN
      IF NEW.transfer_account_id IS NULL THEN
        RAISE EXCEPTION 'transfer movements require transfer_account_id';
      END IF;
      IF NEW.transfer_account_id = NEW.account_id THEN
        RAISE EXCEPTION 'transfer_account_id must differ from account_id';
      END IF;
      IF NOT EXISTS (SELECT 1 FROM accounts WHERE id = NEW.transfer_account_id AND deleted_at IS NULL) THEN
        RAISE EXCEPTION 'transfer account does not exist';
      END IF;
    ELSE
      NEW.transfer_account_id = NULL;
    END IF;
    
    -- Validar monto
    IF NEW.movement_type IN ('income', 'expense', 'payment', 'transfer') AND NEW.amount <= 0 THEN
      RAISE EXCEPTION 'amount must be positive for this movement_type';
    END IF;
    
    RETURN NEW;
  END IF;

  -- Para UPDATE normal (no soft delete)
  IF NOT public.owns_account(NEW.account_id) THEN
    RAISE EXCEPTION 'account ownership mismatch';
  END IF;
  
  IF NEW.category_id IS NOT NULL AND NOT public.owns_category(NEW.category_id) THEN
    RAISE EXCEPTION 'category ownership mismatch';
  END IF;
  
  IF NEW.debt_id IS NOT NULL AND NOT public.owns_debt(NEW.debt_id) THEN
    RAISE EXCEPTION 'debt ownership mismatch';
  END IF;

  RETURN NEW;
END;
$$;


ALTER FUNCTION public.validate_movement_payload() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 342 (class 1259 OID 17781)
-- Name: accounts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.accounts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    name public.citext NOT NULL,
    account_type public.account_type NOT NULL,
    bank_name public.citext,
    current_balance numeric(18,2) DEFAULT 0 NOT NULL,
    credit_limit numeric(18,2),
    statement_close_day smallint,
    payment_due_day smallint,
    is_active boolean DEFAULT true NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    CONSTRAINT accounts_payment_due_day_check CHECK (((payment_due_day >= 1) AND (payment_due_day <= 31))),
    CONSTRAINT accounts_statement_close_day_check CHECK (((statement_close_day >= 1) AND (statement_close_day <= 31)))
);


ALTER TABLE public.accounts OWNER TO postgres;

--
-- TOC entry 351 (class 1259 OID 18004)
-- Name: audit_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.audit_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid,
    table_name text NOT NULL,
    record_id uuid,
    action text NOT NULL,
    old_data jsonb,
    new_data jsonb,
    changed_by uuid DEFAULT auth.uid(),
    changed_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT audit_log_action_check CHECK ((action = ANY (ARRAY['insert'::text, 'update'::text, 'delete'::text])))
);


ALTER TABLE public.audit_log OWNER TO postgres;

--
-- TOC entry 349 (class 1259 OID 17964)
-- Name: budgets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.budgets (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    category_id uuid NOT NULL,
    budget_period public.budget_period NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    amount_limit numeric(18,2) NOT NULL,
    alert_threshold numeric(5,4) DEFAULT 0.8000 NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    CONSTRAINT budgets_alert_threshold_check CHECK (((alert_threshold > (0)::numeric) AND (alert_threshold <= (1)::numeric))),
    CONSTRAINT budgets_amount_limit_check CHECK ((amount_limit > (0)::numeric))
);


ALTER TABLE public.budgets OWNER TO postgres;

--
-- TOC entry 343 (class 1259 OID 17802)
-- Name: categories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categories (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    name public.citext NOT NULL,
    icon character varying(255),
    color character varying(255),
    category_type public.category_type DEFAULT 'expense'::public.category_type NOT NULL,
    is_system boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL
);


ALTER TABLE public.categories OWNER TO postgres;

--
-- TOC entry 344 (class 1259 OID 17820)
-- Name: debts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.debts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    account_id uuid,
    name public.citext NOT NULL,
    debt_type public.debt_type NOT NULL,
    original_amount numeric(18,2) NOT NULL,
    remaining_balance numeric(18,2) NOT NULL,
    interest_rate numeric(7,4) DEFAULT 0 NOT NULL,
    fixed_payment numeric(18,2),
    minimum_payment numeric(18,2),
    payment_to_avoid_interest numeric(18,2),
    total_installments integer,
    remaining_installments integer,
    statement_close_day smallint,
    due_day smallint,
    start_date date DEFAULT CURRENT_DATE NOT NULL,
    end_date date,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    CONSTRAINT debts_due_day_check CHECK (((due_day >= 1) AND (due_day <= 31))),
    CONSTRAINT debts_interest_rate_check CHECK ((interest_rate >= (0)::numeric)),
    CONSTRAINT debts_original_amount_check CHECK ((original_amount > (0)::numeric)),
    CONSTRAINT debts_remaining_balance_check CHECK ((remaining_balance >= (0)::numeric)),
    CONSTRAINT debts_remaining_installments_check CHECK ((remaining_installments >= 0)),
    CONSTRAINT debts_statement_close_day_check CHECK (((statement_close_day >= 1) AND (statement_close_day <= 31))),
    CONSTRAINT debts_total_installments_check CHECK ((total_installments > 0))
);


ALTER TABLE public.debts OWNER TO postgres;

--
-- TOC entry 348 (class 1259 OID 17943)
-- Name: financial_goals; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.financial_goals (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    target_amount numeric(18,2) NOT NULL,
    current_progress numeric(18,2) DEFAULT 0 NOT NULL,
    target_date date,
    status public.goal_status DEFAULT 'active'::public.goal_status NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    CONSTRAINT financial_goals_current_progress_check CHECK ((current_progress >= (0)::numeric)),
    CONSTRAINT financial_goals_target_amount_check CHECK ((target_amount > (0)::numeric))
);


ALTER TABLE public.financial_goals OWNER TO postgres;

--
-- TOC entry 346 (class 1259 OID 17891)
-- Name: installments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.installments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    debt_id uuid NOT NULL,
    number integer NOT NULL,
    amount numeric(18,2) NOT NULL,
    due_date date NOT NULL,
    paid boolean DEFAULT false NOT NULL,
    paid_at timestamp with time zone,
    payment_movement_id uuid,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    user_id uuid NOT NULL,
    account_id uuid,
    original_purchase_amount numeric(18,2),
    interest_rate numeric(7,4) DEFAULT 0,
    CONSTRAINT installments_amount_check CHECK ((amount > (0)::numeric)),
    CONSTRAINT installments_number_check CHECK ((number > 0))
);


ALTER TABLE public.installments OWNER TO postgres;

--
-- TOC entry 345 (class 1259 OID 17851)
-- Name: movements; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.movements (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    account_id uuid NOT NULL,
    transfer_account_id uuid,
    category_id uuid,
    debt_id uuid,
    movement_type public.movement_type NOT NULL,
    amount numeric(18,2) NOT NULL,
    description character varying(255),
    movement_date date DEFAULT CURRENT_DATE NOT NULL,
    tags text[] DEFAULT '{}'::text[] NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    CONSTRAINT movements_amount_check CHECK ((((movement_type = 'adjustment'::public.movement_type) AND (amount <> (0)::numeric)) OR ((movement_type <> 'adjustment'::public.movement_type) AND (amount > (0)::numeric))))
);


ALTER TABLE public.movements OWNER TO postgres;

--
-- TOC entry 350 (class 1259 OID 17989)
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    type public.notification_type NOT NULL,
    message text NOT NULL,
    payload jsonb DEFAULT '{}'::jsonb NOT NULL,
    read_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- TOC entry 341 (class 1259 OID 17761)
-- Name: profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.profiles (
    user_id uuid NOT NULL,
    full_name public.citext NOT NULL,
    avatar_url character varying(255),
    locale text DEFAULT 'es-MX'::text NOT NULL,
    timezone text DEFAULT 'America/Mexico_City'::text NOT NULL,
    currency_code character(3) DEFAULT 'MXN'::bpchar NOT NULL,
    settings jsonb DEFAULT '{}'::jsonb NOT NULL,
    preferences jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    monthly_income numeric(38,2) DEFAULT 0
);


ALTER TABLE public.profiles OWNER TO postgres;

--
-- TOC entry 347 (class 1259 OID 17913)
-- Name: scheduled_payments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.scheduled_payments (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    account_id uuid,
    category_id uuid,
    name public.citext NOT NULL,
    amount numeric(18,2) NOT NULL,
    frequency public.payment_frequency NOT NULL,
    next_date date NOT NULL,
    last_date date,
    mandatory boolean DEFAULT false NOT NULL,
    active boolean DEFAULT true NOT NULL,
    metadata jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    deleted_at timestamp with time zone,
    row_version bigint DEFAULT 1 NOT NULL,
    payment_type text DEFAULT 'expense'::text,
    end_date date,
    CONSTRAINT scheduled_payments_amount_check CHECK ((amount > (0)::numeric)),
    CONSTRAINT scheduled_payments_payment_type_check CHECK ((payment_type = ANY (ARRAY['expense'::text, 'income'::text])))
);


ALTER TABLE public.scheduled_payments OWNER TO postgres;

--
-- TOC entry 4165 (class 0 OID 17781)
-- Dependencies: 342
-- Data for Name: accounts; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.accounts VALUES ('965a2382-6d07-4cae-8482-d5b413982c55', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'Cuenta principal', 'credit', 'BBVA', 1500.50, 25000.00, NULL, NULL, true, '{"currency": "MXN"}', '2026-06-01 21:18:19.766872+00', '2026-06-01 21:18:49.465723+00', '2026-06-01 21:18:49.465723+00', 2);
INSERT INTO public.accounts VALUES ('79181fb5-25bb-4c96-9806-d30960e9f3fb', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'banamex', 'cash', 'banamex', 6000.00, NULL, NULL, NULL, true, '{"currency": "MXN"}', '2026-06-01 21:18:40.407911+00', '2026-06-02 05:15:17.934999+00', '2026-06-02 01:10:04.919319+00', 7);
INSERT INTO public.accounts VALUES ('9d6dfe90-686b-4c28-b554-ab07c37fc540', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'Credito', 'credit', 'Citibanamex', 0.00, 25000.00, 9, 29, true, '{"currency": "MXN"}', '2026-06-02 02:11:09.095162+00', '2026-06-04 08:48:52.98467+00', NULL, 30);
INSERT INTO public.accounts VALUES ('2f6e6ab9-0902-4858-b642-8b8e5b868243', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'debito', 'debit', 'Citibanamex', 0.00, NULL, NULL, NULL, true, '{"currency": "MXN"}', '2026-06-02 05:21:46.30033+00', '2026-06-04 08:49:00.060626+00', NULL, 45);
INSERT INTO public.accounts VALUES ('f6ba01c6-ea0c-492f-9e50-41b1419afab9', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'Efectivo', 'cash', 'efectivo', 0.00, NULL, NULL, NULL, true, '{"currency": "MXN"}', '2026-06-03 06:33:22.938721+00', '2026-06-04 08:49:06.944871+00', NULL, 50);


--
-- TOC entry 4174 (class 0 OID 18004)
-- Dependencies: 351
-- Data for Name: audit_log; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- TOC entry 4172 (class 0 OID 17964)
-- Dependencies: 349
-- Data for Name: budgets; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.budgets VALUES ('c5ae1b5a-0acb-4ceb-8a6e-37b1a975942e', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '84e73d07-de7f-4565-8f04-876883a6939c', 'monthly', '2026-06-01', '2026-06-30', 2000.00, 0.0080, '{}', '2026-06-01 23:30:20.556603+00', '2026-06-02 01:10:36.451752+00', '2026-06-02 01:10:36.451752+00', 2);
INSERT INTO public.budgets VALUES ('42d11671-ff6e-4f46-8cb3-6c680f9835e8', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '84e73d07-de7f-4565-8f04-876883a6939c', 'monthly', '2026-06-01', '2026-06-30', 2000.00, 0.0080, '{}', '2026-06-02 05:20:48.305667+00', '2026-06-04 08:50:20.846526+00', '2026-06-04 08:50:20.846526+00', 2);


--
-- TOC entry 4166 (class 0 OID 17802)
-- Dependencies: 343
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.categories VALUES ('8828e0df-d0f2-47df-bca8-0bbd3ebcae57', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'alimentos', '', '#6ee7b7', 'expense', false, '2026-06-01 09:18:22.487697+00', '2026-06-01 22:55:30.398359+00', '2026-06-01 22:55:30.398359+00', 2);
INSERT INTO public.categories VALUES ('84e73d07-de7f-4565-8f04-876883a6939c', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'Comida', '', '#6ee7b7', 'expense', false, '2026-06-01 23:00:48.714249+00', '2026-06-01 23:00:48.714249+00', NULL, 1);
INSERT INTO public.categories VALUES ('c36a3978-8c0e-4f0e-a006-44a123bbeb0e', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'Quincena', '💶', '#6ee7b7', 'income', false, '2026-06-01 23:01:31.71926+00', '2026-06-01 23:01:31.71926+00', NULL, 1);
INSERT INTO public.categories VALUES ('ce7f500e-de30-47a4-b4a8-928041ae227f', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'salidas', '🍷', '#140b0b', 'income', false, '2026-06-03 00:16:49.569215+00', '2026-06-03 00:16:49.569215+00', NULL, 1);


--
-- TOC entry 4167 (class 0 OID 17820)
-- Dependencies: 344
-- Data for Name: debts; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.debts VALUES ('4e3de667-1328-4413-9ae2-81ad1dd5cc33', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, 'prestamo', 'loan', 15000.00, 13500.00, 0.0000, 1500.00, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-15', NULL, '{"notes": "prestamo", "frequency": "monthly", "nextDueDate": "2026-06-15"}', '2026-06-02 05:19:08.398224+00', '2026-06-02 21:34:32.684597+00', '2026-06-02 05:46:15.238479+00', 9);
INSERT INTO public.debts VALUES ('450819e2-78d1-4719-8483-992db9f93e9c', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, 'prestamo', 'loan', 15000.00, 10000.00, 0.0000, 1500.00, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-01', NULL, '{"notes": "prestamo", "frequency": "monthly", "nextDueDate": "2026-06-01"}', '2026-06-01 21:40:22.44922+00', '2026-06-02 01:10:26.239736+00', '2026-06-02 01:10:26.239736+00', 17);
INSERT INTO public.debts VALUES ('e7d9c70d-3d22-4d54-b913-db4900b57822', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, 'boleto', 'loan', 1000.00, 1000.00, 0.0000, 333.33, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-02', NULL, '{"notes": "Compra a meses con tarjeta de crédito", "frequency": "monthly", "nextDueDate": "2026-06-02"}', '2026-06-02 09:02:21.338205+00', '2026-06-02 09:15:00.458476+00', '2026-06-02 09:15:00.458476+00', 2);
INSERT INTO public.debts VALUES ('abc69cef-d490-47e0-996e-483d2edff023', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, 'prueba', 'loan', 5000.00, 5000.00, 0.0000, 416.67, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-16', NULL, '{"notes": "Compra a meses con tarjeta de crédito", "frequency": "monthly", "nextDueDate": "2026-06-16"}', '2026-06-02 23:39:33.110883+00', '2026-06-03 07:20:17.040747+00', '2026-06-03 07:20:17.040747+00', 2);
INSERT INTO public.debts VALUES ('b5362a47-3925-4134-a049-4ca6aba55ea5', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, 'boleto', 'loan', 5000.00, 1666.66, 0.0000, 1666.67, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-16', NULL, '{"notes": "Compra a meses con tarjeta de crédito", "frequency": "monthly", "nextDueDate": "2026-06-16"}', '2026-06-03 07:20:49.161697+00', '2026-06-04 08:49:14.593332+00', '2026-06-04 08:49:14.593332+00', 6);
INSERT INTO public.debts VALUES ('d26b00f7-68bc-4c5a-a1b0-d895fa212e50', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, 'prestamo', 'loan', 15000.00, 13500.00, 0.0000, 1500.00, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-15', NULL, '{"notes": "prestamo", "frequency": "monthly", "nextDueDate": "2026-06-15"}', '2026-06-02 05:46:33.834649+00', '2026-06-04 08:49:16.428454+00', '2026-06-04 08:49:16.428454+00', 8);
INSERT INTO public.debts VALUES ('6b15532b-4bf1-48b1-ba87-ecec30ac3bc9', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, 'tarjeta', 'loan', 500.00, 0.00, 0.0000, 500.00, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-30', NULL, '{"notes": "tarjeta", "frequency": "monthly", "nextDueDate": "2026-06-30"}', '2026-06-02 07:42:28.582657+00', '2026-06-04 08:49:18.771259+00', '2026-06-04 08:49:18.771259+00', 4);


--
-- TOC entry 4171 (class 0 OID 17943)
-- Dependencies: 348
-- Data for Name: financial_goals; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.financial_goals VALUES ('f335df57-311c-4996-942e-1219c5dc9ff1', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'casa', 10000.00, 0.00, '2026-01-12', 'active', '{}', '2026-06-02 00:19:10.816916+00', '2026-06-02 01:09:07.375342+00', '2026-06-02 01:09:07.375342+00', 2);
INSERT INTO public.financial_goals VALUES ('b56ea2cd-37dd-4525-87f1-bed22390fc42', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'casa', 10000.00, 0.00, '2026-01-12', 'active', '{}', '2026-06-02 00:19:10.818072+00', '2026-06-02 01:09:09.919406+00', '2026-06-02 01:09:09.919406+00', 2);
INSERT INTO public.financial_goals VALUES ('fc978ab3-eb44-4289-8883-f8af326a637f', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'casa', 10000.00, 0.00, '2026-01-12', 'active', '{}', '2026-06-02 00:19:10.894016+00', '2026-06-02 01:09:11.969213+00', '2026-06-02 01:09:11.969213+00', 2);
INSERT INTO public.financial_goals VALUES ('bf623f13-0582-49a2-98e5-dc0df4bd3f1a', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'viaje', 20000.00, 10000.00, '2026-12-31', 'active', '{}', '2026-06-01 23:29:53.384569+00', '2026-06-02 01:09:41.427859+00', '2026-06-02 01:09:41.427859+00', 3);
INSERT INTO public.financial_goals VALUES ('94e5a50a-119e-4102-b2a6-dc32fff9ae36', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'Casa', 10000.00, 2000.00, '2026-06-30', 'active', '{}', '2026-06-02 01:09:24.788466+00', '2026-06-02 01:09:43.117619+00', '2026-06-02 01:09:43.117619+00', 3);


--
-- TOC entry 4169 (class 0 OID 17891)
-- Dependencies: 346
-- Data for Name: installments; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.installments VALUES ('4c33b53e-b53c-4ca5-b326-63c3bb26a2fa', '450819e2-78d1-4719-8483-992db9f93e9c', 1, 2000.00, '2026-05-31', true, '2026-06-01 23:48:21.247826+00', NULL, '2026-06-01 23:48:21.247826+00', '2026-06-02 00:00:55.355172+00', '2026-06-02 00:00:55.355172+00', 4, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('f82eb0b5-dc97-450a-98cd-669fc9903b6f', 'b5362a47-3925-4134-a049-4ca6aba55ea5', 4, 1666.67, '2026-06-04', false, NULL, NULL, '2026-06-04 04:23:53.584118+00', '2026-06-04 08:49:22.343913+00', '2026-06-04 08:49:22.343913+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('88299361-52c2-4644-a069-e0df1d6ceb81', 'b5362a47-3925-4134-a049-4ca6aba55ea5', 5, 1666.67, '2026-08-16', false, NULL, NULL, '2026-06-03 07:20:49.161697+00', '2026-06-04 08:49:26.042088+00', '2026-06-04 08:49:26.042088+00', 4, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('b2c1a756-ff42-40e5-a29e-dcd9b57c6aac', 'd26b00f7-68bc-4c5a-a1b0-d895fa212e50', 1, 1500.00, '2026-06-02', true, '2026-06-02 22:00:32.798166+00', NULL, '2026-06-02 22:00:29.831481+00', '2026-06-04 08:49:42.137491+00', '2026-06-04 08:49:42.137491+00', 3, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('3c93eda0-c447-4659-846e-8fac68586ce4', '6b15532b-4bf1-48b1-ba87-ecec30ac3bc9', 1, 500.00, '2026-06-02', true, '2026-06-02 10:32:08.920294+00', NULL, '2026-06-02 10:31:06.603357+00', '2026-06-04 08:49:44.987019+00', '2026-06-04 08:49:44.987019+00', 4, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('9e783729-2245-4e4e-9641-1bfc13cb2eb1', 'd26b00f7-68bc-4c5a-a1b0-d895fa212e50', 5, 1500.00, '2026-06-03', true, '2026-06-03 09:21:03.097727+00', NULL, '2026-06-03 08:31:00.245986+00', '2026-06-04 08:49:47.003626+00', '2026-06-04 08:49:47.003626+00', 3, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('a2360ae6-c8ed-4b81-9821-98bc9e2dd731', 'b5362a47-3925-4134-a049-4ca6aba55ea5', 1, 1666.67, '2026-06-16', true, '2026-06-04 00:07:14.234005+00', NULL, '2026-06-03 07:20:49.161697+00', '2026-06-04 08:50:13.564392+00', '2026-06-04 08:50:13.564392+00', 3, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('524c5bc6-4232-47ce-ade9-c8e48bf818aa', 'b5362a47-3925-4134-a049-4ca6aba55ea5', 2, 1666.67, '2026-07-16', true, '2026-06-04 04:21:23.321455+00', NULL, '2026-06-03 07:20:49.161697+00', '2026-06-04 08:50:16.244608+00', '2026-06-04 08:50:16.244608+00', 3, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('8aa0ccbf-fbb7-4c8d-b61e-de96bf7b0fc5', '450819e2-78d1-4719-8483-992db9f93e9c', 1, 1500.00, '2026-06-02', true, '2026-06-02 00:01:05.23317+00', NULL, '2026-06-02 00:01:05.23317+00', '2026-06-02 00:28:07.311391+00', '2026-06-02 00:28:07.311391+00', 5, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('388ffa4a-786a-41bc-8ffb-7011afa90c16', '450819e2-78d1-4719-8483-992db9f93e9c', 2, 2000.00, '2026-06-02', true, '2026-06-02 00:28:17.657716+00', NULL, '2026-06-02 00:17:04.550201+00', '2026-06-02 01:09:46.919867+00', '2026-06-02 01:09:46.919867+00', 6, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('c554c750-77a6-471e-82e1-ea98a526fe20', '450819e2-78d1-4719-8483-992db9f93e9c', 3, 1500.00, '2026-06-02', true, '2026-06-02 00:48:26.038188+00', NULL, '2026-06-02 00:17:43.165131+00', '2026-06-02 01:09:48.416389+00', '2026-06-02 01:09:48.416389+00', 4, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('c6145501-2f3a-48ca-a0e0-2e0edfc5103e', '450819e2-78d1-4719-8483-992db9f93e9c', 4, 1500.00, '2026-06-02', true, '2026-06-02 01:07:01.665654+00', NULL, '2026-06-02 01:06:33.102406+00', '2026-06-02 01:09:49.678684+00', '2026-06-02 01:09:49.678684+00', 4, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('3b655ff8-5331-4bea-ac43-d8fb48fa4212', 'e7d9c70d-3d22-4d54-b913-db4900b57822', 1, 333.33, '2026-06-02', false, NULL, NULL, '2026-06-02 09:02:21.338205+00', '2026-06-02 09:15:07.164127+00', '2026-06-02 09:15:07.164127+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 1000.00, 0.0000);
INSERT INTO public.installments VALUES ('1431ce49-a10a-4e04-87e7-5a2e69ad49cc', 'e7d9c70d-3d22-4d54-b913-db4900b57822', 2, 333.33, '2026-07-02', false, NULL, NULL, '2026-06-02 09:02:21.338205+00', '2026-06-02 09:15:09.62268+00', '2026-06-02 09:15:09.62268+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 1000.00, 0.0000);
INSERT INTO public.installments VALUES ('5a14b5b8-607e-4af7-a4a7-500ad6c8c307', 'e7d9c70d-3d22-4d54-b913-db4900b57822', 3, 333.33, '2026-08-02', false, NULL, NULL, '2026-06-02 09:02:21.338205+00', '2026-06-02 09:15:11.724172+00', '2026-06-02 09:15:11.724172+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 1000.00, 0.0000);
INSERT INTO public.installments VALUES ('fbe3e086-a899-453a-9009-5431c6565889', 'd26b00f7-68bc-4c5a-a1b0-d895fa212e50', 1, 1500.00, '2026-06-02', true, '2026-06-02 22:00:02.816949+00', NULL, '2026-06-02 22:00:02.816949+00', '2026-06-02 22:00:13.931277+00', '2026-06-02 22:00:13.931277+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, NULL, 0.0000);
INSERT INTO public.installments VALUES ('12d8cfa7-97fc-469f-8ac3-9afc327fb32d', 'abc69cef-d490-47e0-996e-483d2edff023', 1, 416.67, '2026-06-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:40:53.752018+00', '2026-06-02 23:40:53.752018+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('6d6b431b-df60-4303-9094-07a717afdc66', 'abc69cef-d490-47e0-996e-483d2edff023', 2, 416.67, '2026-07-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:28.230553+00', '2026-06-02 23:42:28.230553+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('a1f134d9-9805-47a5-9ebc-74d15f42b94b', 'abc69cef-d490-47e0-996e-483d2edff023', 3, 416.67, '2026-08-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:30.122295+00', '2026-06-02 23:42:30.122295+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('2b29c2f0-e14e-452a-9a4f-12d4499ffd7a', 'abc69cef-d490-47e0-996e-483d2edff023', 4, 416.67, '2026-09-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:32.143838+00', '2026-06-02 23:42:32.143838+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('573d8b56-fad4-4705-acc8-bdef58be677a', 'abc69cef-d490-47e0-996e-483d2edff023', 5, 416.67, '2026-10-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:34.001052+00', '2026-06-02 23:42:34.001052+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('c8084a1f-7bb9-4583-a33d-9dd724f0d03f', 'abc69cef-d490-47e0-996e-483d2edff023', 6, 416.67, '2026-11-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:35.931407+00', '2026-06-02 23:42:35.931407+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('67371058-2af1-4352-b057-b3c84c26cde9', 'abc69cef-d490-47e0-996e-483d2edff023', 7, 416.67, '2026-12-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:38.041136+00', '2026-06-02 23:42:38.041136+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('77178c2a-f8ac-479c-98f7-e6047f4c6258', 'abc69cef-d490-47e0-996e-483d2edff023', 8, 416.67, '2027-01-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:39.846773+00', '2026-06-02 23:42:39.846773+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('fe4808c2-fe5e-4ae0-a520-5b71695a7720', 'abc69cef-d490-47e0-996e-483d2edff023', 9, 416.67, '2027-02-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:41.83359+00', '2026-06-02 23:42:41.83359+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('e5ef2052-28f0-4474-b0fa-1b91ba692876', 'abc69cef-d490-47e0-996e-483d2edff023', 10, 416.67, '2027-03-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:44.345204+00', '2026-06-02 23:42:44.345204+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('65bb0908-d676-4a5c-bc93-13c14642e684', 'abc69cef-d490-47e0-996e-483d2edff023', 11, 416.67, '2027-04-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-02 23:42:48.797277+00', '2026-06-02 23:42:48.797277+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);
INSERT INTO public.installments VALUES ('68c8b5cb-708b-4cf9-b6a4-2c0e48ca6bf3', 'abc69cef-d490-47e0-996e-483d2edff023', 12, 416.67, '2027-05-16', false, NULL, NULL, '2026-06-02 23:39:33.110883+00', '2026-06-03 00:01:49.564326+00', '2026-06-03 00:01:49.564326+00', 2, 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', 5000.00, 0.0000);


--
-- TOC entry 4168 (class 0 OID 17851)
-- Dependencies: 345
-- Data for Name: movements; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.movements VALUES ('cb21c84b-0128-4c2a-b7db-25918b919790', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'income', 500.00, 'deposito', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 05:14:20.933124+00', '2026-06-02 05:15:09.554307+00', '2026-06-02 05:15:09.554307+00', 2);
INSERT INTO public.movements VALUES ('2fe83d31-3317-4a85-b1d9-25d47a48002f', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, '84e73d07-de7f-4565-8f04-876883a6939c', NULL, 'expense', 1000.00, 'Prueba', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 02:11:36.232041+00', '2026-06-02 05:15:12.369761+00', '2026-06-02 05:15:12.369761+00', 2);
INSERT INTO public.movements VALUES ('36bb2013-f82c-4ba1-9acd-ce5fa2bfb445', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, NULL, NULL, 'income', 1000.00, 'Deposito cred', '2026-06-01', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 02:13:38.474213+00', '2026-06-02 05:15:14.218821+00', '2026-06-02 05:15:14.218821+00', 2);
INSERT INTO public.movements VALUES ('928d5f94-fae4-4f28-9cf3-a252f09b5fdd', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '79181fb5-25bb-4c96-9806-d30960e9f3fb', NULL, '84e73d07-de7f-4565-8f04-876883a6939c', NULL, 'expense', 300.00, 'kfc', '2026-06-01', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-01 23:30:39.89614+00', '2026-06-02 05:15:16.301138+00', '2026-06-02 05:15:16.301138+00', 2);
INSERT INTO public.movements VALUES ('262f99db-61c2-4aff-b5c1-988f3a67aa98', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '79181fb5-25bb-4c96-9806-d30960e9f3fb', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'expense', 500.00, 'prueba', '2026-06-01', '{}', '{"notes": "prueba", "currency": "MXN"}', '2026-06-01 23:29:09.356371+00', '2026-06-02 05:15:17.934999+00', '2026-06-02 05:15:17.934999+00', 2);
INSERT INTO public.movements VALUES ('c09f20d4-8a3d-4d0f-8a5f-a9cff54af28c', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'income', 1000.00, 'pago', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 05:27:54.334525+00', '2026-06-02 05:42:11.173223+00', '2026-06-02 05:42:11.173223+00', 2);
INSERT INTO public.movements VALUES ('9f4fbb71-ff45-46f7-87e3-910a710251ea', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'expense', 500.00, 'comida', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 05:21:54.666424+00', '2026-06-02 05:42:13.418264+00', '2026-06-02 05:42:13.418264+00', 2);
INSERT INTO public.movements VALUES ('9731a8b5-923b-4dab-b419-c21d16ecf1bf', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'payment', 333.33, 'pago', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 09:03:20.160284+00', '2026-06-02 09:14:31.63615+00', '2026-06-02 09:14:31.63615+00', 2);
INSERT INTO public.movements VALUES ('a5a42381-0430-4b55-975e-8eaa7c311367', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'income', 500.00, 'prueba', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 10:14:22.673627+00', '2026-06-02 10:16:51.004291+00', '2026-06-02 10:16:51.004291+00', 2);
INSERT INTO public.movements VALUES ('1e6a991c-39e2-4ed7-b956-97879cbfeb27', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'payment', 1000.00, 'pago', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 05:45:52.057748+00', '2026-06-02 10:17:11.193992+00', '2026-06-02 10:17:11.193992+00', 2);
INSERT INTO public.movements VALUES ('b8be1b6a-10f0-4a01-be4e-761894b12c8c', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, NULL, NULL, 'payment', 1666.67, 'Pago recibido - Partialidad', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:21:23.321455+00', '2026-06-04 08:47:23.268115+00', '2026-06-04 08:47:23.268115+00', 2);
INSERT INTO public.movements VALUES ('2230c12d-741c-467b-991d-dec597c32821', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #2 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:21:22.990771+00', '2026-06-04 08:47:25.765784+00', '2026-06-04 08:47:25.765784+00', 2);
INSERT INTO public.movements VALUES ('723e72a8-f9b1-49db-be0b-2d518ee7bb00', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'expense', 1666.67, 'Pago de partialidad', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 00:07:14.234005+00', '2026-06-04 08:47:27.877061+00', '2026-06-04 08:47:27.877061+00', 2);
INSERT INTO public.movements VALUES ('cbf5e5bd-bb5d-4414-a2b2-d29496119047', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, NULL, NULL, 'payment', 1666.67, 'Pago recibido - Partialidad', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 00:07:14.234005+00', '2026-06-04 08:47:29.909358+00', '2026-06-04 08:47:29.909358+00', 2);
INSERT INTO public.movements VALUES ('5098ee41-3667-4458-9fd6-ef7f730eda4b', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #1 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 00:07:13.843636+00', '2026-06-04 08:47:31.743796+00', '2026-06-04 08:47:31.743796+00', 2);
INSERT INTO public.movements VALUES ('1414cb7d-f696-4dd8-bde8-44ba9e2fdcb7', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'expense', 1500.00, 'Pago de partialidad', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 09:21:03.097727+00', '2026-06-04 08:47:33.610594+00', '2026-06-04 08:47:33.610594+00', 2);
INSERT INTO public.movements VALUES ('3272b9c1-999b-436e-85a9-6b04d2e1f821', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1500.00, 'Pago de partialidad #5 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 09:21:02.834472+00', '2026-06-04 08:47:35.428169+00', '2026-06-04 08:47:35.428169+00', 2);
INSERT INTO public.movements VALUES ('a2478e90-3467-4cca-995c-19e7dcd993b3', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1500.00, 'Pago de partialidad #5 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 09:17:58.896545+00', '2026-06-04 08:47:37.475275+00', '2026-06-04 08:47:37.475275+00', 2);
INSERT INTO public.movements VALUES ('a1fb059c-614d-43ad-92a6-9499f0c6af06', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1500.00, 'Pago de partialidad #5 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 09:15:24.008114+00', '2026-06-04 08:47:44.486568+00', '2026-06-04 08:47:44.486568+00', 2);
INSERT INTO public.movements VALUES ('90a79b48-8c7b-49e8-98ef-8daac1e50354', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1500.00, 'Pago de partialidad #5 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:59:08.058139+00', '2026-06-04 08:47:46.494123+00', '2026-06-04 08:47:46.494123+00', 2);
INSERT INTO public.movements VALUES ('d33352ca-3c04-43af-aff5-1adf898322ff', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1500.00, 'Pago de partialidad #5 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:57:51.542079+00', '2026-06-04 08:47:48.531209+00', '2026-06-04 08:47:48.531209+00', 2);
INSERT INTO public.movements VALUES ('979f6912-2c06-42c5-bfc3-3c1908959458', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1500.00, 'Pago de partialidad #5 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:49:39.523909+00', '2026-06-04 08:47:53.397788+00', '2026-06-04 08:47:53.397788+00', 2);
INSERT INTO public.movements VALUES ('f003c5a3-c649-4039-9640-514d84d80a59', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1500.00, 'Pago de partialidad #5 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:31:28.291185+00', '2026-06-04 08:47:56.364189+00', '2026-06-04 08:47:56.364189+00', 2);
INSERT INTO public.movements VALUES ('52e3a104-abc3-4f2e-856b-2c1c95273c23', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #1 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:21:59.865579+00', '2026-06-04 08:48:01.658114+00', '2026-06-04 08:48:01.658114+00', 2);
INSERT INTO public.movements VALUES ('3f9477fb-9ccc-40d3-87fc-856e1c67b10b', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'income', 1666.67, 'devolucion', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:21:46.869129+00', '2026-06-04 08:48:04.462563+00', '2026-06-04 08:48:04.462563+00', 2);
INSERT INTO public.movements VALUES ('11ef5488-b850-4eff-b80a-e664400ea823', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'income', 3333.34, 'devolucion', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:15:18.126456+00', '2026-06-04 08:48:06.918014+00', '2026-06-04 08:48:06.918014+00', 2);
INSERT INTO public.movements VALUES ('08ea625c-03b0-4732-abd0-707ef89ca370', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #1 - Deuda', '2026-06-03', '{}', '{"notes": "123", "currency": "MXN"}', '2026-06-03 08:07:08.159674+00', '2026-06-04 08:48:09.360854+00', '2026-06-04 08:48:09.360854+00', 2);
INSERT INTO public.movements VALUES ('9e079236-e4d4-4043-8224-117019a30de9', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #1 - Deuda', '2026-06-03', '{}', '{"notes": "123", "currency": "MXN"}', '2026-06-03 08:06:59.174707+00', '2026-06-04 08:48:11.726449+00', '2026-06-04 08:48:11.726449+00', 2);
INSERT INTO public.movements VALUES ('4250f111-9d67-4ff6-ab74-9e7dd3633b8f', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #1 - Deuda', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:06:53.135121+00', '2026-06-04 08:48:13.999536+00', '2026-06-04 08:48:13.999536+00', 2);
INSERT INTO public.movements VALUES ('be708c7d-66b9-46c7-95e5-d3a84caf393d', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, '84e73d07-de7f-4565-8f04-876883a6939c', NULL, 'expense', 1500.00, 'comida', '2026-06-03', '{}', '{"notes": "123", "currency": "MXN"}', '2026-06-03 08:06:26.923327+00', '2026-06-04 08:48:16.406227+00', '2026-06-04 08:48:16.406227+00', 2);
INSERT INTO public.movements VALUES ('88d7d68a-0cd5-49eb-a4a3-2163e9993430', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, 'ce7f500e-de30-47a4-b4a8-928041ae227f', NULL, 'income', 300.00, 'Depósito de efectivo: retiro', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:06:07.604568+00', '2026-06-04 08:48:19.163561+00', '2026-06-04 08:48:19.163561+00', 2);
INSERT INTO public.movements VALUES ('88d1906a-2963-462a-87ef-8e69b7e01431', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, 'ce7f500e-de30-47a4-b4a8-928041ae227f', NULL, 'expense', 300.00, 'Retiro de efectivo: retiro', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:06:07.362333+00', '2026-06-04 08:48:20.892575+00', '2026-06-04 08:48:20.892575+00', 2);
INSERT INTO public.movements VALUES ('6a1dd8bd-19a1-4251-af09-ad3404fb9e41', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, 'ce7f500e-de30-47a4-b4a8-928041ae227f', NULL, 'expense', 200.00, 'retiro', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:05:23.418851+00', '2026-06-04 08:48:22.900968+00', '2026-06-04 08:48:22.900968+00', 2);
INSERT INTO public.movements VALUES ('043d6be6-bdf1-4c0f-8f48-03e415cd5be8', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'expense', 500.00, 'retiro', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 06:36:16.378847+00', '2026-06-04 08:48:26.012157+00', '2026-06-04 08:48:26.012157+00', 2);
INSERT INTO public.movements VALUES ('29e6b9a1-9197-4b6a-b6c8-d638407e0108', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '9d6dfe90-686b-4c28-b554-ab07c37fc540', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'payment', 1000.00, 'pago mes', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 21:59:35.711622+00', '2026-06-04 08:48:27.684811+00', '2026-06-04 08:48:27.684811+00', 2);
INSERT INTO public.movements VALUES ('25f94b41-50fb-4e61-853f-2e806e3944de', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, '84e73d07-de7f-4565-8f04-876883a6939c', NULL, 'expense', 1200.00, 'prueba', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 10:15:05.206135+00', '2026-06-04 08:48:30.324313+00', '2026-06-04 08:48:30.324313+00', 2);
INSERT INTO public.movements VALUES ('67f8e0dd-eb93-4225-bd9b-e353cfb2e27c', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'income', 14500.00, 'quincena', '2026-06-02', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-02 05:47:05.087462+00', '2026-06-04 08:48:33.049557+00', '2026-06-04 08:48:33.049557+00', 2);
INSERT INTO public.movements VALUES ('0b4066f2-037e-4a87-9cc9-3729656ba79c', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #3 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:22:57.383457+00', '2026-06-04 08:46:54.839191+00', '2026-06-04 08:46:54.839191+00', 2);
INSERT INTO public.movements VALUES ('414f8a8a-0d55-4d96-a585-635e2c97e471', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #3 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:22:39.308419+00', '2026-06-04 08:46:56.450275+00', '2026-06-04 08:46:56.450275+00', 2);
INSERT INTO public.movements VALUES ('7886005c-e89a-40ca-97dc-07f663e6a118', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #3 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:22:28.187086+00', '2026-06-04 08:47:18.705665+00', '2026-06-04 08:47:18.705665+00', 2);
INSERT INTO public.movements VALUES ('5aed6b70-5034-4744-b829-6a2162348410', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #5 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:25:35.102498+00', '2026-06-04 08:46:45.8928+00', '2026-06-04 08:46:45.8928+00', 2);
INSERT INTO public.movements VALUES ('f58e1a10-8035-45b6-b6e8-86f501761067', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #4 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:25:02.669684+00', '2026-06-04 08:46:47.671026+00', '2026-06-04 08:46:47.671026+00', 2);
INSERT INTO public.movements VALUES ('c07053ea-c390-498e-afb7-4cbb26891f36', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #4 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:24:37.411626+00', '2026-06-04 08:46:49.436275+00', '2026-06-04 08:46:49.436275+00', 2);
INSERT INTO public.movements VALUES ('47d47e55-dd15-4683-9817-179f8ed7ce72', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #4 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:23:57.024615+00', '2026-06-04 08:46:50.984826+00', '2026-06-04 08:46:50.984826+00', 2);
INSERT INTO public.movements VALUES ('71fd8f99-0edf-4248-abdb-8df008ac8d74', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #3 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:23:00.930236+00', '2026-06-04 08:46:53.175178+00', '2026-06-04 08:46:53.175178+00', 2);
INSERT INTO public.movements VALUES ('bf7f56de-566e-490b-830a-e01524a74000', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #3 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:22:33.455217+00', '2026-06-04 08:46:57.869689+00', '2026-06-04 08:46:57.869689+00', 2);
INSERT INTO public.movements VALUES ('eaf168a6-8823-4090-9046-281c8df07a1e', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'payment', 1666.67, 'Pago de partialidad #4 - Deuda', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:29:10.606796+00', '2026-06-04 08:47:16.418248+00', '2026-06-04 08:47:16.418248+00', 2);
INSERT INTO public.movements VALUES ('fe50855a-1449-4227-a142-1ccd5aa24b7e', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'f6ba01c6-ea0c-492f-9e50-41b1419afab9', NULL, NULL, NULL, 'expense', 1666.67, 'Pago de partialidad', '2026-06-04', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-04 04:21:23.321455+00', '2026-06-04 08:47:21.312113+00', '2026-06-04 08:47:21.312113+00', 2);
INSERT INTO public.movements VALUES ('9e755400-2ca2-46aa-b7d3-8cbc6be002e7', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', '2f6e6ab9-0902-4858-b642-8b8e5b868243', NULL, 'c36a3978-8c0e-4f0e-a006-44a123bbeb0e', NULL, 'income', 1666.67, 'devolucion', '2026-06-03', '{}', '{"notes": null, "currency": "MXN"}', '2026-06-03 08:23:17.397779+00', '2026-06-04 08:47:59.426603+00', '2026-06-04 08:47:59.426603+00', 2);


--
-- TOC entry 4173 (class 0 OID 17989)
-- Dependencies: 350
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- TOC entry 4164 (class 0 OID 17761)
-- Dependencies: 341
-- Data for Name: profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.profiles VALUES ('a7992e67-2be0-4485-9006-3d11f3ec3fc3', 'erik ortiz', NULL, 'es-MX', 'America/Mexico_City', 'MXN', '{"payDays": "[1,15]", "payCycle": "biweekly", "mainAccountId": "2f6e6ab9-0902-4858-b642-8b8e5b868243", "monthlyIncome": 0}', '{}', '2026-06-01 07:30:36.101424+00', '2026-06-04 08:51:09.82575+00', NULL, 11, 0.00);


--
-- TOC entry 4170 (class 0 OID 17913)
-- Dependencies: 347
-- Data for Name: scheduled_payments; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO public.scheduled_payments VALUES ('a92915b8-4eb4-428f-851e-d2de2d756f52', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, '84e73d07-de7f-4565-8f04-876883a6939c', 'Comida casa', 1500.00, 'biweekly', '2026-06-15', NULL, false, true, '{"currency": "MXN"}', '2026-06-01 23:00:32.214782+00', '2026-06-02 01:10:22.531176+00', '2026-06-02 01:10:22.531176+00', 3, 'expense', NULL);
INSERT INTO public.scheduled_payments VALUES ('ac390dbb-8d91-4fba-9b71-cd830604d857', 'a7992e67-2be0-4485-9006-3d11f3ec3fc3', NULL, '84e73d07-de7f-4565-8f04-876883a6939c', 'comida', 1500.00, 'biweekly', '2026-06-29', NULL, false, true, '{"currency": "MXN"}', '2026-06-02 05:18:10.385309+00', '2026-06-04 08:49:11.034539+00', '2026-06-04 08:49:11.034539+00', 3, 'expense', NULL);


--
-- TOC entry 3879 (class 2606 OID 17796)
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- TOC entry 3922 (class 2606 OID 18014)
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- TOC entry 3916 (class 2606 OID 17978)
-- Name: budgets budgets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_pkey PRIMARY KEY (id);


--
-- TOC entry 3883 (class 2606 OID 17814)
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- TOC entry 3887 (class 2606 OID 17840)
-- Name: debts debts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.debts
    ADD CONSTRAINT debts_pkey PRIMARY KEY (id);


--
-- TOC entry 3913 (class 2606 OID 17958)
-- Name: financial_goals financial_goals_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.financial_goals
    ADD CONSTRAINT financial_goals_pkey PRIMARY KEY (id);


--
-- TOC entry 3904 (class 2606 OID 17902)
-- Name: installments installments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.installments
    ADD CONSTRAINT installments_pkey PRIMARY KEY (id);


--
-- TOC entry 3893 (class 2606 OID 17865)
-- Name: movements movements_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.movements
    ADD CONSTRAINT movements_pkey PRIMARY KEY (id);


--
-- TOC entry 3919 (class 2606 OID 17998)
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- TOC entry 3876 (class 2606 OID 17775)
-- Name: profiles profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (user_id);


--
-- TOC entry 3911 (class 2606 OID 17927)
-- Name: scheduled_payments scheduled_payments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scheduled_payments
    ADD CONSTRAINT scheduled_payments_pkey PRIMARY KEY (id);


--
-- TOC entry 3880 (class 1259 OID 18019)
-- Name: accounts_user_id_active_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX accounts_user_id_active_idx ON public.accounts USING btree (user_id, is_active, deleted_at, updated_at DESC);


--
-- TOC entry 3881 (class 1259 OID 18020)
-- Name: accounts_user_name_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX accounts_user_name_unique ON public.accounts USING btree (user_id, name) WHERE (deleted_at IS NULL);


--
-- TOC entry 3923 (class 1259 OID 18035)
-- Name: audit_log_user_table_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX audit_log_user_table_idx ON public.audit_log USING btree (user_id, table_name, changed_at DESC);


--
-- TOC entry 3917 (class 1259 OID 18362)
-- Name: budgets_user_period_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX budgets_user_period_idx ON public.budgets USING btree (user_id, budget_period, period_start, period_end) WHERE (deleted_at IS NULL);


--
-- TOC entry 3884 (class 1259 OID 18369)
-- Name: categories_user_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX categories_user_idx ON public.categories USING btree (user_id, category_type, deleted_at);


--
-- TOC entry 3885 (class 1259 OID 18368)
-- Name: categories_user_name_type_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX categories_user_name_type_unique ON public.categories USING btree (user_id, category_type, name) WHERE (deleted_at IS NULL);


--
-- TOC entry 3888 (class 1259 OID 18370)
-- Name: debts_user_status_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX debts_user_status_idx ON public.debts USING btree (user_id, debt_type, deleted_at, updated_at DESC);


--
-- TOC entry 3914 (class 1259 OID 18364)
-- Name: goals_user_status_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX goals_user_status_idx ON public.financial_goals USING btree (user_id, status, deleted_at);


--
-- TOC entry 3896 (class 1259 OID 18431)
-- Name: idx_installments_account_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_installments_account_id ON public.installments USING btree (account_id) WHERE (deleted_at IS NULL);


--
-- TOC entry 3897 (class 1259 OID 18236)
-- Name: idx_installments_debt_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_installments_debt_id ON public.installments USING btree (debt_id);


--
-- TOC entry 3898 (class 1259 OID 18237)
-- Name: idx_installments_due_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_installments_due_date ON public.installments USING btree (due_date, paid);


--
-- TOC entry 3899 (class 1259 OID 18238)
-- Name: idx_installments_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_installments_user_id ON public.installments USING btree (user_id);


--
-- TOC entry 3905 (class 1259 OID 18429)
-- Name: idx_scheduled_payments_end_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scheduled_payments_end_date ON public.scheduled_payments USING btree (end_date) WHERE (deleted_at IS NULL);


--
-- TOC entry 3906 (class 1259 OID 18428)
-- Name: idx_scheduled_payments_next_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scheduled_payments_next_date ON public.scheduled_payments USING btree (next_date) WHERE (deleted_at IS NULL);


--
-- TOC entry 3907 (class 1259 OID 18430)
-- Name: idx_scheduled_payments_payment_type; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_scheduled_payments_payment_type ON public.scheduled_payments USING btree (payment_type) WHERE (deleted_at IS NULL);


--
-- TOC entry 3900 (class 1259 OID 18399)
-- Name: installments_account_id_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX installments_account_id_idx ON public.installments USING btree (account_id) WHERE (deleted_at IS NULL);


--
-- TOC entry 3901 (class 1259 OID 18029)
-- Name: installments_debt_due_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX installments_debt_due_idx ON public.installments USING btree (debt_id, due_date, paid) WHERE (deleted_at IS NULL);


--
-- TOC entry 3902 (class 1259 OID 18030)
-- Name: installments_debt_number_unique; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX installments_debt_number_unique ON public.installments USING btree (debt_id, number) WHERE (deleted_at IS NULL);


--
-- TOC entry 3889 (class 1259 OID 18024)
-- Name: movements_account_date_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX movements_account_date_idx ON public.movements USING btree (account_id, movement_date DESC) WHERE (deleted_at IS NULL);


--
-- TOC entry 3890 (class 1259 OID 18025)
-- Name: movements_category_date_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX movements_category_date_idx ON public.movements USING btree (category_id, movement_date DESC) WHERE (deleted_at IS NULL);


--
-- TOC entry 3891 (class 1259 OID 18026)
-- Name: movements_debt_date_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX movements_debt_date_idx ON public.movements USING btree (debt_id, movement_date DESC) WHERE (deleted_at IS NULL);


--
-- TOC entry 3894 (class 1259 OID 18375)
-- Name: movements_tags_gin_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX movements_tags_gin_idx ON public.movements USING gin (tags);


--
-- TOC entry 3895 (class 1259 OID 18023)
-- Name: movements_user_date_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX movements_user_date_idx ON public.movements USING btree (user_id, movement_date DESC, created_at DESC) WHERE (deleted_at IS NULL);


--
-- TOC entry 3920 (class 1259 OID 18034)
-- Name: notifications_user_read_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX notifications_user_read_idx ON public.notifications USING btree (user_id, read_at, created_at DESC);


--
-- TOC entry 3877 (class 1259 OID 18018)
-- Name: profiles_user_id_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX profiles_user_id_idx ON public.profiles USING btree (user_id);


--
-- TOC entry 3908 (class 1259 OID 18031)
-- Name: scheduled_payments_next_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX scheduled_payments_next_idx ON public.scheduled_payments USING btree (user_id, active, next_date) WHERE (deleted_at IS NULL);


--
-- TOC entry 3909 (class 1259 OID 18409)
-- Name: scheduled_payments_payment_type_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX scheduled_payments_payment_type_idx ON public.scheduled_payments USING btree (payment_type) WHERE (deleted_at IS NULL);


--
-- TOC entry 3946 (class 2620 OID 18038)
-- Name: accounts trg_accounts_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_accounts_touch BEFORE UPDATE ON public.accounts FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3947 (class 2620 OID 18037)
-- Name: accounts trg_accounts_validate; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_accounts_validate BEFORE INSERT OR UPDATE ON public.accounts FOR EACH ROW EXECUTE FUNCTION public.validate_account_payload();


--
-- TOC entry 3962 (class 2620 OID 18051)
-- Name: budgets trg_budgets_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_budgets_touch BEFORE UPDATE ON public.budgets FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3948 (class 2620 OID 18040)
-- Name: categories trg_categories_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_categories_touch BEFORE UPDATE ON public.categories FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3949 (class 2620 OID 18039)
-- Name: categories trg_categories_validate; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_categories_validate BEFORE INSERT OR UPDATE ON public.categories FOR EACH ROW EXECUTE FUNCTION public.validate_category_payload();


--
-- TOC entry 3950 (class 2620 OID 18233)
-- Name: debts trg_debts_generate_installments; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_debts_generate_installments AFTER INSERT ON public.debts FOR EACH ROW EXECUTE FUNCTION public.generate_installments_for_debt();


--
-- TOC entry 3951 (class 2620 OID 18042)
-- Name: debts trg_debts_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_debts_touch BEFORE UPDATE ON public.debts FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3952 (class 2620 OID 18041)
-- Name: debts trg_debts_validate; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_debts_validate BEFORE INSERT OR UPDATE ON public.debts FOR EACH ROW EXECUTE FUNCTION public.validate_debt_payload();


--
-- TOC entry 3961 (class 2620 OID 18050)
-- Name: financial_goals trg_goals_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_goals_touch BEFORE UPDATE ON public.financial_goals FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3957 (class 2620 OID 18240)
-- Name: installments trg_installments_paid_at; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_installments_paid_at BEFORE INSERT OR UPDATE ON public.installments FOR EACH ROW EXECUTE FUNCTION public.sync_installment_payment();


--
-- TOC entry 3958 (class 2620 OID 18239)
-- Name: installments trg_installments_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_installments_touch BEFORE UPDATE ON public.installments FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3959 (class 2620 OID 18235)
-- Name: installments trg_installments_update_debt; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_installments_update_debt AFTER INSERT OR UPDATE OF paid ON public.installments FOR EACH ROW EXECUTE FUNCTION public.update_debt_remaining_installments();


--
-- TOC entry 3953 (class 2620 OID 18411)
-- Name: movements trg_movements_balance; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_movements_balance AFTER INSERT OR DELETE OR UPDATE ON public.movements FOR EACH ROW EXECUTE FUNCTION public.apply_movement_balance();


--
-- TOC entry 3954 (class 2620 OID 18044)
-- Name: movements trg_movements_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_movements_touch BEFORE UPDATE ON public.movements FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3955 (class 2620 OID 18219)
-- Name: movements trg_movements_update_debt; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_movements_update_debt AFTER INSERT ON public.movements FOR EACH ROW EXECUTE FUNCTION public.update_debt_balance_on_payment();


--
-- TOC entry 3956 (class 2620 OID 18410)
-- Name: movements trg_movements_validate; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_movements_validate BEFORE INSERT OR UPDATE ON public.movements FOR EACH ROW EXECUTE FUNCTION public.validate_movement_payload();


--
-- TOC entry 3945 (class 2620 OID 18036)
-- Name: profiles trg_profiles_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_profiles_touch BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3960 (class 2620 OID 18049)
-- Name: scheduled_payments trg_scheduled_payments_touch; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_scheduled_payments_touch BEFORE UPDATE ON public.scheduled_payments FOR EACH ROW EXECUTE FUNCTION public.touch_updated_at();


--
-- TOC entry 3925 (class 2606 OID 17797)
-- Name: accounts accounts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3942 (class 2606 OID 17984)
-- Name: budgets budgets_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id) ON DELETE CASCADE;


--
-- TOC entry 3943 (class 2606 OID 17979)
-- Name: budgets budgets_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3926 (class 2606 OID 17815)
-- Name: categories categories_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3927 (class 2606 OID 17846)
-- Name: debts debts_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.debts
    ADD CONSTRAINT debts_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE SET NULL;


--
-- TOC entry 3928 (class 2606 OID 17841)
-- Name: debts debts_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.debts
    ADD CONSTRAINT debts_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3941 (class 2606 OID 17959)
-- Name: financial_goals financial_goals_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.financial_goals
    ADD CONSTRAINT financial_goals_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3934 (class 2606 OID 18223)
-- Name: installments fk_installments_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.installments
    ADD CONSTRAINT fk_installments_user FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3935 (class 2606 OID 18393)
-- Name: installments installments_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.installments
    ADD CONSTRAINT installments_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE SET NULL;


--
-- TOC entry 3936 (class 2606 OID 17903)
-- Name: installments installments_debt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.installments
    ADD CONSTRAINT installments_debt_id_fkey FOREIGN KEY (debt_id) REFERENCES public.debts(id) ON DELETE CASCADE;


--
-- TOC entry 3937 (class 2606 OID 17908)
-- Name: installments installments_payment_movement_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.installments
    ADD CONSTRAINT installments_payment_movement_id_fkey FOREIGN KEY (payment_movement_id) REFERENCES public.movements(id) ON DELETE SET NULL;


--
-- TOC entry 3929 (class 2606 OID 17871)
-- Name: movements movements_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.movements
    ADD CONSTRAINT movements_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- TOC entry 3930 (class 2606 OID 17881)
-- Name: movements movements_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.movements
    ADD CONSTRAINT movements_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id) ON DELETE SET NULL;


--
-- TOC entry 3931 (class 2606 OID 17886)
-- Name: movements movements_debt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.movements
    ADD CONSTRAINT movements_debt_id_fkey FOREIGN KEY (debt_id) REFERENCES public.debts(id) ON DELETE SET NULL;


--
-- TOC entry 3932 (class 2606 OID 17876)
-- Name: movements movements_transfer_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.movements
    ADD CONSTRAINT movements_transfer_account_id_fkey FOREIGN KEY (transfer_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- TOC entry 3933 (class 2606 OID 17866)
-- Name: movements movements_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.movements
    ADD CONSTRAINT movements_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3944 (class 2606 OID 17999)
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3924 (class 2606 OID 17776)
-- Name: profiles profiles_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 3938 (class 2606 OID 17933)
-- Name: scheduled_payments scheduled_payments_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scheduled_payments
    ADD CONSTRAINT scheduled_payments_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE SET NULL;


--
-- TOC entry 3939 (class 2606 OID 17938)
-- Name: scheduled_payments scheduled_payments_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scheduled_payments
    ADD CONSTRAINT scheduled_payments_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id) ON DELETE SET NULL;


--
-- TOC entry 3940 (class 2606 OID 17928)
-- Name: scheduled_payments scheduled_payments_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.scheduled_payments
    ADD CONSTRAINT scheduled_payments_user_id_fkey FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;


--
-- TOC entry 4112 (class 0 OID 17781)
-- Dependencies: 342
-- Name: accounts; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4129 (class 3256 OID 18059)
-- Name: accounts accounts_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY accounts_delete_own ON public.accounts FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4127 (class 3256 OID 18057)
-- Name: accounts accounts_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY accounts_insert_own ON public.accounts FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4126 (class 3256 OID 18056)
-- Name: accounts accounts_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY accounts_select_own ON public.accounts FOR SELECT USING ((public.is_owner(user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4128 (class 3256 OID 18058)
-- Name: accounts accounts_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY accounts_update_own ON public.accounts FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4121 (class 0 OID 18004)
-- Dependencies: 351
-- Name: audit_log; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.audit_log ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4159 (class 3256 OID 18092)
-- Name: audit_log audit_log_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY audit_log_select_own ON public.audit_log FOR SELECT USING (public.is_owner(user_id));


--
-- TOC entry 4119 (class 0 OID 17964)
-- Dependencies: 349
-- Name: budgets; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.budgets ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4154 (class 3256 OID 18087)
-- Name: budgets budgets_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY budgets_delete_own ON public.budgets FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4152 (class 3256 OID 18085)
-- Name: budgets budgets_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY budgets_insert_own ON public.budgets FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4151 (class 3256 OID 18084)
-- Name: budgets budgets_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY budgets_select_own ON public.budgets FOR SELECT USING ((public.is_owner(user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4153 (class 3256 OID 18086)
-- Name: budgets budgets_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY budgets_update_own ON public.budgets FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4113 (class 0 OID 17802)
-- Dependencies: 343
-- Name: categories; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.categories ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4133 (class 3256 OID 18063)
-- Name: categories categories_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY categories_delete_own ON public.categories FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4131 (class 3256 OID 18061)
-- Name: categories categories_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY categories_insert_own ON public.categories FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4130 (class 3256 OID 18060)
-- Name: categories categories_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY categories_select_own ON public.categories FOR SELECT USING ((public.is_owner(user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4132 (class 3256 OID 18062)
-- Name: categories categories_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY categories_update_own ON public.categories FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4114 (class 0 OID 17820)
-- Dependencies: 344
-- Name: debts; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.debts ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4138 (class 3256 OID 18067)
-- Name: debts debts_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY debts_delete_own ON public.debts FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4135 (class 3256 OID 18065)
-- Name: debts debts_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY debts_insert_own ON public.debts FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4134 (class 3256 OID 18064)
-- Name: debts debts_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY debts_select_own ON public.debts FOR SELECT USING ((public.is_owner(user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4137 (class 3256 OID 18066)
-- Name: debts debts_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY debts_update_own ON public.debts FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4118 (class 0 OID 17943)
-- Dependencies: 348
-- Name: financial_goals; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.financial_goals ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4150 (class 3256 OID 18083)
-- Name: financial_goals goals_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY goals_delete_own ON public.financial_goals FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4148 (class 3256 OID 18081)
-- Name: financial_goals goals_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY goals_insert_own ON public.financial_goals FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4147 (class 3256 OID 18080)
-- Name: financial_goals goals_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY goals_select_own ON public.financial_goals FOR SELECT USING ((public.is_owner(user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4149 (class 3256 OID 18082)
-- Name: financial_goals goals_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY goals_update_own ON public.financial_goals FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4116 (class 0 OID 17891)
-- Dependencies: 346
-- Name: installments; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.installments ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4162 (class 3256 OID 18231)
-- Name: installments installments_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY installments_delete_own ON public.installments FOR DELETE USING ((auth.uid() = user_id));


--
-- TOC entry 4136 (class 3256 OID 18229)
-- Name: installments installments_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY installments_insert_own ON public.installments FOR INSERT WITH CHECK ((auth.uid() = user_id));


--
-- TOC entry 4160 (class 3256 OID 18228)
-- Name: installments installments_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY installments_select_own ON public.installments FOR SELECT USING (((auth.uid() = user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4161 (class 3256 OID 18230)
-- Name: installments installments_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY installments_update_own ON public.installments FOR UPDATE USING ((auth.uid() = user_id)) WITH CHECK ((auth.uid() = user_id));


--
-- TOC entry 4115 (class 0 OID 17851)
-- Dependencies: 345
-- Name: movements; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.movements ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4142 (class 3256 OID 18071)
-- Name: movements movements_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY movements_delete_own ON public.movements FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4140 (class 3256 OID 18069)
-- Name: movements movements_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY movements_insert_own ON public.movements FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4139 (class 3256 OID 18068)
-- Name: movements movements_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY movements_select_own ON public.movements FOR SELECT USING ((public.is_owner(user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4141 (class 3256 OID 18070)
-- Name: movements movements_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY movements_update_own ON public.movements FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4120 (class 0 OID 17989)
-- Dependencies: 350
-- Name: notifications; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4158 (class 3256 OID 18091)
-- Name: notifications notifications_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY notifications_delete_own ON public.notifications FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4156 (class 3256 OID 18089)
-- Name: notifications notifications_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY notifications_insert_own ON public.notifications FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4155 (class 3256 OID 18088)
-- Name: notifications notifications_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY notifications_select_own ON public.notifications FOR SELECT USING (public.is_owner(user_id));


--
-- TOC entry 4157 (class 3256 OID 18090)
-- Name: notifications notifications_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY notifications_update_own ON public.notifications FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4111 (class 0 OID 17761)
-- Dependencies: 341
-- Name: profiles; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4125 (class 3256 OID 18055)
-- Name: profiles profiles_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY profiles_delete_own ON public.profiles FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4123 (class 3256 OID 18053)
-- Name: profiles profiles_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY profiles_insert_own ON public.profiles FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4122 (class 3256 OID 18052)
-- Name: profiles profiles_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY profiles_select_own ON public.profiles FOR SELECT USING (public.is_owner(user_id));


--
-- TOC entry 4124 (class 3256 OID 18054)
-- Name: profiles profiles_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY profiles_update_own ON public.profiles FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4117 (class 0 OID 17913)
-- Dependencies: 347
-- Name: scheduled_payments; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.scheduled_payments ENABLE ROW LEVEL SECURITY;

--
-- TOC entry 4146 (class 3256 OID 18079)
-- Name: scheduled_payments scheduled_payments_delete_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY scheduled_payments_delete_own ON public.scheduled_payments FOR DELETE USING (public.is_owner(user_id));


--
-- TOC entry 4144 (class 3256 OID 18077)
-- Name: scheduled_payments scheduled_payments_insert_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY scheduled_payments_insert_own ON public.scheduled_payments FOR INSERT WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4143 (class 3256 OID 18076)
-- Name: scheduled_payments scheduled_payments_select_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY scheduled_payments_select_own ON public.scheduled_payments FOR SELECT USING ((public.is_owner(user_id) AND (deleted_at IS NULL)));


--
-- TOC entry 4145 (class 3256 OID 18078)
-- Name: scheduled_payments scheduled_payments_update_own; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY scheduled_payments_update_own ON public.scheduled_payments FOR UPDATE USING (public.is_owner(user_id)) WITH CHECK (public.is_owner(user_id));


--
-- TOC entry 4182 (class 0 OID 0)
-- Dependencies: 4180
-- Name: DATABASE postgres; Type: ACL; Schema: -; Owner: postgres
--

GRANT CREATE ON DATABASE postgres TO supabase_etl_admin;
GRANT CREATE ON DATABASE postgres TO supabase_storage_admin;
GRANT ALL ON DATABASE postgres TO dashboard_user;


--
-- TOC entry 4185 (class 0 OID 0)
-- Dependencies: 37
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

GRANT USAGE ON SCHEMA public TO postgres;
GRANT USAGE ON SCHEMA public TO anon;
GRANT USAGE ON SCHEMA public TO authenticated;
GRANT USAGE ON SCHEMA public TO service_role;


--
-- TOC entry 4186 (class 0 OID 0)
-- Dependencies: 510
-- Name: FUNCTION apply_movement_balance(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.apply_movement_balance() TO anon;
GRANT ALL ON FUNCTION public.apply_movement_balance() TO authenticated;
GRANT ALL ON FUNCTION public.apply_movement_balance() TO service_role;


--
-- TOC entry 4187 (class 0 OID 0)
-- Dependencies: 521
-- Name: FUNCTION generate_installments_for_debt(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.generate_installments_for_debt() TO anon;
GRANT ALL ON FUNCTION public.generate_installments_for_debt() TO authenticated;
GRANT ALL ON FUNCTION public.generate_installments_for_debt() TO service_role;


--
-- TOC entry 4188 (class 0 OID 0)
-- Dependencies: 512
-- Name: FUNCTION handle_new_user(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.handle_new_user() TO anon;
GRANT ALL ON FUNCTION public.handle_new_user() TO authenticated;
GRANT ALL ON FUNCTION public.handle_new_user() TO service_role;


--
-- TOC entry 4189 (class 0 OID 0)
-- Dependencies: 525
-- Name: FUNCTION handle_new_user_cash_account(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.handle_new_user_cash_account() TO anon;
GRANT ALL ON FUNCTION public.handle_new_user_cash_account() TO authenticated;
GRANT ALL ON FUNCTION public.handle_new_user_cash_account() TO service_role;


--
-- TOC entry 4190 (class 0 OID 0)
-- Dependencies: 504
-- Name: FUNCTION is_owner(p_user_id uuid); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.is_owner(p_user_id uuid) TO anon;
GRANT ALL ON FUNCTION public.is_owner(p_user_id uuid) TO authenticated;
GRANT ALL ON FUNCTION public.is_owner(p_user_id uuid) TO service_role;


--
-- TOC entry 4191 (class 0 OID 0)
-- Dependencies: 518
-- Name: FUNCTION migrate_from_indexeddb(p_categories jsonb, p_accounts jsonb, p_movements jsonb, p_debts jsonb, p_scheduled_payments jsonb); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.migrate_from_indexeddb(p_categories jsonb, p_accounts jsonb, p_movements jsonb, p_debts jsonb, p_scheduled_payments jsonb) TO anon;
GRANT ALL ON FUNCTION public.migrate_from_indexeddb(p_categories jsonb, p_accounts jsonb, p_movements jsonb, p_debts jsonb, p_scheduled_payments jsonb) TO authenticated;
GRANT ALL ON FUNCTION public.migrate_from_indexeddb(p_categories jsonb, p_accounts jsonb, p_movements jsonb, p_debts jsonb, p_scheduled_payments jsonb) TO service_role;


--
-- TOC entry 4192 (class 0 OID 0)
-- Dependencies: 505
-- Name: FUNCTION movement_effect(p_type public.movement_type, p_amount numeric); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.movement_effect(p_type public.movement_type, p_amount numeric) TO anon;
GRANT ALL ON FUNCTION public.movement_effect(p_type public.movement_type, p_amount numeric) TO authenticated;
GRANT ALL ON FUNCTION public.movement_effect(p_type public.movement_type, p_amount numeric) TO service_role;


--
-- TOC entry 4193 (class 0 OID 0)
-- Dependencies: 513
-- Name: FUNCTION owns_account(p_account_id uuid); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.owns_account(p_account_id uuid) TO anon;
GRANT ALL ON FUNCTION public.owns_account(p_account_id uuid) TO authenticated;
GRANT ALL ON FUNCTION public.owns_account(p_account_id uuid) TO service_role;


--
-- TOC entry 4194 (class 0 OID 0)
-- Dependencies: 514
-- Name: FUNCTION owns_category(p_category_id uuid); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.owns_category(p_category_id uuid) TO anon;
GRANT ALL ON FUNCTION public.owns_category(p_category_id uuid) TO authenticated;
GRANT ALL ON FUNCTION public.owns_category(p_category_id uuid) TO service_role;


--
-- TOC entry 4195 (class 0 OID 0)
-- Dependencies: 515
-- Name: FUNCTION owns_debt(p_debt_id uuid); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.owns_debt(p_debt_id uuid) TO anon;
GRANT ALL ON FUNCTION public.owns_debt(p_debt_id uuid) TO authenticated;
GRANT ALL ON FUNCTION public.owns_debt(p_debt_id uuid) TO service_role;


--
-- TOC entry 4196 (class 0 OID 0)
-- Dependencies: 516
-- Name: FUNCTION register_debt_payment(p_debt_id uuid, p_account_id uuid, p_amount numeric, p_movement_date date, p_description text, p_installment_id uuid); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.register_debt_payment(p_debt_id uuid, p_account_id uuid, p_amount numeric, p_movement_date date, p_description text, p_installment_id uuid) TO anon;
GRANT ALL ON FUNCTION public.register_debt_payment(p_debt_id uuid, p_account_id uuid, p_amount numeric, p_movement_date date, p_description text, p_installment_id uuid) TO authenticated;
GRANT ALL ON FUNCTION public.register_debt_payment(p_debt_id uuid, p_account_id uuid, p_amount numeric, p_movement_date date, p_description text, p_installment_id uuid) TO service_role;


--
-- TOC entry 4197 (class 0 OID 0)
-- Dependencies: 517
-- Name: FUNCTION soft_delete_entity(p_table text, p_id uuid); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.soft_delete_entity(p_table text, p_id uuid) TO anon;
GRANT ALL ON FUNCTION public.soft_delete_entity(p_table text, p_id uuid) TO authenticated;
GRANT ALL ON FUNCTION public.soft_delete_entity(p_table text, p_id uuid) TO service_role;


--
-- TOC entry 4198 (class 0 OID 0)
-- Dependencies: 511
-- Name: FUNCTION sync_installment_payment(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.sync_installment_payment() TO anon;
GRANT ALL ON FUNCTION public.sync_installment_payment() TO authenticated;
GRANT ALL ON FUNCTION public.sync_installment_payment() TO service_role;


--
-- TOC entry 4199 (class 0 OID 0)
-- Dependencies: 503
-- Name: FUNCTION touch_updated_at(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.touch_updated_at() TO anon;
GRANT ALL ON FUNCTION public.touch_updated_at() TO authenticated;
GRANT ALL ON FUNCTION public.touch_updated_at() TO service_role;


--
-- TOC entry 4200 (class 0 OID 0)
-- Dependencies: 519
-- Name: FUNCTION update_debt_balance_on_payment(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.update_debt_balance_on_payment() TO anon;
GRANT ALL ON FUNCTION public.update_debt_balance_on_payment() TO authenticated;
GRANT ALL ON FUNCTION public.update_debt_balance_on_payment() TO service_role;


--
-- TOC entry 4201 (class 0 OID 0)
-- Dependencies: 522
-- Name: FUNCTION update_debt_remaining_installments(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.update_debt_remaining_installments() TO anon;
GRANT ALL ON FUNCTION public.update_debt_remaining_installments() TO authenticated;
GRANT ALL ON FUNCTION public.update_debt_remaining_installments() TO service_role;


--
-- TOC entry 4202 (class 0 OID 0)
-- Dependencies: 520
-- Name: FUNCTION update_goal_progress(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.update_goal_progress() TO anon;
GRANT ALL ON FUNCTION public.update_goal_progress() TO authenticated;
GRANT ALL ON FUNCTION public.update_goal_progress() TO service_role;


--
-- TOC entry 4203 (class 0 OID 0)
-- Dependencies: 506
-- Name: FUNCTION validate_account_payload(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.validate_account_payload() TO anon;
GRANT ALL ON FUNCTION public.validate_account_payload() TO authenticated;
GRANT ALL ON FUNCTION public.validate_account_payload() TO service_role;


--
-- TOC entry 4204 (class 0 OID 0)
-- Dependencies: 507
-- Name: FUNCTION validate_category_payload(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.validate_category_payload() TO anon;
GRANT ALL ON FUNCTION public.validate_category_payload() TO authenticated;
GRANT ALL ON FUNCTION public.validate_category_payload() TO service_role;


--
-- TOC entry 4205 (class 0 OID 0)
-- Dependencies: 508
-- Name: FUNCTION validate_debt_payload(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.validate_debt_payload() TO anon;
GRANT ALL ON FUNCTION public.validate_debt_payload() TO authenticated;
GRANT ALL ON FUNCTION public.validate_debt_payload() TO service_role;


--
-- TOC entry 4206 (class 0 OID 0)
-- Dependencies: 509
-- Name: FUNCTION validate_movement_payload(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.validate_movement_payload() TO anon;
GRANT ALL ON FUNCTION public.validate_movement_payload() TO authenticated;
GRANT ALL ON FUNCTION public.validate_movement_payload() TO service_role;


--
-- TOC entry 4207 (class 0 OID 0)
-- Dependencies: 342
-- Name: TABLE accounts; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.accounts TO anon;
GRANT ALL ON TABLE public.accounts TO authenticated;
GRANT ALL ON TABLE public.accounts TO service_role;


--
-- TOC entry 4208 (class 0 OID 0)
-- Dependencies: 351
-- Name: TABLE audit_log; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.audit_log TO anon;
GRANT ALL ON TABLE public.audit_log TO authenticated;
GRANT ALL ON TABLE public.audit_log TO service_role;


--
-- TOC entry 4209 (class 0 OID 0)
-- Dependencies: 349
-- Name: TABLE budgets; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.budgets TO anon;
GRANT ALL ON TABLE public.budgets TO authenticated;
GRANT ALL ON TABLE public.budgets TO service_role;


--
-- TOC entry 4210 (class 0 OID 0)
-- Dependencies: 343
-- Name: TABLE categories; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.categories TO anon;
GRANT ALL ON TABLE public.categories TO authenticated;
GRANT ALL ON TABLE public.categories TO service_role;


--
-- TOC entry 4211 (class 0 OID 0)
-- Dependencies: 344
-- Name: TABLE debts; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.debts TO anon;
GRANT ALL ON TABLE public.debts TO authenticated;
GRANT ALL ON TABLE public.debts TO service_role;


--
-- TOC entry 4212 (class 0 OID 0)
-- Dependencies: 348
-- Name: TABLE financial_goals; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.financial_goals TO anon;
GRANT ALL ON TABLE public.financial_goals TO authenticated;
GRANT ALL ON TABLE public.financial_goals TO service_role;


--
-- TOC entry 4213 (class 0 OID 0)
-- Dependencies: 346
-- Name: TABLE installments; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.installments TO anon;
GRANT ALL ON TABLE public.installments TO authenticated;
GRANT ALL ON TABLE public.installments TO service_role;


--
-- TOC entry 4214 (class 0 OID 0)
-- Dependencies: 345
-- Name: TABLE movements; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.movements TO anon;
GRANT ALL ON TABLE public.movements TO authenticated;
GRANT ALL ON TABLE public.movements TO service_role;


--
-- TOC entry 4215 (class 0 OID 0)
-- Dependencies: 350
-- Name: TABLE notifications; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.notifications TO anon;
GRANT ALL ON TABLE public.notifications TO authenticated;
GRANT ALL ON TABLE public.notifications TO service_role;


--
-- TOC entry 4216 (class 0 OID 0)
-- Dependencies: 341
-- Name: TABLE profiles; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.profiles TO anon;
GRANT ALL ON TABLE public.profiles TO authenticated;
GRANT ALL ON TABLE public.profiles TO service_role;


--
-- TOC entry 4217 (class 0 OID 0)
-- Dependencies: 347
-- Name: TABLE scheduled_payments; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.scheduled_payments TO anon;
GRANT ALL ON TABLE public.scheduled_payments TO authenticated;
GRANT ALL ON TABLE public.scheduled_payments TO service_role;


--
-- TOC entry 2539 (class 826 OID 16494)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO service_role;


--
-- TOC entry 2540 (class 826 OID 16495)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: supabase_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO service_role;


--
-- TOC entry 2538 (class 826 OID 16493)
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO service_role;


--
-- TOC entry 2542 (class 826 OID 16497)
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: public; Owner: supabase_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO service_role;


--
-- TOC entry 2537 (class 826 OID 16492)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO service_role;


--
-- TOC entry 2541 (class 826 OID 16496)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: supabase_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO service_role;


-- Completed on 2026-06-04 17:42:09

--
-- PostgreSQL database dump complete
--

\unrestrict uehkTi1FaIyLbgIErxGDf4NqtgbUxBzdtr6BEKe3rveBvqZKLm5rsYjvOAzAFGo

