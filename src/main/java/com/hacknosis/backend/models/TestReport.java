package com.hacknosis.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "test_reports")
public class TestReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull(message = "Report type cannot be null")
    @Enumerated(EnumType.STRING)
    private ReportType type;

    @NotNull(message = "Test data cannot be null")
    private String testData;

    @NotNull(message = "Report status cannot be null")
    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    @NotNull(message = "Analysis result must be not null")
    private String analysisResult;
}
