package com.payflow.wallet.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum AccountStatus { ACTIVE, SUSPENDED, CLOSED }

    // ── Constructors ──────────────────────────────────────────
    public Account() {}

    public Account(Long userId, String accountNumber) {
        this.userId        = userId;
        this.accountNumber = accountNumber;
    }

    // ── Business methods ──────────────────────────────────────
    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        this.balance   = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        this.balance   = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    // ── Getters & Setters ─────────────────────────────────────
    public Long getId()                      { return id; }
    public Long getUserId()                  { return userId; }
    public void setUserId(Long v)            { this.userId = v; }
    public String getAccountNumber()         { return accountNumber; }
    public void setAccountNumber(String v)   { this.accountNumber = v; }
    public BigDecimal getBalance()           { return balance; }
    public AccountStatus getStatus()         { return status; }
    public void setStatus(AccountStatus v)   { this.status = v; }
    public LocalDateTime getCreatedAt()      { return createdAt; }
    public LocalDateTime getUpdatedAt()      { return updatedAt; }
}