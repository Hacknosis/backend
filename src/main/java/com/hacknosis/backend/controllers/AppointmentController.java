package com.hacknosis.backend.controllers;

import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.models.Appointment;

import com.hacknosis.backend.services.PatientService;
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
@RequestMapping("api/appointment")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AppointmentController {
    private PatientService patientService;

    @PutMapping(value = "/{patientId}")
    public ResponseEntity<String> createAppointment(
            @RequestBody @Valid Appointment appointment,
            @PathVariable("patientId") long patientId,
            Authentication authentication) throws AccountNotFoundException {
        patientService.upsertAppointment(appointment, patientId, authentication.getName());
        return ResponseEntity.ok("Appointment updated");
    }

    @DeleteMapping(value = "/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(
            @PathVariable("appointmentId") long appointmentId,
            Authentication authentication) throws AccountNotFoundException, ResourceNotFoundException {
        patientService.deleteAppointment(appointmentId, authentication.getName());
        return ResponseEntity.ok("Appointment deleted");
    }
}
