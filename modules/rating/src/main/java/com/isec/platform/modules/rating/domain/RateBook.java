package com.isec.platform.modules.rating.domain;

import com.isec.platform.common.domain.TenantBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rate_books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateBook extends TenantBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "version_name", nullable = false)
    private String versionName;

    private boolean active;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @OneToMany(mappedBy = "rateBook", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RateRule> rules = new ArrayList<>();
}
