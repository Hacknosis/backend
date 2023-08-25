package com.hacknosis.backend.services;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedical;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedicalClient;
import com.amazonaws.services.comprehendmedical.model.DetectEntitiesV2Request;
import com.amazonaws.services.comprehendmedical.model.DetectEntitiesV2Result;
import com.amazonaws.services.comprehendmedical.model.InferICD10CMRequest;
import com.amazonaws.services.comprehendmedical.model.InferICD10CMResult;
import com.hacknosis.backend.exceptions.ReportProcessingException;
import com.hacknosis.backend.models.ReportType;
import com.hacknosis.backend.models.TestReport;
import com.hacknosis.backend.models.User;
import com.hacknosis.backend.repositories.PatientRepository;
import com.hacknosis.backend.repositories.TestReportRepository;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ReportService {
    private final String awsAccessKey;
    private final String awsSecretKey;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private TestReportRepository testReportRepository;
    @Autowired
    private UserService userService;
    @Autowired
    public ReportService(@Value("${aws_access_key}") String awsAccessKey, @Value("${aws_secret_key}") String awsSecretKey) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
    }
    public List<TestReport> readTestReport(long patientId) throws AccountNotFoundException {
        if (!patientRepository.existsById(patientId)) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        return testReportRepository.findTestReportByPatientId(patientId);
    }

    public String processImageReport(MultipartFile imageReport, String username) throws AccountNotFoundException, IOException {
        User doctor = userService.getUser(username);
        byte[] bytes = imageReport.getBytes();
        String encodedByte = Base64.getEncoder().encodeToString(bytes);
        encodedByte = encodedByte.replaceAll("\u0000", "");

        /*MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return imageReport.getOriginalFilename();
            }
        });*/

        WebClient client = buildWebClient();
        String result = client.post()
                .uri("/report/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                // .body(BodyInserters.fromMultipartData(formData))
                .bodyValue(encodedByte)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> {
                    throw new ReportProcessingException("Unknown error while processing the report");
                })
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
                    throw new ReportProcessingException("Unknown error while processing the report");
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10000000))
                .block();

        TestReport report = buildTestReport(doctor, ReportType.MRI, result, encodedByte);
        testReportRepository.save(report);
        return result;
    }

    public String processTextualReport(String textualReport, String username) throws AccountNotFoundException {
        User doctor = userService.getUser(username);
        WebClient client = buildWebClient();
        String result = client.post()
                .uri("/report/textual")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(textualReport)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> {
                    throw new ReportProcessingException("Unknown error while processing the report");
                })
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
                    throw new ReportProcessingException("Unknown error while processing the report");
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10000))
                .block();

        TestReport report = buildTestReport(doctor, ReportType.CT, result, textualReport);
        testReportRepository.save(report);
        return result;
    }
    public String entityDetection(String text) {
        AWSComprehendMedical client = buildAWSClient();

        DetectEntitiesV2Request request = new DetectEntitiesV2Request();
        request.setText(text);

        DetectEntitiesV2Result result = client.detectEntitiesV2(request);
        return result.toString();
    }

    public String ontologyLinking(String text) {
        AWSComprehendMedical client = buildAWSClient();

        InferICD10CMRequest request = new InferICD10CMRequest();
        request.setText(text);

        InferICD10CMResult result = client.inferICD10CM(request);
        return result.toString();
    }

    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500000000)
                .responseTimeout(Duration.ofMillis(500000000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(500000000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000000, TimeUnit.MILLISECONDS)));

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://localhost:8000/api")
                .build();

        return client;
    }
    public TestReport buildTestReport(User doctor, ReportType reportType, String result, String data) {
        return TestReport.builder()
                .type(reportType)
                .user(doctor)
                .testData(data)
                .analysisResult(result)
                .build();
    }
    public AWSComprehendMedical buildAWSClient() {
        AWSCredentialsProvider credentials
                = new AWSStaticCredentialsProvider(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
        return AWSComprehendMedicalClient.builder()
                .withCredentials(credentials)
                .withRegion("us-east-1")
                .build();
    }
}
