package com.isec.platform.modules.documents.dto;

import lombok.Data;

@Data
public class AuthorizedValuerRequest {
    private String companyName;
    private String contactPerson;
    private String email;
    private String phoneNumbers;
    private String locations;
    private Boolean active;
}
