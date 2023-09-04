package com.hacknosis.backend.models;

import lombok.Getter;

@Getter
public class ReportData {
    private String issueDescription;
    private String screenshotData;
    private String timestamp;

    public ReportData(String issueDescription, String screenshotData, String timestamp) {
        this.issueDescription = issueDescription;
        this.screenshotData = screenshotData;
        this.timestamp = timestamp;
    }

}
