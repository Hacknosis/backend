package com.hacknosis.backend.repositories;

import com.hacknosis.backend.models.ImageReport;
import com.hacknosis.backend.models.TextualReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageReportRepository extends JpaRepository<ImageReport, Long> {
    public List<ImageReport> findImageReportByPatientId(long patientId);
}
