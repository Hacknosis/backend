package com.hacknosis.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Date;

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

    //@NotNull(message = "Report status cannot be null")
    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    @Lob // Use the @Lob annotation
    @Column(columnDefinition = "TEXT")
    private String entityDetectionAnalysisResult;

    @Lob // Use the @Lob annotation
    @Column(columnDefinition = "TEXT")
    private String ontologyLinkingAnalysisResult;

    private String contentId;

    private String publicationId;

    private LocalDateTime date;
}
