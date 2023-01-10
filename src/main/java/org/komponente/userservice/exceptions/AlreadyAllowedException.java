package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class AlreadyAllowedException extends CustomException{
    public AlreadyAllowedException(String message) {
        super(message, ErrorCode.ALREADY_CAN_USE, HttpStatus.BAD_REQUEST);
    }
}
