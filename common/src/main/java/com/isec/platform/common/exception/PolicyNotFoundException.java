package com.isec.platform.common.exception;

public class PolicyNotFoundException extends ResourceNotFoundException {
    public PolicyNotFoundException(Long applicationId) {
        super("No policy found for application ID: " + applicationId);
    }
}
