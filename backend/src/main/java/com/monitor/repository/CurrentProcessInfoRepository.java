package com.monitor.repository;

import com.monitor.entity.CurrentProcessInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentProcessInfoRepository extends JpaRepository<CurrentProcessInfo, Long> {
    List<CurrentProcessInfo> findByAgentIdOrderByCollectedAtDesc(String agentId);
    void deleteByAgentId(String agentId);
}
