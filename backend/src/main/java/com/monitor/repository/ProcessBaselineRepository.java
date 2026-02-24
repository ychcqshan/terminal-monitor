package com.monitor.repository;

import com.monitor.entity.ProcessBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessBaselineRepository extends JpaRepository<ProcessBaseline, Long> {
    List<ProcessBaseline> findByAgentId(String agentId);
    Optional<ProcessBaseline> findByAgentIdAndProcessName(String agentId, String processName);
    void deleteByAgentId(String agentId);
}
