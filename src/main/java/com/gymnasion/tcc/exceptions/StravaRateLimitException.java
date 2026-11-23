package com.gymnasion.tcc.exceptions;

public class StravaRateLimitException extends BusinessException {
    public StravaRateLimitException(String message) {
        super(message);
    }
}
