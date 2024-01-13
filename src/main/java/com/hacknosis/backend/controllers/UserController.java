package com.hacknosis.backend.controllers;

import com.hacknosis.backend.models.Patient;
import com.hacknosis.backend.models.TestReport;
import com.hacknosis.backend.models.User;
import com.hacknosis.backend.repositories.UserRepository;
import com.hacknosis.backend.services.UserService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.security.auth.login.AccountNotFoundException;
import java.util.List;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/user")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {
    private UserService userService;

    @GetMapping({"/all"})
    public ResponseEntity<List<User>> readUsers() throws AccountNotFoundException {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/info_update")
    public ResponseEntity<String> userInfoUpdate(@RequestBody User user) throws AccountNotFoundException {
        userService.updateUser(user);
        return ResponseEntity.ok("User information was updated");
    }
    @PostMapping("/add_patient/{user}")
    public ResponseEntity<String> addPatient(@RequestBody Patient patient, @PathVariable("user") String username)
            throws AccountNotFoundException {
        userService.addPatient(patient, username);
        return ResponseEntity.ok("New patient has been added");
    }
}
