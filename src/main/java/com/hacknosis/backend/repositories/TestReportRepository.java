package com.hacknosis.backend.repositories;

import com.hacknosis.backend.models.TestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Long> {

}
