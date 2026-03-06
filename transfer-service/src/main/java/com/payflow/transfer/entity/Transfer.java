package com.payflow.transfer.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderUserId;

    @Column(nullable = false)
    private Long receiverUserId;

    @Column(nullable = false)
    private String senderAccountNumber;

    @Column(nullable = false)
    private String receiverAccountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime completedAt;

    @Column
    private String failureReason;

    public enum TransferStatus { PENDING, COMPLETED, FAILED }

    // ── Constructors ──────────────────────────────────────────
    public Transfer() {}

    // ── Business methods ──────────────────────────────────────
    public void complete() {
        this.status      = TransferStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        this.status        = TransferStatus.FAILED;
        this.failureReason = reason;
        this.completedAt   = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────
    public Long getId()                          { return id; }
    public Long getSenderUserId()                { return senderUserId; }
    public void setSenderUserId(Long v)          { this.senderUserId = v; }
    public Long getReceiverUserId()              { return receiverUserId; }
    public void setReceiverUserId(Long v)        { this.receiverUserId = v; }
    public String getSenderAccountNumber()       { return senderAccountNumber; }
    public void setSenderAccountNumber(String v) { this.senderAccountNumber = v; }
    public String getReceiverAccountNumber()     { return receiverAccountNumber; }
    public void setReceiverAccountNumber(String v){ this.receiverAccountNumber = v; }
    public BigDecimal getAmount()                { return amount; }
    public void setAmount(BigDecimal v)          { this.amount = v; }
    public String getDescription()               { return description; }
    public void setDescription(String v)         { this.description = v; }
    public TransferStatus getStatus()            { return status; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getCompletedAt()        { return completedAt; }
    public String getFailureReason()             { return failureReason; }
}