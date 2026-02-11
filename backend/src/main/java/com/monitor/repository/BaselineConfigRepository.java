package com.monitor.repository;

import com.monitor.entity.BaselineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BaselineConfigRepository extends JpaRepository<BaselineConfig, Long> {

    Optional<BaselineConfig> findByAgentIdAndBaselineType(String agentId, String baselineType);

    List<BaselineConfig> findByAgentId(String agentId);

    List<BaselineConfig> findByAgentIdAndStatus(String agentId, String status);

    List<BaselineConfig> findByStatus(String status);

    boolean existsByAgentIdAndBaselineType(String agentId, String baselineType);

    void deleteByAgentIdAndBaselineType(String agentId, String baselineType);
}
