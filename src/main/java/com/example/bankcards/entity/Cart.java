package com.example.bankcards.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Column(name = "cart_number_encrypted")
    private String cartNumber;
    @Column(name = "expiry_date")
    private Date expiredDate;
    @Column(name = "status")
    private Status status;
    @Column(name = "balance")
    private BigDecimal balance;
}
