package com.isec.platform.modules.certificates.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long policyId;

    @Column(unique = true)
    private String dmvicReference;

    @Enumerated(EnumType.STRING)
    private CertificateType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status;

    @Column(unique = true)
    private String idempotencyKey;

    private LocalDate startDate;
    private LocalDate expiryDate;
    private LocalDateTime issuedAt;
}
