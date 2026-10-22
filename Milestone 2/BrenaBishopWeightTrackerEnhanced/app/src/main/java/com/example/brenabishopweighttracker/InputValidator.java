package com.example.brenabishopweighttracker;

/**
 * Centralizes input validation so UI code does not have to duplicate
 * parsing and business-rule checks.
 */
public final class InputValidator {

    private InputValidator() {
        // Utility class.
    }

    public static String validateCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return "Enter a username and password.";
        }

        return null;
    }

    public static NumericValidationResult validatePositiveNumber(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            return NumericValidationResult.invalid("Enter " + fieldName + ".");
        }

        try {
            double value = Double.parseDouble(input.trim());

            if (!Double.isFinite(value) || value <= 0) {
                return NumericValidationResult.invalid(
                        fieldName + " must be greater than zero."
                );
            }

            return NumericValidationResult.valid(value);
        } catch (NumberFormatException exception) {
            return NumericValidationResult.invalid(
                    "Enter a valid number for " + fieldName + "."
            );
        }
    }

    /**
     * Carries either a validated numeric value or a user-facing validation message.
     */
    public static final class NumericValidationResult {
        private final boolean valid;
        private final double value;
        private final String errorMessage;

        private NumericValidationResult(boolean valid, double value, String errorMessage) {
            this.valid = valid;
            this.value = value;
            this.errorMessage = errorMessage;
        }

        public static NumericValidationResult valid(double value) {
            return new NumericValidationResult(true, value, null);
        }

        public static NumericValidationResult invalid(String errorMessage) {
            return new NumericValidationResult(false, 0, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public double getValue() {
            return value;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
