package com.isec.platform.modules.vehicles.exception;

import com.isec.platform.common.exception.BusinessException;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
