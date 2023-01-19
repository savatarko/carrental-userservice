package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class ZeroIndexException extends CustomException{

    public ZeroIndexException(String message) {
        super(message, ErrorCode.ZERO_INDEX, HttpStatus.BAD_REQUEST);
    }
}
