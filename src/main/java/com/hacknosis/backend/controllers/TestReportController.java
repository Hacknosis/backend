package com.hacknosis.backend.controllers;

import com.hacknosis.backend.models.TestReport;
import com.hacknosis.backend.services.PatientService;
import com.hacknosis.backend.services.ReportService;
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
import java.io.IOException;
import java.util.List;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/report")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class TestReportController {
    private ReportService reportService;

    @PostMapping(value = "/image/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadImageReport(
            @Parameter(
                    description = "Report to be uploaded",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "report") MultipartFile imageReport, Authentication authentication)
            throws IOException, AccountNotFoundException {

        String result = reportService.processImageReport(imageReport, authentication.getName());
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/textual/upload")
    public ResponseEntity<String> uploadTextualReport(@RequestBody String textualReport, Authentication authentication)
            throws AccountNotFoundException {
        String result = reportService.processTextualReport(textualReport, authentication.getName());
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/read/{patient_id}")
    public ResponseEntity<List<TestReport>> readReport(@PathVariable("patient_id") long patientId)
            throws AccountNotFoundException {
        return ResponseEntity.ok(reportService.readTestReport(patientId));
    }

    @PostMapping(value = "/textual/entity_detection")
    public ResponseEntity<String> entityDetection(@RequestBody String text) {
        return ResponseEntity.ok(reportService.entityDetection(text));
    }

    @PostMapping(value = "/textual/ontology_linking")
    public ResponseEntity<String> OntologyLinking(@RequestBody String text) {
        return ResponseEntity.ok(reportService.ontologyLinking(text));
    }
}
