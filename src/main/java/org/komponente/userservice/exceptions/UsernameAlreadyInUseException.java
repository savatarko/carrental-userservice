package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyInUseException extends CustomException{
    public UsernameAlreadyInUseException(String message) {
        super(message, ErrorCode.USERNAME_ALREADY_IN_USE, HttpStatus.CONFLICT);
    }
}
