package com.isec.platform.modules.customers.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;

@Table("customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    private Long id;

    private String userId;

    private String email;

    private String fullName;

    private String phoneNumber;

    private String physicalAddress;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
