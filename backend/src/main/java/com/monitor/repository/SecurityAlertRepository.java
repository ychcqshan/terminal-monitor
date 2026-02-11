package com.monitor.repository;

import com.monitor.entity.SecurityAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    List<SecurityAlert> findByAgentIdOrderByCreatedAtDesc(String agentId);

    Page<SecurityAlert> findByAgentIdOrderByCreatedAtDesc(String agentId, Pageable pageable);

    Page<SecurityAlert> findByAlertStatusOrderByCreatedAtDesc(String alertStatus, Pageable pageable);

    List<SecurityAlert> findByAgentIdAndAlertStatus(String agentId, String alertStatus);

    List<SecurityAlert> findByAlertStatusOrderByCreatedAtDesc(String alertStatus);

    List<SecurityAlert> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(a) FROM SecurityAlert a WHERE a.agentId = :agentId AND a.alertStatus = :status")
    long countByAgentIdAndStatus(@Param("agentId") String agentId, @Param("status") String status);

    @Query("SELECT COUNT(a) FROM SecurityAlert a WHERE a.alertStatus = 'NEW'")
    long countNewAlerts();

    @Query("SELECT COUNT(a) FROM SecurityAlert a WHERE a.agentId = :agentId AND a.alertStatus = 'NEW'")
    long countNewAlertsByAgent(@Param("agentId") String agentId);

    @Query("SELECT COUNT(a) FROM SecurityAlert a WHERE a.alertLevel = 'CRITICAL' AND a.alertStatus != 'RESOLVED'")
    long countCriticalUnresolved();

    @Query("SELECT COUNT(a) FROM SecurityAlert a WHERE a.alertStatus = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT a FROM SecurityAlert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<SecurityAlert> findRecentAlerts(@Param("since") LocalDateTime since);

    void deleteByAgentId(String agentId);
}
