package com.hacknosis.backend.services;

import com.hacknosis.backend.dto.ZoomMeetingObject;
import com.hacknosis.backend.dto.OAuth2Response;
import com.hacknosis.backend.dto.ZoomMeetingSettings;
import com.hacknosis.backend.models.Appointment;
import com.hacknosis.backend.models.Patient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Log4j2
public class ZoomService {
    private final String clientId;
    private final String clientSecret;
    private final String accountId;
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    @Autowired
    public ZoomService(@Value("${zoom_client_id}") String clientId,
                       @Value("${zoom_client_secret}") String clientSecret,
                       @Value("${zoom_account_id}") String accountId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accountId = accountId;
    }
    public ZoomMeetingObject createMeeting(Appointment appointment) {
        ZoomMeetingObject zoomMeetingObject = new ZoomMeetingObject();
        String apiUrl = "https://api.zoom.us/v2/users/me/meetings";
        zoomMeetingObject.setPassword("hacknosis");

        ZoomMeetingSettings settingsDTO = new ZoomMeetingSettings();
        settingsDTO.setJoin_before_host(true);
        zoomMeetingObject.setSettings(settingsDTO);

        zoomMeetingObject.setStart_time(appointment.getAppointmentTime().format(formatter));
        zoomMeetingObject.setTimezone("America/New_York");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + getAccessToken());
        headers.add("content-type", "application/json");
        HttpEntity<ZoomMeetingObject> httpEntity = new HttpEntity<ZoomMeetingObject>(zoomMeetingObject, headers);
        ResponseEntity<ZoomMeetingObject> zEntity = restTemplate.exchange(apiUrl, HttpMethod.POST, httpEntity, ZoomMeetingObject.class);
        if(zEntity.getStatusCodeValue() == 201) {
            System.out.println(zEntity.getBody());
            return zEntity.getBody();
        } else {
            log.error("Error while creating zoom meeting {}", zEntity.getStatusCode());
        }
        return zoomMeetingObject;
    }

    public void deleteMeeting(Appointment appointment) {
        String apiUrl = "https://api.zoom.us/v2/meetings/" + appointment.getMeetingId();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + getAccessToken());
        HttpEntity<ZoomMeetingObject> httpEntity = new HttpEntity<ZoomMeetingObject>(headers);
        ResponseEntity<ZoomMeetingObject> zEntity = restTemplate.exchange(apiUrl, HttpMethod.DELETE, httpEntity, ZoomMeetingObject.class);
        if(zEntity.getStatusCodeValue() != 204) {
            log.error("Error while deleting the zoom meeting {} for appointment: {}", zEntity.getStatusCode(), appointment);
        }
    }

    public void registerPatient(Patient patient, Long meetingId) {
        String apiUrl = "https://api.zoom.us/v2/meetings/" + meetingId + "/registrants";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + getAccessToken());
        headers.add("content-type", "application/json");

        Map<String, String> registrant = new HashMap<>();
        String[] name = patient.getName().split(" ");
        registrant.put("first_name", name[0]);
        registrant.put("last_name", name.length > 1 ? name[1] : "");
        registrant.put("email", "heyupang04@gmail.com");

        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(registrant, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, httpEntity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Error while registering patient {} to zoom meeting!", patient);
        }
    }


    public String getAccessToken() {
        String tokenUri = "https://zoom.us/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=account_credentials&account_id=" + accountId;
        RequestEntity<String> requestEntity = RequestEntity
                .post(tokenUri)
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()))
                .headers(headers)
                .body(body);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<OAuth2Response> responseEntity = restTemplate.exchange(requestEntity, OAuth2Response.class);
        OAuth2Response responseBody = responseEntity.getBody();

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseBody.getAccess_token();
        } else {
            log.error("Error: " + responseEntity.getStatusCode());
            return "";
        }
    }
}
