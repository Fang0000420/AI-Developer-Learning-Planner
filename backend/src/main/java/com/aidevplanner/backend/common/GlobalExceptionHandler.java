package com.aidevplanner.backend.common;

import com.aidevplanner.backend.agent.AgentServiceException;
import com.aidevplanner.backend.auth.DuplicateUserException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.JsonMappingException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidationError(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiErrorResponse response = ApiErrorResponse.of(
                "BAD_REQUEST",
                "Request validation failed.",
                errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiErrorResponse> handleHandlerMethodValidationError(
            HandlerMethodValidationException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String parameterName = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.putIfAbsent(parameterName, error.getDefaultMessage());
            }
        }

        ApiErrorResponse response = ApiErrorResponse.of(
                "BAD_REQUEST",
                "Request validation failed.",
                errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String fieldName = extractLastPathSegment(violation.getPropertyPath().toString());
            errors.putIfAbsent(fieldName, violation.getMessage());
        }

        ApiErrorResponse response = ApiErrorResponse.of(
                "BAD_REQUEST",
                "Request validation failed.",
                errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String fieldName = exception.getName();
        ApiErrorResponse response = ApiErrorResponse.of(
                "BAD_REQUEST",
                "Request contains invalid values.",
                Map.of(fieldName, "Invalid value for " + fieldName + ".")
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        String fieldName = extractJsonFieldName(exception);
        ApiErrorResponse response = ApiErrorResponse.of(
                "BAD_REQUEST",
                "Request contains invalid values.",
                Map.of(fieldName, "request".equals(fieldName)
                        ? "Request body is malformed or contains invalid values."
                        : "Invalid value for " + fieldName + ".")
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "BAD_REQUEST",
                exception.getMessage(),
                Map.of("request", exception.getMessage())
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        ApiErrorResponse response = ApiErrorResponse.of("NOT_FOUND", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AgentServiceException.class)
    ResponseEntity<ApiErrorResponse> handleAgentServiceError(AgentServiceException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "BAD_GATEWAY",
                exception.getMessage(),
                Map.of("agentService", exception.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    @ExceptionHandler(DuplicateUserException.class)
    ResponseEntity<ApiErrorResponse> handleDuplicateUser(DuplicateUserException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "CONFLICT",
                exception.getMessage(),
                Map.of("auth", exception.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ConcurrentJobExecutionException.class)
    ResponseEntity<ApiErrorResponse> handleConcurrentJobExecution(ConcurrentJobExecutionException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "CONFLICT",
                exception.getMessage(),
                Map.of("job", exception.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException exception) {
        ApiErrorResponse response = ApiErrorResponse.of(
                "UNAUTHORIZED",
                "Invalid username or password.",
                Map.of("auth", "Invalid username or password.")
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    private String extractLastPathSegment(String propertyPath) {
        int separatorIndex = propertyPath.lastIndexOf('.');
        if (separatorIndex < 0 || separatorIndex == propertyPath.length() - 1) {
            return propertyPath;
        }
        return propertyPath.substring(separatorIndex + 1);
    }

    private String extractJsonFieldName(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof JsonMappingException mappingException
                && mappingException.getPath() != null
                && !mappingException.getPath().isEmpty()) {
            String fieldName = mappingException.getPath()
                    .get(mappingException.getPath().size() - 1)
                    .getFieldName();
            if (fieldName != null && !fieldName.isBlank()) {
                return fieldName;
            }
        }
        return "request";
    }
}
