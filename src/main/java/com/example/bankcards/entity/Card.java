package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.util.Date;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "card_number_encrypted")
    private String cardNumber;

    @Column(name = "card_number_last_four")
    private String cardNumberLastFour;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CardStatus status;

    @Column(name = "balance")
    private BigDecimal balance;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "holder_name", nullable = false)
    private String cardHolderName;

    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == CardStatus.BLOCKED;
    }
}

