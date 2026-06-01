<<<<<<< HEAD
# Finance contract backend

Spring Boot backend aligned to the API contract from the pasted file.

## Routes covered

- `POST /auth/sign-up`
- `POST /auth/sign-in`
- `POST /auth/refresh`
- `POST /auth/sign-out`
- `GET /auth/session`
- `GET /me`
- `PATCH /me`
- `GET /categories`
- `POST /categories`
- `PATCH /categories/{id}`
- `DELETE /categories/{id}`
- `GET /accounts`
- `POST /accounts`
- `PATCH /accounts/{id}`
- `DELETE /accounts/{id}`
- `GET /transactions`
- `POST /transactions`
- `PATCH /transactions/{id}`
- `DELETE /transactions/{id}`
- `GET /debts`
- `POST /debts`
- `PATCH /debts/{id}`
- `DELETE /debts/{id}`
- `GET /recurring-payments`
- `POST /recurring-payments`
- `PATCH /recurring-payments/{id}`
- `DELETE /recurring-payments/{id}`
- `GET /sync/pull`
- `POST /sync/push`
- `POST /sync/resolve-conflict`
- `POST /migration/import-local`
- `GET /stats/summary`
- `GET /stats/categories`
- `GET /stats/upcoming`
- `GET /backup/export`
- `POST /backup/import`

## Storage mapping

- `transactions` -> `movements`
- `recurring-payments` -> `scheduled_payments`

## Notes

- JWT auth is expected from Supabase.
- `syncStatus` and `version` are included in API responses.
- This backend assumes the Supabase schema generated earlier in `supabase_finanzas_personales.sql`.
=======
# finance-backend
>>>>>>> e3ca2c82662a2381277afc79d3d9666a3a71a391
