package com.ibm.hybrid.cloud.sample.stocktrader.account.accountgraphql.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> notFoundHandler() {
        return ResponseEntity.notFound().build();
    }
}
