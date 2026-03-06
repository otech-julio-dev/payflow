package com.payflow.transfer.repository;

import com.payflow.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t WHERE t.senderUserId = :userId " +
           "OR t.receiverUserId = :userId ORDER BY t.createdAt DESC")
    List<Transfer> findAllByUserId(@Param("userId") Long userId);

    List<Transfer> findBySenderUserIdOrderByCreatedAtDesc(Long userId);
}