package com.homely.rental.interaction.repository;

import com.homely.rental.interaction.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);
}
