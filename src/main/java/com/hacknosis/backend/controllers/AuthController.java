package com.hacknosis.backend.controllers;

import com.hacknosis.backend.dto.JwtRequest;
import com.hacknosis.backend.dto.JwtResponse;
import com.hacknosis.backend.models.User;
import com.hacknosis.backend.services.UserService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import javax.validation.Valid;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/auth")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody @Valid JwtRequest authenticationRequest) {
        String token = userService.authenticate(authenticationRequest);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @GetMapping("/user_info")
    public ResponseEntity<User> userInfo(Authentication authentication) throws AccountNotFoundException {
        return ResponseEntity.ok(userService.getUser(authentication.getName()));
    }


}
