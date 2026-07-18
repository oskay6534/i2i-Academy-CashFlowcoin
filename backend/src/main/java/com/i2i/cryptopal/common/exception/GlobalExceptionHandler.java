package com.i2i.cryptopal.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicateResource(
        DuplicateResourceException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.CONFLICT,
            exception.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler({
        InvalidCredentialsException.class,
        UnauthorizedException.class
    })
    public ResponseEntity<ApiError> handleUnauthorized(
        RuntimeException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.UNAUTHORIZED,
            exception.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler({
        InvalidMarketSymbolException.class,
        InvalidTradeException.class,
        InsufficientFundsException.class,
        InsufficientAssetException.class
    })
    public ResponseEntity<ApiError> handleBusinessRule(
        RuntimeException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            exception.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
        ResourceNotFoundException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            exception.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleAiUnavailable(
        AiServiceUnavailableException exception,
        HttpServletRequest request
    ) {
        return buildResponse(
            HttpStatus.SERVICE_UNAVAILABLE,
            exception.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error ->
                error.getField()
                    + ": "
                    + error.getDefaultMessage()
            )
            .collect(Collectors.joining(", "));

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            message,
            request.getRequestURI()
        );
    }

    private ResponseEntity<ApiError> buildResponse(
        HttpStatus status,
        String message,
        String path
    ) {
        ApiError error = new ApiError(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            path
        );

        return ResponseEntity.status(status).body(error);
    }
}