package com.monitor.repository;

import com.monitor.entity.UsbDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsbDeviceRepository extends JpaRepository<UsbDevice, Long> {

    List<UsbDevice> findByAgentIdOrderByCollectedAtDesc(String agentId);

    List<UsbDevice> findByAgentIdAndDeviceType(String agentId, String deviceType);

    void deleteByAgentId(String agentId);
}
