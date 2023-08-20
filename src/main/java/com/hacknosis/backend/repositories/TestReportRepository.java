package com.hacknosis.backend.repositories;

import com.hacknosis.backend.models.TestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {
    public List<TestReport> findTestReportByPatientId(long patientId);
}
