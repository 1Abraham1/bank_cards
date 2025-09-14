package com.abrik.bank_cards.bank_cards.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<AppError> build(HttpStatus status, String message, Throwable ex) {
        if (ex != null) log.warn("{}: {}", status.value(), message, ex);
        else log.warn("{}: {}", status.value(), message);
        return ResponseEntity.status(status).body(new AppError(status.value(), message));
    }

    // неверные креды (кидает authenticationManager)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AppError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Incorrect username or password", ex);
    }

    // ошибки JWT-проверки
    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<AppError> handleJwt(JwtAuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex);
    }

    // нет прав
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AppError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access is denied", ex);
    }

    // пользователь не найден (UserDetailsService)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<AppError> handleUserNotFound(UsernameNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppError> handleAny(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<AppError> handlePasswordMismatch(PasswordMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<AppError> handleUsernameExists(UsernameAlreadyExistsException ex) {
        return build(HttpStatus.BAD_REQUEST, "The user with the specified name already exists: " + ex.getMessage(), ex);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<AppError> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }

    @ExceptionHandler(BadInputParameters.class)
    public ResponseEntity<AppError> handleBadInputParameters(BadInputParameters ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<AppError> handleBadRequest(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<AppError> handleRoleNotFound(RoleNotFoundException ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<AppError> handleRoleNotFound(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), ex);
    }
}
