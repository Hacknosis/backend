package com.hacknosis.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportAnalysisResult {
    List<ResultEntity> entities = new ArrayList<>();
    @Builder
    @Data
    public static class ResultEntity {
        String text;
        List<ResultAttribute> attributes;
        List<Traits> traits;
    }
    @Builder
    @Data
    public static class ResultAttribute {
        String type;
        String text;
    }
    public enum Traits {
        PAST_HISTORY,
        NEGATION,
        DIAGNOSIS,
        SIGN,
        SYMPTOM
    }
}

