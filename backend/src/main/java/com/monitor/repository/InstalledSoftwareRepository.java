package com.monitor.repository;

import com.monitor.entity.InstalledSoftware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstalledSoftwareRepository extends JpaRepository<InstalledSoftware, Long> {

    List<InstalledSoftware> findByAgentIdOrderByCollectedAtDesc(String agentId);

    List<InstalledSoftware> findByAgentIdAndSoftwareType(String agentId, String softwareType);

    long countByAgentId(String agentId);

    void deleteByAgentId(String agentId);
}
