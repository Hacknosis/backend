package com.hacknosis.backend.services;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedical;
import com.amazonaws.services.comprehendmedical.AWSComprehendMedicalClient;
import com.amazonaws.services.comprehendmedical.model.*;
import com.hacknosis.backend.dto.ReportAnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AWSMedicalService {
    private final String awsAccessKey;
    private final String awsSecretKey;

    private final static List<String> ignoreEntity = new ArrayList<>(Arrays.asList("PROTECTED_HEALTH_INFORMATION"));

    @Autowired
    public AWSMedicalService(@Value("${aws_access_key}") String awsAccessKey, @Value("${aws_secret_key}") String awsSecretKey) {
        this.awsAccessKey = awsAccessKey;
        this.awsSecretKey = awsSecretKey;
    }
    public ReportAnalysisResult entityDetection(String text) {
        AWSComprehendMedical client = buildAWSClient();

        DetectEntitiesV2Request request = new DetectEntitiesV2Request();
        request.setText(text);

        DetectEntitiesV2Result result = client.detectEntitiesV2(request);

        return parseEntityDetectionAnalysisResult(result);
    }

    public ReportAnalysisResult ontologyLinking(String text) {
        AWSComprehendMedical client = buildAWSClient();

        InferICD10CMRequest request = new InferICD10CMRequest();
        request.setText(text);

        InferICD10CMResult result = client.inferICD10CM(request);

        return parseOntologyAnalysisResult(result);
    }

    public ReportAnalysisResult parseEntityDetectionAnalysisResult(DetectEntitiesV2Result result) {
        List<Entity> entities = result.getEntities();
        ReportAnalysisResult reportAnalysisResult = new ReportAnalysisResult();
        for (Entity val : entities) {
            if (ignoreEntity.contains(val.getCategory())) continue;
            List<Attribute> attributes = val.getAttributes();
            List<Trait> traits = val.getTraits();
            ReportAnalysisResult.ResultEntity resultEntity = ReportAnalysisResult.ResultEntity.builder()
                    .text(val.getText())
                    .attributes(new ArrayList<>())
                    .traits(new ArrayList<>())
                    .build();
            if (attributes != null) {
                attributes.forEach((attribute -> {
                    ReportAnalysisResult.ResultAttribute resultAttribute = ReportAnalysisResult.ResultAttribute.builder()
                            .type(attribute.getType())
                            .text(attribute.getText())
                            .build();
                    resultEntity.getAttributes().add(resultAttribute);
                }));
            }
            if (traits != null) {
                traits.forEach(trait -> {
                    if (Arrays.stream(ReportAnalysisResult.Traits.values()).anyMatch(v -> v.name().equals(trait.getName()))) {
                        ReportAnalysisResult.Traits resultTrait = ReportAnalysisResult.Traits.valueOf(trait.getName());
                        resultEntity.getTraits().add(resultTrait);
                    }
                });
            }
            reportAnalysisResult.getEntities().add(resultEntity);
        }
        return reportAnalysisResult;
    }

    public ReportAnalysisResult parseOntologyAnalysisResult(InferICD10CMResult result) {
        List<ICD10CMEntity> entities = result.getEntities();
        ReportAnalysisResult reportAnalysisResult = new ReportAnalysisResult();
        for (ICD10CMEntity val : entities) {
            if (ignoreEntity.contains(val.getCategory())) continue;
            List<ICD10CMAttribute> attributes = val.getAttributes();
            List<ICD10CMTrait> traits = val.getTraits();
            ReportAnalysisResult.ResultEntity resultEntity = ReportAnalysisResult.ResultEntity.builder()
                    .text(val.getText())
                    .attributes(new ArrayList<>())
                    .traits(new ArrayList<>())
                    .build();

            attributes.forEach((attribute -> {
                ReportAnalysisResult.ResultAttribute resultAttribute = ReportAnalysisResult.ResultAttribute.builder()
                        .type(attribute.getType())
                        .text(attribute.getText())
                        .build();
                resultEntity.getAttributes().add(resultAttribute);
            }));
            traits.forEach(trait -> {
                if (Arrays.stream(ReportAnalysisResult.Traits.values()).anyMatch(v -> v.name().equals(trait.getName()))) {
                    ReportAnalysisResult.Traits resultTrait = ReportAnalysisResult.Traits.valueOf(trait.getName());
                    resultEntity.getTraits().add(resultTrait);
                }
            });
            reportAnalysisResult.getEntities().add(resultEntity);
        }
        return reportAnalysisResult;
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
