package com.venn.LoadManager.dao;

import com.venn.LoadManager.domain.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface CustomerTransactionDao extends JpaRepository<CustomerTransaction, Long> {

    boolean existsByCustomerIdAndLoadId(String customerId, String loadId);

    @Query("SELECT COALESCE(SUM(c.amountCents),0) FROM CustomerTransaction c " +
            "WHERE c.customerId = :customerId AND c.accepted = true AND c.time >= :start AND c.time < :end")
    long getAcceptedAmountSum(String customerId, Instant start, Instant end);

    @Query("SELECT COUNT(c) FROM CustomerTransaction c " +
            "WHERE c.customerId = :customerId AND c.accepted = true AND c.time >= :start AND c.time < :end")
    long getAcceptedTransactionCount(String customerId, Instant start, Instant end);
}
