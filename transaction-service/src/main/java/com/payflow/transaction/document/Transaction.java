package com.payflow.transaction.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    @Indexed
    private Long userId;

    @Indexed
    private Long counterpartyUserId;

    private String accountNumber;
    private String counterpartyAccountNumber;

    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private String referenceId;   // transferId de transfer-service
    private TransactionStatus status;
    private LocalDateTime createdAt;

    public enum TransactionType  { CREDIT, DEBIT, TOPUP }
    public enum TransactionStatus { COMPLETED, FAILED, PENDING }

    // ── Constructors ──────────────────────────────────────────
    public Transaction() {}

    // ── Static factories ──────────────────────────────────────
    public static Transaction credit(Long userId, Long fromUserId,
                                     String accountNumber,
                                     String fromAccountNumber,
                                     BigDecimal amount, BigDecimal balanceAfter,
                                     String description, String referenceId) {
        Transaction t = new Transaction();
        t.userId                   = userId;
        t.counterpartyUserId       = fromUserId;
        t.accountNumber            = accountNumber;
        t.counterpartyAccountNumber= fromAccountNumber;
        t.type                     = TransactionType.CREDIT;
        t.amount                   = amount;
        t.balanceAfter             = balanceAfter;
        t.description              = description;
        t.referenceId              = referenceId;
        t.status                   = TransactionStatus.COMPLETED;
        t.createdAt                = LocalDateTime.now();
        return t;
    }

    public static Transaction debit(Long userId, Long toUserId,
                                    String accountNumber,
                                    String toAccountNumber,
                                    BigDecimal amount, BigDecimal balanceAfter,
                                    String description, String referenceId) {
        Transaction t = new Transaction();
        t.userId                   = userId;
        t.counterpartyUserId       = toUserId;
        t.accountNumber            = accountNumber;
        t.counterpartyAccountNumber= toAccountNumber;
        t.type                     = TransactionType.DEBIT;
        t.amount                   = amount;
        t.balanceAfter             = balanceAfter;
        t.description              = description;
        t.referenceId              = referenceId;
        t.status                   = TransactionStatus.COMPLETED;
        t.createdAt                = LocalDateTime.now();
        return t;
    }

    public static Transaction topup(Long userId, String accountNumber,
                                    BigDecimal amount, BigDecimal balanceAfter,
                                    String description) {
        Transaction t = new Transaction();
        t.userId        = userId;
        t.accountNumber = accountNumber;
        t.type          = TransactionType.TOPUP;
        t.amount        = amount;
        t.balanceAfter  = balanceAfter;
        t.description   = description;
        t.referenceId   = "TOPUP-" + System.currentTimeMillis();
        t.status        = TransactionStatus.COMPLETED;
        t.createdAt     = LocalDateTime.now();
        return t;
    }

    // ── Getters ───────────────────────────────────────────────
    public String getId()                        { return id; }
    public Long getUserId()                      { return userId; }
    public Long getCounterpartyUserId()          { return counterpartyUserId; }
    public String getAccountNumber()             { return accountNumber; }
    public String getCounterpartyAccountNumber() { return counterpartyAccountNumber; }
    public TransactionType getType()             { return type; }
    public BigDecimal getAmount()                { return amount; }
    public BigDecimal getBalanceAfter()          { return balanceAfter; }
    public String getDescription()               { return description; }
    public String getReferenceId()               { return referenceId; }
    public TransactionStatus getStatus()         { return status; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
}