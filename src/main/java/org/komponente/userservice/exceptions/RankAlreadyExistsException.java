package org.komponente.userservice.exceptions;

import org.springframework.http.HttpStatus;

public class RankAlreadyExistsException extends CustomException{
    public RankAlreadyExistsException(String message) {
        super(message, ErrorCode.RANK_NAME_ERROR, HttpStatus.CONFLICT);
    }
}
