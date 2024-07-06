package com.hacknosis.backend.controllers;

import com.hacknosis.backend.dto.ReportAnalysisResult;
import com.hacknosis.backend.dto.ReportSegmentRequest;
import com.hacknosis.backend.dto.ReportSegmentResponse;
import com.hacknosis.backend.models.ReportType;
import com.hacknosis.backend.models.TextualReport;
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
public class TestReportController {
    private ReportService reportService;
    @PostMapping(value = "/image/upload/{patient_id}", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadImageReport(
            @Parameter(
                    description = "Report to be uploaded",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "report") MultipartFile imageReport,
            @RequestPart(value="reportType") String reportType,
            Authentication authentication,
            @PathVariable(value = "patient_id") long patientId)
            throws IOException, AccountNotFoundException {
        if (imageReport.getContentType() != null && (imageReport.getContentType().startsWith("image/") || imageReport.getContentType().startsWith("application/pdf"))) {
            reportService.processReport(imageReport, authentication.getName(), patientId, ReportType.valueOf(reportType));
        } else {
            return ResponseEntity.badRequest().body("Invalid file format. Please upload an image file.");
        }
        return ResponseEntity.ok("Report is being processed");
    }

    @PostMapping(value = "/image/segment/{report_id}")
    public ResponseEntity<ReportSegmentResponse> segmentImageReport(@PathVariable(value = "report_id") long reportId, @RequestBody ReportSegmentRequest request) throws IOException {
        return ResponseEntity.ok(reportService.segmentImageReport(reportId, request.getBox()));
    }

    @PostMapping(value = "/textual/upload/{patient_id}")
    public ResponseEntity<String> uploadTextualReport(
            @Parameter(
                    description = "Report to be uploaded",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart(value = "report") MultipartFile textualReport, Authentication authentication, @PathVariable(value = "patient_id") long patientId)
            throws AccountNotFoundException, IOException {
        if (!textualReport.isEmpty() && "text/plain".equals(textualReport.getContentType())) {
            reportService.processReport(textualReport, authentication.getName(), patientId, ReportType.TEXT);
            return ResponseEntity.ok("Report is being processed");
        } else {
            return ResponseEntity.badRequest().body("Invalid file format. Please upload a text file.");
        }
    }

    @GetMapping(value = "/patient_report/read/{patient_id}")
    public ResponseEntity<List<TextualReport>> readReport(@PathVariable("patient_id") long patientId)
            throws AccountNotFoundException {
        return ResponseEntity.ok(reportService.readTextualReport(patientId));
    }
}
