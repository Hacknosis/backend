package com.hacknosis.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacknosis.backend.exceptions.ReportProcessingException;
import com.hacknosis.backend.exceptions.ResourceNotFoundException;
import com.hacknosis.backend.utils.OAuth2TokenUtil;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@Log4j2
@AllArgsConstructor
public class OpenTextService {
    private OAuth2TokenUtil oAuth2TokenUtil;
    public String uploadDocumentToContentStorage(byte[] fileByte, String filename) throws IOException {
        String url = "/v2/content";
        WebClient client = buildWebClient("https://css.na-1-dev.api.opentext.com");
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("name", new ByteArrayResource(fileByte))
                .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"name\"; filename=\"" + filename + "\"");

        WebClient.ResponseSpec spec = client.post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve();

        String result = retrieveResult(spec, String.class);

        // get the content id
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(result);

        JsonNode idNode = jsonNode.path("entries").get(0).path("id");
        String contentId = idNode.asText();
        return contentId;
    }

    public String publishDocument(String contentId) throws JsonProcessingException {
        String url = "/publications";
        String publicationJSONString = "{\n" +
                "  \"publicationVersion\": \"1.0\",\n" +
                "  \"policy\": {\n" +
                "    \"namespace\": \"opentext.publishing.brava\",\n" +
                "    \"name\": \"SimpleBravaView\",\n" +
                "    \"version\": \"1.x\"\n" +
                "  },\n" +
                "  \"featureSettings\": [\n" +
                "    {\n" +
                "      \"feature\": { \"namespace\": \"opentext.publishing.sources\", \"name\": \"LoadSources\" },\n" +
                "      \"path\": \"/documents\",\n" +
                "      \"value\": [{ \"url\": \"https://css.na-1-dev.api.opentext.com/v2/content/{contentId}/download\", \"formatHint\": \"text\", \"filenameHint\": \"medical_report.txt\" }]\n" +
                "    },\n" +
                "    {\n" +
                "      \"feature\": { \"namespace\": \"opentext.publishing.execution\", \"name\": \"SetPublishingTarget\" },\n" +
                "      \"path\": \"/publishingTarget\",\n" +
                "      \"value\": \"https://css.na-1-dev.api.opentext.com/v2/content/{contentId}/renditions\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String replacedJSONString = publicationJSONString.replaceAll("\\{contentId\\}", contentId);

        WebClient client = buildWebClient("https://na-1-dev.api.opentext.com/publication/api/v1");

        WebClient.ResponseSpec spec = client.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(replacedJSONString)
                .retrieve();

        String result = retrieveResult(spec, String.class);

        // get the publication id
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(result);

        JsonNode idNode = jsonNode.path("id");
        String publicationId = idNode.asText();
        return publicationId;
    }

    public String getPublication(String publicationId) {
        String url = "/publications/" + publicationId + "?embed=page_links";
        log.info("Publication Read url - {}", url);
        WebClient client = buildWebClient("https://na-1-dev.api.opentext.com/publication/api/v1");

        // get publication resource object
        WebClient.ResponseSpec spec = client.get()
                .uri(url)
                .retrieve();
        return retrieveResult(spec, String.class);
    }

    public <T> T retrieveResult(WebClient.ResponseSpec spec, Class<T> responseType) {
        return spec.onStatus(HttpStatus::is4xxClientError, clientResponse -> {
                    if (clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        throw new ResourceNotFoundException("The specified resource is not found");
                    } else {
                        throw new ReportProcessingException("Unknown Error");
                    }
                })
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> {
                    String errorBody = clientResponse.bodyToMono(String.class).block();
                    log.error("5xx Server Error: {}", errorBody);
                    throw new ReportProcessingException("Unknown error while processing the report");
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
                        conn.addHandlerLast(new ReadTimeoutHandler(50, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(50, TimeUnit.SECONDS)));

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + oAuth2TokenUtil.getAccessToken())
                .build();

        return client;
    }
}
