package com.aidevplanner.backend.common;

public class ConcurrentJobExecutionException extends RuntimeException {

    public ConcurrentJobExecutionException(String message) {
        super(message);
    }
}
