package com.example.reportingservice.repositories;

import com.example.reportingservice.entities.ReportingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportingRepository extends JpaRepository<ReportingEntity, Long> {
    Optional<ReportingEntity> findTopByReportTypeOrderByGeneratedAtDesc(String reportType);
}
