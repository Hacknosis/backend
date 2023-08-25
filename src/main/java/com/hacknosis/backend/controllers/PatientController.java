package com.hacknosis.backend.controllers;

import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.models.Appointment;
import com.hacknosis.backend.models.Patient;
import com.hacknosis.backend.models.TestReport;
import com.hacknosis.backend.services.PatientService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.security.auth.login.AccountNotFoundException;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/patient")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class PatientController {
    private PatientService patientService;

    @PutMapping(value = "/info_update")
    public ResponseEntity<String> updatePatientInformation(@RequestBody @Valid Patient patient) throws AccountNotFoundException {
        patientService.updatePatient(patient);
        return ResponseEntity.ok("Patient information has been updated");
    }
    @PutMapping(value = "appointment/{patientId}")
    public ResponseEntity<String> createAppointment(
            @RequestBody @Valid Appointment appointment,
            @PathVariable("patientId") long patientId,
            Authentication authentication) throws AccountNotFoundException {
        patientService.upsertAppointment(appointment, patientId, authentication.getName());
        return ResponseEntity.ok("Appointment updated");
    }

    @DeleteMapping(value = "appointment/{appointmentId}")
    public ResponseEntity<String> deleteAppointment(
            @PathVariable("appointmentId") long appointmentId,
            Authentication authentication) throws AccountNotFoundException, ResourceNotFoundException {
        patientService.deleteAppointment(appointmentId, authentication.getName());
        return ResponseEntity.ok("Appointment deleted");
    }
}
