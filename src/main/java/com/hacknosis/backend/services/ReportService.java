package com.hacknosis.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacknosis.backend.dto.ReportAnalysisResult;
import com.hacknosis.backend.exceptions.ReportProcessingException;
import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.models.*;
import com.hacknosis.backend.repositories.PatientRepository;
import com.hacknosis.backend.repositories.TestReportRepository;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Log4j2
@AllArgsConstructor
public class ReportService {
    private PatientRepository patientRepository;
    private TestReportRepository testReportRepository;
    private UserService userService;
    private AWSMedicalService awsMedicalService;
    private OpenTextService openTextService;

    public List<TestReport> readTestReport(long patientId) throws AccountNotFoundException {
        if (!patientRepository.existsById(patientId)) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        return testReportRepository.findTestReportByPatientId(patientId);
    }
    public String readPublication(String publicationId) throws ResourceNotFoundException {
        return openTextService.getPublication(publicationId);
    }
    public void processReport(MultipartFile medicalReport, String username, Long patientId, boolean text) throws AccountNotFoundException, IOException {
        if (!patientRepository.existsById(patientId)) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        User doctor = userService.getUser(username);
        Patient patient = patientRepository.getReferenceById(patientId);
        String contentId = openTextService.uploadDocumentToContentStorage(medicalReport);
        log.info("Uploaded content id is: " + contentId);

        String publicationId = openTextService.publishDocument(contentId);
        log.info("Publication id is: " + publicationId);

        ReportAnalysisResult entityDetectionAnalysisResult = null, ontologyAnalysisResult = null;
        if (text) {
            String content = new String(medicalReport.getBytes());
            entityDetectionAnalysisResult = entityDetection(content);
            ontologyAnalysisResult = ontologyLinking(content);
        }

        TestReport report = TestReport.builder()
                .user(doctor)
                .contentId(contentId)
                .publicationId(publicationId)
                .patient(patient)
                .date(LocalDateTime.now())
                .type(ReportType.CT)
                .reportStatus(ReportStatus.AVAILABLE)
                .entityDetectionAnalysisResult(jsonStringify(entityDetectionAnalysisResult))
                .ontologyLinkingAnalysisResult(jsonStringify(ontologyAnalysisResult))
                .build();

        testReportRepository.save(report);
    }
    public ReportAnalysisResult entityDetection(String text) {
        // things to parse:
        // entity -> text, entity -> attributes(type, text), entity -> traits
        return awsMedicalService.entityDetection(text);
    }
    public ReportAnalysisResult ontologyLinking(String text) {
        // things to parse:
        // entity -> text, entity -> attributes(type, text), entity -> traits(NEGATION, SIGN, DIAGNOSIS)
        return awsMedicalService.ontologyLinking(text);
    }

    public String jsonStringify(Object value) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (value == null) return "";
        return objectMapper.writeValueAsString(value);
    }
}
