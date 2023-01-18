package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class NotActivatedException extends CustomException{
    public NotActivatedException(String message) {
        super(message, ErrorCode.NOT_ACTIVATED, HttpStatus.UNAUTHORIZED);
    }
}
