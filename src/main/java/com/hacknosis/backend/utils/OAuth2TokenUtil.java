package com.hacknosis.backend.utils;

import com.hacknosis.backend.dto.OAuth2Response;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;


@Component
@Log4j2
public class OAuth2TokenUtil {
	private final String clientId;
	private final String clientSecret;
	private final String username;
	private final String password;

	@Autowired
	public OAuth2TokenUtil(@Value("${opentext_client_id}") String clientId,
						   @Value("${opentext_client_secret}") String clientSecret,
						   @Value("${opentext_username}") String username,
						   @Value("${opentext_password}") String password) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.username = username;
		this.password = password;
	}
    public String getAccessToken() {
        String tokenUri = "https://na-1-dev.api.opentext.com/tenants/d7b35c2e-0ea6-4152-81a7-aeccbd59d2a3/oauth2/token";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		String body = "grant_type=password&username=" + username + "&password=" + password;
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
