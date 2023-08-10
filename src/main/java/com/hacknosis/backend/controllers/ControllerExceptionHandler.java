package com.hacknosis.backend.controllers;

import com.hacknosis.backend.exceptions.*;
import com.hacknosis.backend.models.HttpErrorResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.http.fileupload.impl.FileSizeLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Log4j2
@RestControllerAdvice
public class ControllerExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    protected HttpErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn(ex.getMessage(), ex);
        BindingResult bindingResult = ex.getBindingResult();

        List<String> errors = bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        errors.addAll(bindingResult.getGlobalErrors().stream()
                .map(error -> error.getObjectName() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList()));

        return HttpErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errors(errors)
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = AccountInfoConflictException.class)
    protected HttpErrorResponse handleAccountInfoConflictException(Exception ex) {
        AccountInfoConflictException e = (AccountInfoConflictException) ex;
        return HttpErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .error(e.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = AuthenticationException.class)
    protected HttpErrorResponse handleAuthenticationException(Exception ex) {
        AuthenticationException e = (AuthenticationException) ex;
        return HttpErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .error(e.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = TimeoutException.class)
    protected HttpErrorResponse handleTimeoutException(Exception ex) {
        TimeoutException e = (TimeoutException) ex;
        return HttpErrorResponse.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .error("Timeout occurred while processing video")
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = ResourceNotFoundException.class)
    protected HttpErrorResponse handleResourceNotFoundException(Exception ex) {
        ResourceNotFoundException e = (ResourceNotFoundException) ex;
        return HttpErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .error(e.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = InvalidTokenException.class)
    protected HttpErrorResponse handleInvalidTokenException(Exception ex) {
        InvalidTokenException e = (InvalidTokenException) ex;
        return HttpErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .error(e.getMessage())
                .build();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    protected HttpErrorResponse handleFileSizeLimitExceededException(Exception ex) {
        MaxUploadSizeExceededException e = (MaxUploadSizeExceededException) ex;
        return HttpErrorResponse.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .error("File size cannot exceed 50MB")
                .build();
    }
}
