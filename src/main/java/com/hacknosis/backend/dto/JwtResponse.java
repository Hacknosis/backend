package com.hacknosis.backend.dto;

import com.hacknosis.backend.models.User;
import lombok.Data;

import java.io.Serializable;

@Data
public class JwtResponse implements Serializable {
    private static final long serialVersionUID = -8091879091924046844L;
    private final String access_token;
    private final User user;

    public JwtResponse(String jwttoken, User user) {
        this.access_token = jwttoken;
        this.user = user;
    }
}