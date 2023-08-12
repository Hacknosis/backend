package com.hacknosis.backend.services;

import com.hacknosis.backend.dto.JwtRequest;
import com.hacknosis.backend.exceptions.AccountInfoConflictException;
import com.hacknosis.backend.exceptions.AuthenticationException;
import com.hacknosis.backend.models.Patient;
import com.hacknosis.backend.models.User;
import com.hacknosis.backend.repositories.UserRepository;
import com.hacknosis.backend.utils.JwtTokenUtil;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountNotFoundException;

@Service
@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;
    private JwtTokenUtil jwtTokenUtil;
    private AuthenticationManager authenticationManager;
    public String authenticate(JwtRequest authenticationRequest) {
        final Authentication authentication;
        try {
            authentication = generateAuthentication(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        } catch (BadCredentialsException e) {
            throw new AuthenticationException("Invalid User Credentials");
        }
        return jwtTokenUtil.generateToken(authentication);
    }
    private Authentication generateAuthentication(String username, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
    public boolean usernameExist(String username) {
        return userRepository.findUserByUsername(username).isPresent();
    }
    public User getUser(String username) throws AccountNotFoundException {
        if (!usernameExist(username)) {
            throw new AccountNotFoundException(String.format("Account with username %s does not exist", username));
        }
        return userRepository.findUserByUsername(username).get();
    }

    public void updateUser(User user) throws AccountNotFoundException {
        if (!usernameExist(user.getUsername())) {
            throw new AccountNotFoundException(String.format("Account with username %s does not exist", user.getUsername()));
        }
        userRepository.save(user);
    }

    public void addPatient(Patient patient, String username) throws AccountNotFoundException {
        if (!usernameExist(username)) {
            throw new AccountNotFoundException(String.format("Account with username %s does not exist", username));
        }
        User doctor = userRepository.findUserByUsername(username).get();
        doctor.getPatients().add(patient);
        userRepository.save(doctor);
    }
}
