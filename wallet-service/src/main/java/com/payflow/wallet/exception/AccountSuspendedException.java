package com.payflow.wallet.exception;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException(String accountNumber) {
        super("La cuenta está suspendida: " + accountNumber);
    }
}
