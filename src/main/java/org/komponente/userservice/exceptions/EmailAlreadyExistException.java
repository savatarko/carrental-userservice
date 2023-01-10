package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistException extends CustomException{
    public EmailAlreadyExistException(String message) {
        super(message, ErrorCode.EMAIL_ALREADY_IN_USE, HttpStatus.CONFLICT);
    }
}
