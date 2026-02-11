package com.monitor.repository;

import com.monitor.entity.BaselineSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BaselineSnapshotRepository extends JpaRepository<BaselineSnapshot, Long> {

    List<BaselineSnapshot> findByAgentIdAndBaselineTypeOrderByCreatedAtDesc(String agentId, String baselineType);

    List<BaselineSnapshot> findByAgentIdAndBaselineType(String agentId, String baselineType);

    Optional<BaselineSnapshot> findFirstByAgentIdAndBaselineTypeOrderByCreatedAtDesc(String agentId, String baselineType);

    List<BaselineSnapshot> findByAgentId(String agentId);

    void deleteByAgentIdAndBaselineType(String agentId, String baselineType);
}
