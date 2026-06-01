# Curl examples for the finance backend

Use `curl.exe` on Windows PowerShell.

```powershell
$baseUrl = "http://127.0.0.1:8080"
$accessToken = "<ACCESS_TOKEN>"
$refreshToken = "<REFRESH_TOKEN>"
```

All routes except `/auth/**` require:

```powershell
-H "Authorization: Bearer $accessToken"
```

## Auth

Sign up:

```powershell
curl.exe -X POST "$baseUrl/auth/sign-up" `
  -H "Content-Type: application/json" `
  -d '{"email":"ana@example.com","password":"Secret123!","displayName":"Ana Perez"}'
```

Sign in:

```powershell
curl.exe -X POST "$baseUrl/auth/sign-in" `
  -H "Content-Type: application/json" `
  -d '{"email":"ana@example.com","password":"Secret123!"}'
```

Refresh session:

```powershell
curl.exe -X POST "$baseUrl/auth/refresh" `
  -H "Content-Type: application/json" `
  -d "{\"refreshToken\":\"$refreshToken\"}"
```

Sign out:

```powershell
curl.exe -X POST "$baseUrl/auth/sign-out" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d "{\"refreshToken\":\"$refreshToken\"}"
```

Current session:

```powershell
curl.exe "$baseUrl/auth/session" `
  -H "Authorization: Bearer $accessToken" `
  -H "X-Refresh-Token: $refreshToken"
```

## Profile

Get profile:

```powershell
curl.exe "$baseUrl/me" `
  -H "Authorization: Bearer $accessToken"
```

Update profile:

```powershell
curl.exe -X PATCH "$baseUrl/me" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"displayName":"Ana Perez","currency":"MXN","payCycle":"quincenal","payDays":[1,15]}'
```

## Categories

List categories:

```powershell
curl.exe "$baseUrl/categories" `
  -H "Authorization: Bearer $accessToken"
```

Create category:

```powershell
curl.exe -X POST "$baseUrl/categories" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"name":"Comida","color":"#ff7a00","icon":"utensils","type":"expense"}'
```

Update category:

```powershell
curl.exe -X PATCH "$baseUrl/categories/<CATEGORY_ID>" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"name":"Comida","color":"#ff7a00","icon":"utensils","type":"expense"}'
```

Delete category:

```powershell
curl.exe -X DELETE "$baseUrl/categories/<CATEGORY_ID>" `
  -H "Authorization: Bearer $accessToken"
```

## Accounts

List accounts:

```powershell
curl.exe "$baseUrl/accounts" `
  -H "Authorization: Bearer $accessToken"
```

Create account:

```powershell
curl.exe -X POST "$baseUrl/accounts" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"type":"checking","name":"Cuenta principal","institution":"BBVA","currency":"MXN","balance":1500.50,"creditLimit":0,"closingDay":null,"dueDay":null,"active":true}'
```

Update account:

```powershell
curl.exe -X PATCH "$baseUrl/accounts/<ACCOUNT_ID>" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"type":"checking","name":"Cuenta principal","institution":"BBVA","currency":"MXN","balance":1700.75,"creditLimit":0,"closingDay":null,"dueDay":null,"active":true}'
```

Delete account:

```powershell
curl.exe -X DELETE "$baseUrl/accounts/<ACCOUNT_ID>" `
  -H "Authorization: Bearer $accessToken"
```

## Transactions

List transactions:

```powershell
curl.exe "$baseUrl/transactions?from=2026-05-01&to=2026-05-31&limit=50" `
  -H "Authorization: Bearer $accessToken"
```

Create transaction:

```powershell
curl.exe -X POST "$baseUrl/transactions" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"accountId":"<ACCOUNT_ID>","transferAccountId":null,"categoryId":"<CATEGORY_ID>","type":"expense","description":"Super","amount":320.45,"currency":"MXN","transactionDate":"2026-05-31","notes":"Compra semanal"}'
```

Update transaction:

```powershell
curl.exe -X PATCH "$baseUrl/transactions/<TRANSACTION_ID>" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"accountId":"<ACCOUNT_ID>","transferAccountId":null,"categoryId":"<CATEGORY_ID>","type":"expense","description":"Super","amount":350.00,"currency":"MXN","transactionDate":"2026-05-31","notes":"Compra semanal"}'
```

Delete transaction:

```powershell
curl.exe -X DELETE "$baseUrl/transactions/<TRANSACTION_ID>" `
  -H "Authorization: Bearer $accessToken"
```

## Debts

List debts:

```powershell
curl.exe "$baseUrl/debts" `
  -H "Authorization: Bearer $accessToken"
```

Create debt:

```powershell
curl.exe -X POST "$baseUrl/debts" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"name":"Tarjeta","principalBalance":5000.00,"installment":500.00,"frequency":"monthly","nextDueDate":"2026-06-15","notes":"Pago mensual"}'
```

Update debt:

```powershell
curl.exe -X PATCH "$baseUrl/debts/<DEBT_ID>" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"name":"Tarjeta","principalBalance":4500.00,"installment":500.00,"frequency":"monthly","nextDueDate":"2026-06-15","notes":"Pago mensual"}'
```

Delete debt:

```powershell
curl.exe -X DELETE "$baseUrl/debts/<DEBT_ID>" `
  -H "Authorization: Bearer $accessToken"
```

## Recurring payments

List recurring payments:

```powershell
curl.exe "$baseUrl/recurring-payments" `
  -H "Authorization: Bearer $accessToken"
```

Create recurring payment:

```powershell
curl.exe -X POST "$baseUrl/recurring-payments" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"name":"Netflix","amount":229.00,"currency":"MXN","frequency":"monthly","nextDueDate":"2026-06-05","categoryId":"<CATEGORY_ID>"}'
```

Update recurring payment:

```powershell
curl.exe -X PATCH "$baseUrl/recurring-payments/<RECURRING_PAYMENT_ID>" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"name":"Netflix","amount":249.00,"currency":"MXN","frequency":"monthly","nextDueDate":"2026-06-05","categoryId":"<CATEGORY_ID>"}'
```

Delete recurring payment:

```powershell
curl.exe -X DELETE "$baseUrl/recurring-payments/<RECURRING_PAYMENT_ID>" `
  -H "Authorization: Bearer $accessToken"
```

## Sync

Pull sync:

```powershell
curl.exe "$baseUrl/sync/pull?since=2026-05-31T00:00:00Z&entity=transactions" `
  -H "Authorization: Bearer $accessToken"
```

Push sync:

```powershell
curl.exe -X POST "$baseUrl/sync/push" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{
    "deviceId":"desktop-1",
    "changes":[
      {
        "entity":"category",
        "op":"upsert",
        "record":{
          "id":"cat-001",
          "name":"Comida",
          "color":"#ff7a00",
          "icon":"utensils",
          "type":"expense"
        }
      }
    ]
  }'
```

Resolve conflict:

```powershell
curl.exe -X POST "$baseUrl/sync/resolve-conflict" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{"entity":"category","id":"cat-001","resolution":"client_wins"}'
```

## Migration

Import local data:

```powershell
curl.exe -X POST "$baseUrl/migration/import-local" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{
    "source":"indexeddb",
    "schemaVersion":1,
    "payload":{
      "categories":[],
      "accounts":[],
      "transactions":[],
      "debts":[],
      "recurringPayments":[]
    }
  }'
```

## Stats

Summary:

```powershell
curl.exe "$baseUrl/stats/summary?range=monthly" `
  -H "Authorization: Bearer $accessToken"
```

Category stats:

```powershell
curl.exe "$baseUrl/stats/categories?range=monthly" `
  -H "Authorization: Bearer $accessToken"
```

Upcoming:

```powershell
curl.exe "$baseUrl/stats/upcoming" `
  -H "Authorization: Bearer $accessToken"
```

## Backup

Export backup:

```powershell
curl.exe "$baseUrl/backup/export?format=json&encrypted=false" `
  -H "Authorization: Bearer $accessToken"
```

Import backup:

```powershell
curl.exe -X POST "$baseUrl/backup/import" `
  -H "Authorization: Bearer $accessToken" `
  -H "Content-Type: application/json" `
  -d '{
    "format":"json",
    "encrypted":false,
    "payload":{
      "note":"example backup payload"
    }
  }'
```

## Notes

- Replace `<ACCESS_TOKEN>` and `<REFRESH_TOKEN>` with the values returned by `/auth/sign-in` or `/auth/sign-up`.
- Replace the ID placeholders with real IDs from the create/list responses.
- `type` values used in transactions should match your business rules. The current backend accepts `income`, `expense`, and `payment` in its queries.
- `backup/export`, `backup/import`, and `migration/import-local` currently return simplified placeholder responses in this backend scaffold.
