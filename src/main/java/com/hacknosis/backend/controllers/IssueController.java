package com.hacknosis.backend.controllers;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hacknosis.backend.services.EmailService;
import org.springframework.web.multipart.MultipartFile;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/issue")
public class IssueController {
    private EmailService emailService;
    @PostMapping(value = "/report_ticket", consumes = {"multipart/form-data"})
    public ResponseEntity<String> submitReport(@RequestPart(value = "issue") MultipartFile screenshot,
                                               @RequestPart(value = "issueDescription") String issueDescription,
                                               @RequestPart(value = "timestamp") String timestamp,
                                               @RequestPart(value = "reporterID") String reporterID) throws Exception {
        //log.info("Received report data: " + reportData);
        log.info("Received screenshot: " + screenshot);
        log.info("Description: " + issueDescription);
        log.info("Time: " + timestamp);
        log.info("Reporter ID: " + reporterID);
        try {
            emailService.sendIssueEmail(screenshot,issueDescription,timestamp,reporterID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("Report issued successfully");
    }
}
