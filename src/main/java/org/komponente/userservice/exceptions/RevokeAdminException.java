package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class RevokeAdminException extends CustomException{
    public RevokeAdminException(String message) {
        super(message, ErrorCode.REVOKE_ADMIN_ERROR, HttpStatus.BAD_REQUEST);
    }
}
