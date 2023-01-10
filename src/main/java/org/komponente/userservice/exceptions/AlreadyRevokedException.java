package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class AlreadyRevokedException extends CustomException{
    public AlreadyRevokedException(String message) {
        super(message, ErrorCode.ALREADY_REVOKED, HttpStatus.BAD_REQUEST);
    }
}
