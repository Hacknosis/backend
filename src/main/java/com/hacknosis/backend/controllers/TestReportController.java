package com.hacknosis.backend.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hacknosis.backend.dto.ReportAnalysisResult;
import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.models.ReportType;
import com.hacknosis.backend.models.TestReport;
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
import java.io.File;
import java.io.IOException;
import java.util.List;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/report")
public class TestReportController {
    private ReportService reportService;
    @PostMapping(value = "/image/upload/{patient_id}", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadImageReport(
            @Parameter(
                    description = "Report to be uploaded",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "report") MultipartFile imageReport, @RequestPart(value="reportType") String reportType, @RequestPart(value="reportStatus") String reportStatus, Authentication authentication, @PathVariable(value = "patient_id") long patientId)
            throws IOException, AccountNotFoundException {
        if (imageReport.getContentType() != null && (imageReport.getContentType().startsWith("image/") || imageReport.getContentType().startsWith("application/pdf"))) {
            reportService.processReport(imageReport, authentication.getName(), patientId, reportType, reportStatus,false);
        } else {
            return ResponseEntity.badRequest().body("Invalid file format. Please upload an image file.");
        }
        return ResponseEntity.ok("Report is being processed");
    }

    @PostMapping(value = "/textual/upload/{patient_id}")
    public ResponseEntity<String> uploadTextualReport(
            @Parameter(
                    description = "Report to be uploaded",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "report") MultipartFile textualReport, @RequestPart(value="reportType") String reportType, @RequestPart(value="reportStatus") String reportStatus, Authentication authentication, @PathVariable(value = "patient_id") long patientId)
            throws AccountNotFoundException, IOException {
        if (!textualReport.isEmpty() && "text/plain".equals(textualReport.getContentType())) {
            reportService.processReport(textualReport, authentication.getName(), patientId, reportType,reportStatus,true);
            return ResponseEntity.ok("Report is being processed");
        } else {
            return ResponseEntity.badRequest().body("Invalid file format. Please upload a text file.");
        }
    }

    @GetMapping(value = "/patient_report/read/{patient_id}")
    public ResponseEntity<List<TestReport>> readReport(@PathVariable("patient_id") long patientId)
            throws AccountNotFoundException {
        return ResponseEntity.ok(reportService.readTestReport(patientId));
    }
    /*@GetMapping(value = "/publication/read/{publication_id}")
    public ResponseEntity<String> readReportPublicationResource(@PathVariable("publication_id") String publicationId)
            throws ResourceNotFoundException {
        return ResponseEntity.ok(reportService.readPublication(publicationId));
    }*/

    @PostMapping(value = "/textual/entity_detection")
    public ResponseEntity<ReportAnalysisResult> entityDetection(@RequestBody String text) {
        return ResponseEntity.ok(reportService.entityDetection(text));
    }

    @PostMapping(value = "/textual/ontology_linking")
    public ResponseEntity<ReportAnalysisResult> OntologyLinking(@RequestBody String text) {
        return ResponseEntity.ok(reportService.ontologyLinking(text));
    }
}
