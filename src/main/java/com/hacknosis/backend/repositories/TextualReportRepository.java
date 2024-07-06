package com.hacknosis.backend.repositories;

import com.hacknosis.backend.models.TextualReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TextualReportRepository extends JpaRepository<TextualReport, Long> {
    public List<TextualReport> findTextualReportByPatientId(long patientId);
}
