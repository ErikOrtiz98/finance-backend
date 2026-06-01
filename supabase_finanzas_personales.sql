-- Extensions
create extension if not exists pgcrypto;
create extension if not exists citext;

-- Enums
do $$ begin
  create type public.account_type as enum (
    'debit',
    'credit',
    'savings',
    'cash',
    'investment',
    'loan'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.category_type as enum (
    'income',
    'expense',
    'transfer',
    'both'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.movement_type as enum (
    'income',
    'expense',
    'transfer',
    'payment',
    'adjustment'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.debt_type as enum (
    'loan',
    'credit_card',
    'msi',
    'financing'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.payment_frequency as enum (
    'weekly',
    'biweekly',
    'monthly',
    'quarterly',
    'yearly',
    'custom'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.budget_period as enum (
    'weekly',
    'biweekly',
    'monthly',
    'custom'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.goal_status as enum (
    'active',
    'paused',
    'achieved',
    'cancelled'
  );
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.notification_type as enum (
    'info',
    'warning',
    'success',
    'overdue',
    'budget_alert',
    'goal_alert',
    'debt_alert'
  );
exception when duplicate_object then null; end $$;

-- Helper functions
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
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

create or replace function public.is_owner(p_user_id uuid)
returns boolean
language sql
stable
as $$
  select auth.uid() = p_user_id;
$$;

create or replace function public.movement_effect(p_type public.movement_type, p_amount numeric)
returns numeric
language sql
immutable
as $$
  select case
    when p_type = 'income' then abs(p_amount)
    when p_type in ('expense', 'payment') then -abs(p_amount)
    when p_type = 'transfer' then -abs(p_amount)
    when p_type = 'adjustment' then p_amount
  end;
$$;

create or replace function public.validate_account_payload()
returns trigger
language plpgsql
as $$
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

create or replace function public.validate_category_payload()
returns trigger
language plpgsql
as $$
begin
  if auth.uid() is not null and new.user_id is distinct from auth.uid() then
    raise exception 'user_id must match auth.uid()';
  end if;
  return new;
end;
$$;

create or replace function public.validate_debt_payload()
returns trigger
language plpgsql
as $$
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

create or replace function public.validate_movement_payload()
returns trigger
language plpgsql
as $$
begin
  if auth.uid() is not null and new.user_id is distinct from auth.uid() then
    raise exception 'user_id must match auth.uid()';
  end if;

  if not public.owns_account(new.account_id) then
    raise exception 'account ownership mismatch';
  end if;

  if new.category_id is not null and not public.owns_category(new.category_id) then
    raise exception 'category ownership mismatch';
  end if;

  if new.debt_id is not null and not public.owns_debt(new.debt_id) then
    raise exception 'debt ownership mismatch';
  end if;

  if new.movement_type = 'transfer' then
    if new.transfer_account_id is null then
      raise exception 'transfer movements require transfer_account_id';
    end if;
    if new.transfer_account_id = new.account_id then
      raise exception 'transfer_account_id must differ from account_id';
    end if;
    if not public.owns_account(new.transfer_account_id) then
      raise exception 'transfer account ownership mismatch';
    end if;
  else
    new.transfer_account_id = null;
  end if;

  if new.movement_type in ('income', 'expense', 'payment', 'transfer') and new.amount <= 0 then
    raise exception 'amount must be positive for this movement_type';
  end if;

  return new;
end;
$$;

create or replace function public.apply_movement_balance()
returns trigger
language plpgsql
as $$
declare
  source_delta numeric(18,2);
  dest_delta numeric(18,2);
begin
  if tg_op = 'INSERT' then
    if new.deleted_at is not null then
      return new;
    end if;
    source_delta := public.movement_effect(new.movement_type, new.amount);
    update public.accounts
    set current_balance = current_balance + source_delta
    where id = new.account_id;

    if new.movement_type = 'transfer' then
      dest_delta := abs(new.amount);
      update public.accounts
      set current_balance = current_balance + dest_delta
      where id = new.transfer_account_id;
    end if;

    return new;
  elsif tg_op = 'UPDATE' then
    if old.deleted_at is null and new.deleted_at is not null then
      source_delta := public.movement_effect(old.movement_type, old.amount) * -1;
      update public.accounts
      set current_balance = current_balance + source_delta
      where id = old.account_id;

      if old.movement_type = 'transfer' then
        update public.accounts
        set current_balance = current_balance - abs(old.amount)
        where id = old.transfer_account_id;
      end if;

      return new;
    elsif old.deleted_at is not null and new.deleted_at is null then
      source_delta := public.movement_effect(new.movement_type, new.amount);
      update public.accounts
      set current_balance = current_balance + source_delta
      where id = new.account_id;

      if new.movement_type = 'transfer' then
        update public.accounts
        set current_balance = current_balance + abs(new.amount)
        where id = new.transfer_account_id;
      end if;

      return new;
    elsif old.deleted_at is not null and new.deleted_at is not null then
      return new;
    else
      source_delta := public.movement_effect(old.movement_type, old.amount) * -1;
      update public.accounts
      set current_balance = current_balance + source_delta
      where id = old.account_id;

      if old.movement_type = 'transfer' then
        update public.accounts
        set current_balance = current_balance - abs(old.amount)
        where id = old.transfer_account_id;
      end if;

      source_delta := public.movement_effect(new.movement_type, new.amount);
      update public.accounts
      set current_balance = current_balance + source_delta
      where id = new.account_id;

      if new.movement_type = 'transfer' then
        update public.accounts
        set current_balance = current_balance + abs(new.amount)
        where id = new.transfer_account_id;
      end if;
    end if;

    return new;
  else
    source_delta := public.movement_effect(old.movement_type, old.amount) * -1;
    update public.accounts
    set current_balance = current_balance + source_delta
    where id = old.account_id;

    if old.movement_type = 'transfer' then
      update public.accounts
      set current_balance = current_balance - abs(old.amount)
      where id = old.transfer_account_id;
    end if;

    return old;
  end if;
end;
$$;

create or replace function public.sync_installment_payment()
returns trigger
language plpgsql
as $$
begin
  if new.paid = true and (tg_op = 'INSERT' or old.paid is distinct from true) then
    new.paid_at = coalesce(new.paid_at, now());
  elsif new.paid = false then
    new.paid_at = null;
  end if;
  return new;
end;
$$;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
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
    '{}'::jsonb,
    '{}'::jsonb
  )
  on conflict (user_id) do nothing;

  return new;
end;
$$;

-- Profiles
create table if not exists public.profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  full_name citext not null,
  avatar_url text,
  locale text not null default 'es-MX',
  timezone text not null default 'America/Mexico_City',
  currency_code char(3) not null default 'MXN',
  settings jsonb not null default '{}'::jsonb,
  preferences jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Accounts
create table if not exists public.accounts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name citext not null,
  account_type public.account_type not null,
  bank_name citext,
  current_balance numeric(18,2) not null default 0,
  credit_limit numeric(18,2),
  statement_close_day smallint check (statement_close_day between 1 and 31),
  payment_due_day smallint check (payment_due_day between 1 and 31),
  is_active boolean not null default true,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Categories
create table if not exists public.categories (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name citext not null,
  icon text,
  color text,
  category_type public.category_type not null default 'expense',
  is_system boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Debts
create table if not exists public.debts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  account_id uuid references public.accounts(id) on delete set null,
  name citext not null,
  debt_type public.debt_type not null,
  original_amount numeric(18,2) not null check (original_amount > 0),
  remaining_balance numeric(18,2) not null check (remaining_balance >= 0),
  interest_rate numeric(7,4) not null default 0 check (interest_rate >= 0),
  fixed_payment numeric(18,2),
  minimum_payment numeric(18,2),
  payment_to_avoid_interest numeric(18,2),
  total_installments integer check (total_installments > 0),
  remaining_installments integer check (remaining_installments >= 0),
  statement_close_day smallint check (statement_close_day between 1 and 31),
  due_day smallint check (due_day between 1 and 31),
  start_date date not null default current_date,
  end_date date,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Movements
create table if not exists public.movements (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  account_id uuid not null references public.accounts(id) on delete restrict,
  transfer_account_id uuid references public.accounts(id) on delete restrict,
  category_id uuid references public.categories(id) on delete set null,
  debt_id uuid references public.debts(id) on delete set null,
  movement_type public.movement_type not null,
  amount numeric(18,2) not null,
  description text,
  movement_date date not null default current_date,
  tags text[] not null default '{}'::text[],
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1,
  constraint movements_amount_check check (
    (movement_type = 'adjustment' and amount <> 0)
    or (movement_type <> 'adjustment' and amount > 0)
  )
);

-- Installments
create table if not exists public.installments (
  id uuid primary key default gen_random_uuid(),
  debt_id uuid not null references public.debts(id) on delete cascade,
  number integer not null check (number > 0),
  amount numeric(18,2) not null check (amount > 0),
  due_date date not null,
  paid boolean not null default false,
  paid_at timestamptz,
  payment_movement_id uuid references public.movements(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Scheduled payments
create table if not exists public.scheduled_payments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  account_id uuid references public.accounts(id) on delete set null,
  category_id uuid references public.categories(id) on delete set null,
  name citext not null,
  amount numeric(18,2) not null check (amount > 0),
  frequency public.payment_frequency not null,
  next_date date not null,
  last_date date,
  mandatory boolean not null default false,
  active boolean not null default true,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Financial goals
create table if not exists public.financial_goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name citext not null,
  target_amount numeric(18,2) not null check (target_amount > 0),
  current_progress numeric(18,2) not null default 0 check (current_progress >= 0),
  target_date date,
  status public.goal_status not null default 'active',
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Budgets
create table if not exists public.budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  category_id uuid not null references public.categories(id) on delete cascade,
  budget_period public.budget_period not null,
  period_start date not null,
  period_end date not null,
  amount_limit numeric(18,2) not null check (amount_limit > 0),
  alert_threshold numeric(5,4) not null default 0.8000 check (alert_threshold > 0 and alert_threshold <= 1),
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  row_version bigint not null default 1
);

-- Notifications
create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  type public.notification_type not null,
  message text not null,
  payload jsonb not null default '{}'::jsonb,
  read_at timestamptz,
  created_at timestamptz not null default now()
);

-- Audit log
create table if not exists public.audit_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid,
  table_name text not null,
  record_id uuid,
  action text not null check (action in ('insert', 'update', 'delete')),
  old_data jsonb,
  new_data jsonb,
  changed_by uuid default auth.uid(),
  changed_at timestamptz not null default now()
);

-- Ownership helpers depend on the tables above
create or replace function public.owns_account(p_account_id uuid)
returns boolean
language sql
stable
as $$
  select exists (
    select 1
    from public.accounts a
    where a.id = p_account_id
      and a.user_id = auth.uid()
      and a.deleted_at is null
  );
$$;

create or replace function public.owns_category(p_category_id uuid)
returns boolean
language sql
stable
as $$
  select exists (
    select 1
    from public.categories c
    where c.id = p_category_id
      and c.user_id = auth.uid()
      and c.deleted_at is null
  );
$$;

create or replace function public.owns_debt(p_debt_id uuid)
returns boolean
language sql
stable
as $$
  select exists (
    select 1
    from public.debts d
    where d.id = p_debt_id
      and d.user_id = auth.uid()
      and d.deleted_at is null
  );
$$;

-- Useful indexes
create unique index if not exists profiles_user_id_idx on public.profiles (user_id);

create index if not exists accounts_user_id_active_idx
  on public.accounts (user_id, is_active, deleted_at, updated_at desc);

create unique index if not exists accounts_user_name_unique
  on public.accounts (user_id, name)
  where deleted_at is null;

create unique index if not exists categories_user_name_type_unique
  on public.categories (user_id, category_type, name)
  where deleted_at is null;

create index if not exists categories_user_idx
  on public.categories (user_id, category_type, deleted_at);

create index if not exists movements_user_date_idx
  on public.movements (user_id, movement_date desc, created_at desc)
  where deleted_at is null;

create index if not exists movements_account_date_idx
  on public.movements (account_id, movement_date desc)
  where deleted_at is null;

create index if not exists movements_category_date_idx
  on public.movements (category_id, movement_date desc)
  where deleted_at is null;

create index if not exists movements_debt_date_idx
  on public.movements (debt_id, movement_date desc)
  where deleted_at is null;

create index if not exists movements_tags_gin_idx
  on public.movements using gin (tags);

create index if not exists debts_user_status_idx
  on public.debts (user_id, debt_type, deleted_at, updated_at desc);

create index if not exists installments_debt_due_idx
  on public.installments (debt_id, due_date, paid)
  where deleted_at is null;

create unique index if not exists installments_debt_number_unique
  on public.installments (debt_id, number)
  where deleted_at is null;

create index if not exists scheduled_payments_next_idx
  on public.scheduled_payments (user_id, active, next_date)
  where deleted_at is null;

create index if not exists goals_user_status_idx
  on public.financial_goals (user_id, status, deleted_at);

create index if not exists budgets_user_period_idx
  on public.budgets (user_id, budget_period, period_start, period_end)
  where deleted_at is null;

create index if not exists notifications_user_read_idx
  on public.notifications (user_id, read_at, created_at desc);

create index if not exists audit_log_user_table_idx
  on public.audit_log (user_id, table_name, changed_at desc);

-- Triggers for timestamps and validation
create trigger trg_profiles_touch
before update on public.profiles
for each row execute function public.touch_updated_at();

create trigger trg_accounts_validate
before insert or update on public.accounts
for each row execute function public.validate_account_payload();

create trigger trg_accounts_touch
before update on public.accounts
for each row execute function public.touch_updated_at();

create trigger trg_categories_validate
before insert or update on public.categories
for each row execute function public.validate_category_payload();

create trigger trg_categories_touch
before update on public.categories
for each row execute function public.touch_updated_at();

create trigger trg_debts_validate
before insert or update on public.debts
for each row execute function public.validate_debt_payload();

create trigger trg_debts_touch
before update on public.debts
for each row execute function public.touch_updated_at();

create trigger trg_movements_validate
before insert or update on public.movements
for each row execute function public.validate_movement_payload();

create trigger trg_movements_touch
before update on public.movements
for each row execute function public.touch_updated_at();

create trigger trg_movements_balance
after insert or update or delete on public.movements
for each row execute function public.apply_movement_balance();

create trigger trg_installments_touch
before update on public.installments
for each row execute function public.touch_updated_at();

create trigger trg_installments_paid_at
before insert or update on public.installments
for each row execute function public.sync_installment_payment();

create trigger trg_on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

create trigger trg_scheduled_payments_touch
before update on public.scheduled_payments
for each row execute function public.touch_updated_at();

create trigger trg_goals_touch
before update on public.financial_goals
for each row execute function public.touch_updated_at();

create trigger trg_budgets_touch
before update on public.budgets
for each row execute function public.touch_updated_at();

-- Row Level Security
alter table public.profiles enable row level security;
alter table public.accounts enable row level security;
alter table public.categories enable row level security;
alter table public.debts enable row level security;
alter table public.movements enable row level security;
alter table public.installments enable row level security;
alter table public.scheduled_payments enable row level security;
alter table public.financial_goals enable row level security;
alter table public.budgets enable row level security;
alter table public.notifications enable row level security;
alter table public.audit_log enable row level security;

-- Profiles policies
create policy profiles_select_own on public.profiles
for select using (public.is_owner(user_id));

create policy profiles_insert_own on public.profiles
for insert with check (public.is_owner(user_id));

create policy profiles_update_own on public.profiles
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy profiles_delete_own on public.profiles
for delete using (public.is_owner(user_id));

-- Accounts policies
create policy accounts_select_own on public.accounts
for select using (public.is_owner(user_id) and deleted_at is null);

create policy accounts_insert_own on public.accounts
for insert with check (public.is_owner(user_id));

create policy accounts_update_own on public.accounts
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy accounts_delete_own on public.accounts
for delete using (public.is_owner(user_id));

-- Categories policies
create policy categories_select_own on public.categories
for select using (public.is_owner(user_id) and deleted_at is null);

create policy categories_insert_own on public.categories
for insert with check (public.is_owner(user_id));

create policy categories_update_own on public.categories
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy categories_delete_own on public.categories
for delete using (public.is_owner(user_id));

-- Debts policies
create policy debts_select_own on public.debts
for select using (public.is_owner(user_id) and deleted_at is null);

create policy debts_insert_own on public.debts
for insert with check (public.is_owner(user_id));

create policy debts_update_own on public.debts
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy debts_delete_own on public.debts
for delete using (public.is_owner(user_id));

-- Movements policies
create policy movements_select_own on public.movements
for select using (public.is_owner(user_id) and deleted_at is null);

create policy movements_insert_own on public.movements
for insert with check (public.is_owner(user_id));

create policy movements_update_own on public.movements
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy movements_delete_own on public.movements
for delete using (public.is_owner(user_id));

-- Installments policies
create policy installments_select_own on public.installments
for select using (public.owns_debt(debt_id) and deleted_at is null);

create policy installments_insert_own on public.installments
for insert with check (public.owns_debt(debt_id));

create policy installments_update_own on public.installments
for update using (public.owns_debt(debt_id)) with check (public.owns_debt(debt_id));

create policy installments_delete_own on public.installments
for delete using (public.owns_debt(debt_id));

-- Scheduled payments policies
create policy scheduled_payments_select_own on public.scheduled_payments
for select using (public.is_owner(user_id) and deleted_at is null);

create policy scheduled_payments_insert_own on public.scheduled_payments
for insert with check (public.is_owner(user_id));

create policy scheduled_payments_update_own on public.scheduled_payments
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy scheduled_payments_delete_own on public.scheduled_payments
for delete using (public.is_owner(user_id));

-- Goals policies
create policy goals_select_own on public.financial_goals
for select using (public.is_owner(user_id) and deleted_at is null);

create policy goals_insert_own on public.financial_goals
for insert with check (public.is_owner(user_id));

create policy goals_update_own on public.financial_goals
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy goals_delete_own on public.financial_goals
for delete using (public.is_owner(user_id));

-- Budgets policies
create policy budgets_select_own on public.budgets
for select using (public.is_owner(user_id) and deleted_at is null);

create policy budgets_insert_own on public.budgets
for insert with check (public.is_owner(user_id));

create policy budgets_update_own on public.budgets
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy budgets_delete_own on public.budgets
for delete using (public.is_owner(user_id));

-- Notifications policies
create policy notifications_select_own on public.notifications
for select using (public.is_owner(user_id));

create policy notifications_insert_own on public.notifications
for insert with check (public.is_owner(user_id));

create policy notifications_update_own on public.notifications
for update using (public.is_owner(user_id)) with check (public.is_owner(user_id));

create policy notifications_delete_own on public.notifications
for delete using (public.is_owner(user_id));

-- Audit log policies
create policy audit_log_select_own on public.audit_log
for select using (public.is_owner(user_id));

-- Views
create or replace view public.v_account_summary as
select
  a.user_id,
  a.id as account_id,
  a.name,
  a.account_type,
  a.bank_name,
  a.current_balance,
  a.credit_limit,
  a.is_active,
  count(m.id) filter (where m.deleted_at is null) as movement_count,
  max(m.movement_date) as last_movement_date
from public.accounts a
left join public.movements m on m.account_id = a.id and m.deleted_at is null
where a.deleted_at is null
group by a.user_id, a.id;

create or replace view public.v_monthly_cashflow as
select
  m.user_id,
  date_trunc('month', m.movement_date::timestamp)::date as period_month,
  sum(case when m.movement_type = 'income' then m.amount else 0 end) as income_total,
  sum(case when m.movement_type in ('expense', 'payment') then m.amount else 0 end) as expense_total,
  sum(case when m.movement_type = 'adjustment' and m.amount > 0 then m.amount else 0 end) as adjustment_up_total,
  sum(case when m.movement_type = 'adjustment' and m.amount < 0 then abs(m.amount) else 0 end) as adjustment_down_total
from public.movements m
where m.deleted_at is null
group by m.user_id, date_trunc('month', m.movement_date::timestamp)::date;

create or replace view public.v_quincenal_cashflow as
with movement_periods as (
  select
    m.user_id,
    (date_trunc('month', m.movement_date::timestamp)::date +
      case when extract(day from m.movement_date) <= 15 then 0 else 15 end) as period_start,
    case when extract(day from m.movement_date) <= 15
      then (date_trunc('month', m.movement_date::timestamp)::date + 14)
      else (date_trunc('month', m.movement_date::timestamp) + interval '1 month - 1 day')::date
    end as period_end,
    m.movement_type,
    m.amount
  from public.movements m
  where m.deleted_at is null
)
select
  user_id,
  period_start,
  period_end,
  sum(case when movement_type = 'income' then amount else 0 end) as income_total,
  sum(case when movement_type in ('expense', 'payment') then amount else 0 end) as expense_total
from movement_periods
group by user_id, period_start, period_end;

create or replace view public.v_debt_overview as
select
  d.user_id,
  d.id as debt_id,
  d.name,
  d.debt_type,
  d.original_amount,
  d.remaining_balance,
  d.interest_rate,
  d.fixed_payment,
  d.minimum_payment,
  d.payment_to_avoid_interest,
  d.total_installments,
  d.remaining_installments,
  count(i.id) filter (where i.deleted_at is null) as installment_count,
  count(i.id) filter (where i.paid = true and i.deleted_at is null) as paid_installments
from public.debts d
left join public.installments i on i.debt_id = d.id
where d.deleted_at is null
group by d.user_id, d.id;

create or replace view public.v_goal_progress as
select
  g.user_id,
  g.id as goal_id,
  g.name,
  g.target_amount,
  g.current_progress,
  round(case when g.target_amount = 0 then 0 else (g.current_progress / g.target_amount) * 100 end, 2) as progress_percent,
  g.target_date,
  g.status
from public.financial_goals g
where g.deleted_at is null;

create or replace view public.v_budget_usage as
select
  b.user_id,
  b.id as budget_id,
  b.category_id,
  b.amount_limit,
  b.period_start,
  b.period_end,
  coalesce(sum(m.amount), 0) as spent_amount,
  round(case when b.amount_limit = 0 then 0 else (coalesce(sum(m.amount), 0) / b.amount_limit) * 100 end, 2) as usage_percent
from public.budgets b
left join public.movements m
  on m.category_id = b.category_id
 and m.movement_type in ('expense', 'payment')
 and m.movement_date between b.period_start and b.period_end
 and m.deleted_at is null
where b.deleted_at is null
group by b.user_id, b.id;

-- RPC for registering a payment against a debt
create or replace function public.register_debt_payment(
  p_debt_id uuid,
  p_account_id uuid,
  p_amount numeric,
  p_movement_date date default current_date,
  p_description text default null,
  p_installment_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
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

-- RPC for soft delete pattern
create or replace function public.soft_delete_entity(p_table text, p_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
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
