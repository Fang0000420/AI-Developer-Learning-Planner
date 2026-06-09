package com.aidevplanner.backend.common;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " " + id + " was not found.");
    }

    public ResourceNotFoundException(String resourceName, String id) {
        super(resourceName + " " + id + " was not found.");
    }
}
