package com.hacknosis.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class HttpErrorResponse {
    Integer code;
    String message;
    @Singular
    List<String> errors;
}
