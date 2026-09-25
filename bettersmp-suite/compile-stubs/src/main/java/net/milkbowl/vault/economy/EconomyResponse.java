package net.milkbowl.vault.economy;

/**
 * Compile-only stub of Vault's EconomyResponse.
 * Field and method signatures match the real class; at runtime the real
 * class from the installed Vault plugin is used (scope: provided).
 */
public class EconomyResponse {

    public enum ResponseType {
        SUCCESS(1),
        FAILURE(2),
        NOT_IMPLEMENTED(3);

        private final int id;

        ResponseType(int id) {
            this.id = id;
        }
    }

    public final double amount;
    public final double balance;
    public final ResponseType type;
    public final String errorMessage;

    public EconomyResponse(double amount, double balance, ResponseType type, String errorMessage) {
        this.amount = amount;
        this.balance = balance;
        this.type = type;
        this.errorMessage = errorMessage;
    }

    public boolean transactionSuccess() {
        return type == ResponseType.SUCCESS;
    }
}
