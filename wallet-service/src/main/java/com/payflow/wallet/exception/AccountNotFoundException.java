package com.payflow.wallet.exception;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long userId) {
        super("Cuenta no encontrada para el usuario: " + userId);
    }
    public AccountNotFoundException(String accountNumber) {
        super("Cuenta no encontrada: " + accountNumber);
    }
}
