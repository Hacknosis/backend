package com.hacknosis.backend.controllers;

import com.hacknosis.backend.models.ReportData;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hacknosis.backend.services.EmailService;

import javax.security.auth.login.AccountNotFoundException;

@Log4j2
@RestController
@AllArgsConstructor
@RequestMapping("api/issue")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class IssueController {
    private EmailService emailService;
    @PostMapping(value = "/report_ticket")
    public ResponseEntity<String> submitReport(@RequestBody ReportData reportData) throws Exception {
        log.info("Received report data: " + reportData);
        System.out.println(reportData.getIssueDescription());
        try {
            emailService.sendIssueEmail(reportData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok("Report issued successfully");
    }
}
