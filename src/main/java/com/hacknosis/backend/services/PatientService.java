package com.hacknosis.backend.services;

import com.hacknosis.backend.exceptions.ReportProcessingException;
import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.models.*;
import com.hacknosis.backend.repositories.AppointmentRepository;
import com.hacknosis.backend.repositories.PatientRepository;
import com.hacknosis.backend.repositories.TestReportRepository;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.AllArgsConstructor;
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
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class PatientService {
    private PatientRepository patientRepository;
    private UserService userService;
    private TestReportRepository testReportRepository;
    private AppointmentRepository appointmentRepository;

    public void updatePatient(Patient patient) throws AccountNotFoundException {
        if (patientRepository.findById(patient.getId()).isEmpty()) {
            throw new AccountNotFoundException("The provided patient entity does not exist");
        }
        if (patient.getUser() == null) {
            Patient oldPatient = patientRepository.findById(patient.getId()).get();
            patient.setUser(oldPatient.getUser());
        }
        patientRepository.save(patient);
    }

    public void upsertAppointment(Appointment appointment, long patientId, String username) throws AccountNotFoundException {
        if (!userService.usernameExist(username)) {
            throw new AccountNotFoundException("The authenticated Doctor account does not exist");
        } else if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException(String.format("The patient with id - %d does not exist", patientId));
        }
        Patient patient = patientRepository.getReferenceById(patientId);
        appointment.setPatient(patient);
        appointmentRepository.save(appointment);
    }
    public void deleteAppointment(long appointmentId, String username) throws AccountNotFoundException, ResourceNotFoundException {
        if (!userService.usernameExist(username)) {
            throw new AccountNotFoundException("The authenticated Doctor account does not exist");
        } else if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException(String.format("The appointment with id - %d does not exist", appointmentId));
        }
        appointmentRepository.deleteById(appointmentId);
    }

    public void processImageReport(MultipartFile imageReport, String username) throws AccountNotFoundException, IOException {
        User doctor = userService.getUser(username);
        byte[] bytes = imageReport.getBytes();
        String encodedByte = Base64.getEncoder().encodeToString(bytes);
        encodedByte = encodedByte.replaceAll("\u0000", "");

        WebClient client = buildClient();
        String result = client.post()
                .uri("/report/image")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(imageReport)
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

        TestReport report = buildTestReport(doctor, ReportType.IMAGE, result, encodedByte);
        testReportRepository.save(report);
    }

    public void processTextualReport(String textualReport, String username) throws AccountNotFoundException {
        User doctor = userService.getUser(username);
        WebClient client = buildClient();
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

        TestReport report = buildTestReport(doctor, ReportType.IMAGE, result, textualReport);
        testReportRepository.save(report);
    }
    public WebClient buildClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofMillis(5000))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS)));

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
}
