package com.monitor.repository;

import com.monitor.entity.ProcessInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessInfoRepository extends JpaRepository<ProcessInfo, Long> {
    List<ProcessInfo> findByAgentIdOrderByCollectedAtDesc(String agentId);
    List<ProcessInfo> findByAgentId(String agentId);
    void deleteByAgentId(String agentId);
}
