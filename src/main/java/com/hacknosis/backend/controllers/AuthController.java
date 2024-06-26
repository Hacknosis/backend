package com.hacknosis.backend.controllers;

import com.hacknosis.backend.dto.JwtRequest;
import com.hacknosis.backend.dto.JwtResponse;
import com.hacknosis.backend.models.User;
import com.hacknosis.backend.services.UserService;
import com.hacknosis.backend.utils.OAuth2TokenUtil;
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
public class AuthController {
    private final UserService userService;
    private final OAuth2TokenUtil oAuth2TokenUtil;
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody @Valid JwtRequest authenticationRequest) throws AccountNotFoundException {
        String token = userService.authenticate(authenticationRequest);
        return ResponseEntity.ok(new JwtResponse(token, userService.getUser(authenticationRequest.getUsername())));
    }

    @GetMapping("/user_info")
    public ResponseEntity<User> userInfo(Authentication authentication) throws AccountNotFoundException {
        return ResponseEntity.ok(userService.getUser(authentication.getName()));
    }
    @GetMapping("/exchange_token")
    public ResponseEntity<String> exchangeAccessToken() {
        return ResponseEntity.ok(oAuth2TokenUtil.getAccessToken());
    }

}
