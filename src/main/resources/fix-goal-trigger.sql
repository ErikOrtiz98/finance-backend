-- Fix: update_goal_progress() auto-incrementaba progreso ficticio en metas
-- Cada ingreso agregaba 10% del ingreso mensual a TODAS las metas activas,
-- dando la ilusion de progreso sin que el usuario ahorrara realmente.
-- El progreso solo debe actualizarse via PATCH /financial-goals/{id} (manual).

CREATE OR REPLACE FUNCTION public.update_goal_progress()
RETURNS trigger
LANGUAGE plpgsql
AS $$
begin
  return new;
end;
$$;
