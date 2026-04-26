package com.isec.platform.modules.documents.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;
import java.time.LocalDateTime;

@Table("authorized_valuers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizedValuer {
    @Id
    private Long id;

    private String companyName;

    private String contactPerson;

    private String email;

    private String phoneNumbers; // comma separated

    private String locations; // comma separated

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
