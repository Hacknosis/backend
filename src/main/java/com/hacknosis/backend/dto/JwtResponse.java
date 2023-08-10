package com.hacknosis.backend.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class JwtResponse implements Serializable {
    private static final long serialVersionUID = -8091879091924046844L;
    private final String access_token;

    public JwtResponse(String jwttoken) {
        this.access_token = jwttoken;
    }
}