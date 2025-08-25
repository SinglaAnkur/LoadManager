package com.venn.LoadManager.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "CustomerTransaction",
        indexes = {
            @Index(name = "UK_CustomerTransaction_CustomerId_LoadId", columnList = "customerId,loadId", unique = true)
        })
@Getter
@Setter
public class CustomerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String loadId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private long amountCents;

    @Column(nullable = false)
    private Instant time;

    @Column(nullable = false)
    private boolean accepted;

    public CustomerTransaction(String loadId, String customerId, long amountCents, Instant time, boolean accepted) {
        this.loadId = loadId;
        this.customerId = customerId;
        this.amountCents = amountCents;
        this.time = time;
        this.accepted = accepted;
    }
}
