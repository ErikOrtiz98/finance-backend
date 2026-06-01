# Postman setup

Import these two files into Postman:

- `FinanceBackend.postman_collection.json`
- `FinanceLocal.postman_environment.json`

Then:

1. Select the `Finance Local` environment.
2. Run `Auth -> Sign In` first. If needed, run `Auth -> Sign Up` once before that.
3. The tests in the auth requests save `accessToken`, `refreshToken`, and `userId` automatically.
4. Create a category and an account before creating transactions.
5. The create requests save `categoryId`, `accountId`, `transactionId`, `debtId`, and `recurringPaymentId` automatically.
6. `Auth -> Sign Out` now uses the `accessToken` in the `Authorization` header, plus the refresh token in the body for compatibility.

Base URL for the local backend:

- `http://127.0.0.1:8080`
