package com.hacknosis.backend.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacknosis.backend.dto.ReportAnalysisResult;
import com.hacknosis.backend.dto.ReportSegmentResponse;
import com.hacknosis.backend.exceptions.ReportProcessingException;
import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.models.*;
import com.hacknosis.backend.repositories.ImageReportRepository;
import com.hacknosis.backend.repositories.PatientRepository;
import com.hacknosis.backend.repositories.TextualReportRepository;
import com.hacknosis.backend.utils.StorageUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
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
    private final ImageReportRepository imageReportRepository;
    private PatientRepository patientRepository;
    private TextualReportRepository textualReportRepository;
    private UserService userService;
    private AWSMedicalService awsMedicalService;
    private StorageUtil storageUtil;

    public List<TextualReport> readTextualReport(long patientId) throws AccountNotFoundException {
        if (!patientRepository.existsById(patientId)) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        return textualReportRepository.findTextualReportByPatientId(patientId);
    }

    public List<ImageReport> readImageReport(long patientId) throws AccountNotFoundException {
        if (!patientRepository.existsById(patientId)) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        List<ImageReport> reports = imageReportRepository.findImageReportByPatientId(patientId);
        reports.forEach(report -> report.setContent(storageUtil.readContent(report.getStorageId())));
        return reports;
    }

    public byte[] getImageReportContent(long reportId) throws ResourceNotFoundException, IOException {
        if (imageReportRepository.findById(reportId).isEmpty()) {
            throw new ResourceNotFoundException(String.format("The provided report with id %s does not exist", reportId));
        }
        return storageUtil.readContent(imageReportRepository.getReferenceById(reportId).getStorageId());
    }

    public void processReport(MultipartFile medicalReport, String username, Long patientId, ReportType reportType) throws AccountNotFoundException, IOException {
        if (!patientRepository.existsById(patientId)) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }

        User doctor = userService.getUser(username);
        Patient patient = patientRepository.getReferenceById(patientId);

        if (reportType.equals(ReportType.TEXT)) {
            processTextualReport(medicalReport, doctor, patient);
        } else {
            processImageReport(medicalReport, doctor, patient, reportType);
        }
    }

    public void processTextualReport(MultipartFile medicalReport, User doctor, Patient patient) throws IOException {
        String content = new String(medicalReport.getBytes());

        TextualReport report = TextualReport.builder()
                .user(doctor)
                .patient(patient)
                .date(LocalDateTime.now())
                .reportStatus(ReportStatus.PROCESSING)
                .build();

        textualReportRepository.save(report);

        ReportAnalysisResult entityDetectionAnalysisResult = entityDetection(content);
        ReportAnalysisResult ontologyAnalysisResult = ontologyLinking(content);

        report.setReportStatus(ReportStatus.AVAILABLE);
        report.setEntityDetectionAnalysisResult(jsonStringify(entityDetectionAnalysisResult));
        report.setOntologyLinkingAnalysisResult(jsonStringify(ontologyAnalysisResult));

        textualReportRepository.save(report);
    }

    public void processImageReport(MultipartFile medicalReport, User doctor, Patient patient, ReportType reportType) throws IOException {
        String filename = medicalReport.getOriginalFilename();
        String storageId = filename.split("\\.")[0]
                .concat(String.format("#%d.", patient.getId()))
                .concat(filename.split("\\.")[1]);

        ImageReport report = ImageReport.builder()
                .user(doctor)
                .patient(patient)
                .date(LocalDateTime.now())
                .reportStatus(ReportStatus.PROCESSING)
                .reportName(medicalReport.getOriginalFilename())
                .reportType(reportType)
                .storageId(storageId)
                .build();

        imageReportRepository.save(report);

        saveToCloudStorage(medicalReport, report.getStorageId());

        report.setReportStatus(ReportStatus.AVAILABLE);
        imageReportRepository.save(report);
    }

    public void saveToCloudStorage(MultipartFile file, String filename) throws IOException {
        storageUtil.saveContent(file.getBytes(), filename);
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

    public ReportSegmentResponse segmentImageReport(long reportId, Integer[] box) throws IOException {
        if (!imageReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("The provided report entity does not exist");
        }

        ImageReport imageReport = imageReportRepository.getReferenceById(reportId);
        byte[] reportBytes = storageUtil.readContent(imageReport.getStorageId());

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8000/api/report/segment_image";
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

        map.add("report", new ByteArrayResource(reportBytes) {
            @Override
            public String getFilename() {
                return imageReport.getStorageId();
            }
        });
        map.add("name", imageReport.getStorageId());
        map.add("box", Arrays.toString(box));

        try {
            String result = restTemplate.postForObject(url, map, String.class);
            AISegmentResponse response = parse(result, AISegmentResponse.class);
            byte[] segmentedBytes = Base64.getDecoder().decode(response.encodedBytes);

            return ReportSegmentResponse
                    .builder()
                    .originalImageByte(reportBytes)
                    .segmentedImageByte(segmentedBytes)
                    .build();
        } catch (Exception e) {
            log.error(e);
            throw new ReportProcessingException("Failed to generate segmentation");
        }
    }
    static class AISegmentResponse {
        @JsonProperty("encodedBytes")
        private String encodedBytes;
    }
    private  <T> T parse(String str, Class<T> clazz) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(str, clazz);
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
