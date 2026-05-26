package com.resolvehub.common.validation;

public final class PasswordPolicy {

    public static final String PASSWORD_REGEX =
            "^(?=\\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$";

    public static final String MESSAGE =
            "Password must be at least 8 characters and include uppercase, lowercase, number, special character, and no spaces.";

    private PasswordPolicy() {
    }
}
