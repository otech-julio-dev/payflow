package com.payflow.transaction.repository;

import com.payflow.transaction.document.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository
        extends MongoRepository<Transaction, String> {

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(
            Long userId, Pageable pageable);

    long countByUserId(Long userId);
}