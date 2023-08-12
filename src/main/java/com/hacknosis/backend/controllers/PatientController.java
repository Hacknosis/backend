package com.hacknosis.backend.controllers;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hacknosis.backend.models.Indicator;
import com.hacknosis.backend.models.Patient;
import com.hacknosis.backend.models.ResusStatus;
import com.hacknosis.backend.services.PatientService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.security.auth.login.AccountNotFoundException;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/patient")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@OpenAPIDefinition(info = @Info(title = "User API", version = "1.0", description = "Web server for patient operations"))
public class PatientController {
    private PatientService patientService;

    @PostMapping(value = "/report/image/upload")
    public ResponseEntity<String> uploadImageReport(
            @Parameter(
                    description = "Report to be uploaded",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "report") MultipartFile imageReport, Authentication authentication)
            throws IOException, AccountNotFoundException {

        patientService.processImageReport(imageReport, authentication.getName());
        return ResponseEntity.ok("Report is being analyzed");
    }

    @PostMapping(value = "/report/textual/upload")
    public ResponseEntity<String> uploadTextualReport(@RequestBody String textualReport, Authentication authentication)
            throws AccountNotFoundException {
        patientService.processTextualReport(textualReport, authentication.getName());
        return ResponseEntity.ok("Report is being analyzed");
    }

    @PostMapping(value = "/info_update")
    public ResponseEntity<String> updatePatientInformation(@RequestBody @Valid Patient patient) throws AccountNotFoundException {
        patientService.updatePatient(patient);
        return ResponseEntity.ok("Patient information has been updated");
    }

}
