package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class AccessRevokedException extends CustomException{
    public AccessRevokedException(String message) {
        super(message, ErrorCode.NO_ACCESS, HttpStatus.FORBIDDEN);
    }
}
