package com.codex.finance.validator;

import java.util.Set;

public class AccountTypeValidator {

	private static final Set<String> VALID_ACCOUNT_TYPES = Set.of("debit", "credit", "savings", "cash", "investment",
			"loan");

	public static boolean isValid(String accountType) {
		return accountType != null && VALID_ACCOUNT_TYPES.contains(accountType);
	}

	public static void validate(String accountType) {
		if (!isValid(accountType)) {
			throw new IllegalArgumentException("Invalid account_type: '" + accountType + "'. Must be one of: "
					+ String.join(", ", VALID_ACCOUNT_TYPES));
		}
	}
}