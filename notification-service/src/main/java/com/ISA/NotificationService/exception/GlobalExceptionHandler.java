package com.ISA.Restaurant.exception;

import com.ISA.Restaurant.Dto.Response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle specific exceptions with custom messages and statuses

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException e, WebRequest request) {
        log.error("EmailAlreadyExistsException: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(RestaurantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRestaurantNotFound(RestaurantNotFoundException e, WebRequest request) {
        log.warn("RestaurantNotFoundException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(RestaurantNameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleRestaurantNameAlreadyExists(RestaurantNameAlreadyExistsException e, WebRequest request) {
        log.error("RestaurantNameAlreadyExistsException: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e, WebRequest request) {
        log.error("InvalidRequestException: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MenuItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMenuItemNotFound(MenuItemNotFoundException e, WebRequest request) {
        log.warn("MenuItemNotFoundException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e, WebRequest request) {
        log.warn("EntityNotFoundException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(InvalidMenuItemException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMenuItem(InvalidMenuItemException e, WebRequest request) {
        log.error("InvalidMenuItemException: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MenuNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMenuNotFound(MenuNotFoundException e, WebRequest request) {
        log.warn("MenuNotFoundException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e, WebRequest request) {
        log.warn("UnauthorizedException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException e, WebRequest request) {
        log.warn("OrderNotFoundException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(SameOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderState(SameOrderStateException e, WebRequest request) {
        log.warn("InvalidOrderStateException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStateTransition(InvalidOrderStateTransitionException e, WebRequest request) {
        log.warn("InvalidOrderStateTransitionException: {}", e.getMessage());
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, request);
    }

    // Fallback for unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e, WebRequest request) {
        log.error("Unhandled Exception: {}", e.getMessage(), e);
        return buildErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    // Helper method to build the error response
    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception e, HttpStatus status, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                e.getMessage(),
                request.getDescription(false)
        );

        log.info("ErrorResponse built: {}", response);
        return new ResponseEntity<>(response, status);
    }
}
