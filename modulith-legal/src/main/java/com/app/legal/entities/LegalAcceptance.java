package com.app.legal.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_acceptances")
@Getter
@Setter
@NoArgsConstructor
public class LegalAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private LegalAgreement agreement;

    @Column(nullable = false)
    private String versionAccepted;

    @Column(nullable = false)
    private LocalDateTime acceptedAt;

    private String ipAddress;
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        acceptedAt = LocalDateTime.now();
    }
}
