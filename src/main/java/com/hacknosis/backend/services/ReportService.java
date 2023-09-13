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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.security.auth.login.AccountNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
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
    public void processReport(MultipartFile medicalReport, String username, Long patientId, String reportType, String reportStatus, boolean text) throws AccountNotFoundException, IOException {
        if (!patientRepository.existsById(patientId)) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        User doctor = userService.getUser(username);
        Patient patient = patientRepository.getReferenceById(patientId);

        String contentId;
        byte[] encodedByte;
        if (text) {
            encodedByte = medicalReport.getBytes();
        } else {
            encodedByte = preprocess(medicalReport);
            //encodedByte = medicalReport.getBytes();
        }
        contentId = openTextService.uploadDocumentToContentStorage(encodedByte, medicalReport.getOriginalFilename());
        log.info("Uploaded content id is: " + contentId);

        String publicationId = openTextService.publishDocument(contentId);
        log.info("Publication id is: " + publicationId);

        ReportAnalysisResult entityDetectionAnalysisResult = null, ontologyAnalysisResult = null;
        String content = "";
        if (text) {
            content = new String(medicalReport.getBytes());
            entityDetectionAnalysisResult = entityDetection(content);
            ontologyAnalysisResult = ontologyLinking(content);
        }

        TestReport report = TestReport.builder()
                .user(doctor)
                .contentId(contentId)
                .publicationId(publicationId)
                .patient(patient)
                .date(LocalDateTime.now())
                .content(content)
                .type(ReportType.valueOf(reportType))
                .reportStatus(ReportStatus.valueOf(reportStatus))
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
    public byte[] preprocess(MultipartFile report) throws IOException {
        String url = "/report/image";
        WebClient client = buildWebClient("http://localhost:8000/api");

        byte[] bytes = report.getBytes();
        String encodedByte = Base64.getEncoder().encodeToString(bytes);
        encodedByte = encodedByte.replaceAll("\u0000", "");

        WebClient.ResponseSpec spec = client.post()
                .uri(url)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(encodedByte)
                .retrieve();

        byte[] processedImageByte = retrieveResult(spec, byte[].class);
        return processedImageByte;
    }
    public <T> T retrieveResult(WebClient.ResponseSpec spec, Class<T> responseType) {
        return spec.onStatus(HttpStatus::is4xxClientError, clientResponse -> {
                    throw new ReportProcessingException("Unknown Error");
                })
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
                    String errorBody = clientResponse.bodyToMono(String.class).block();
                    log.error("5xx Server Error: {}", errorBody);
                    throw new ReportProcessingException("Unknown error in ai-engine while processing the report");
                })
                .bodyToMono(responseType)
                .timeout(Duration.ofSeconds(500))
                .block();
    }
    public WebClient buildWebClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(100))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(1000, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(1000, TimeUnit.SECONDS)));

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();

        return client;
    }
}
