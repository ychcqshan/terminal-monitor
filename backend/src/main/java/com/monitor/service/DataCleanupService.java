package com.monitor.service;

import com.monitor.repository.ProcessHistoryRepository;
import com.monitor.repository.PortHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DataCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DataCleanupService.class);

    private final ProcessHistoryRepository processHistoryRepository;
    private final PortHistoryRepository portHistoryRepository;

    @Value("${monitor.data.retention-days:30}")
    private int retentionDays;

    public DataCleanupService(ProcessHistoryRepository processHistoryRepository,
                               PortHistoryRepository portHistoryRepository) {
        this.processHistoryRepository = processHistoryRepository;
        this.portHistoryRepository = portHistoryRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldData() {
        logger.info("Starting scheduled data cleanup for records older than {} days", retentionDays);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        int deletedProcesses = processHistoryRepository.deleteAllByCollectedAtBefore(cutoffDate);
        logger.info("Deleted {} process history records older than {}", deletedProcesses, cutoffDate);
        
        int deletedPorts = portHistoryRepository.deleteAllByCollectedAtBefore(cutoffDate);
        logger.info("Deleted {} port history records older than {}", deletedPorts, cutoffDate);
        
        logger.info("Scheduled data cleanup completed");
    }

    @Transactional
    public int cleanupProcessHistory(LocalDateTime before) {
        return processHistoryRepository.deleteAllByCollectedAtBefore(before);
    }

    @Transactional
    public int cleanupPortHistory(LocalDateTime before) {
        return portHistoryRepository.deleteAllByCollectedAtBefore(before);
    }
}
