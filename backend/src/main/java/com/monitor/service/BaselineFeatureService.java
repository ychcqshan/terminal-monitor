package com.monitor.service;

import com.monitor.entity.ProcessBaseline;
import com.monitor.entity.ProcessHistory;
import com.monitor.entity.PortBaseline;
import com.monitor.entity.PortHistory;
import com.monitor.repository.ProcessBaselineRepository;
import com.monitor.repository.ProcessHistoryRepository;
import com.monitor.repository.PortBaselineRepository;
import com.monitor.repository.PortHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BaselineFeatureService {

    private static final Logger logger = LoggerFactory.getLogger(BaselineFeatureService.class);

    private static final double ALWAYS_THRESHOLD = 0.95;
    private static final double COMMON_THRESHOLD = 0.50;
    private static final double RARE_THRESHOLD = 0.10;

    private final ProcessHistoryRepository processHistoryRepository;
    private final PortHistoryRepository portHistoryRepository;
    private final ProcessBaselineRepository processBaselineRepository;
    private final PortBaselineRepository portBaselineRepository;

    public BaselineFeatureService(ProcessHistoryRepository processHistoryRepository,
                                PortHistoryRepository portHistoryRepository,
                                ProcessBaselineRepository processBaselineRepository,
                                PortBaselineRepository portBaselineRepository) {
        this.processHistoryRepository = processHistoryRepository;
        this.portHistoryRepository = portHistoryRepository;
        this.processBaselineRepository = processBaselineRepository;
        this.portBaselineRepository = portBaselineRepository;
    }

    @Transactional
    public void buildProcessBaseline(String agentId, int rounds) {
        logger.info("Building process baseline for agent {} using {} rounds", agentId, rounds);

        List<Integer> recentRounds = processHistoryRepository.findRecentRounds(agentId, rounds);
        if (recentRounds.isEmpty()) {
            logger.warn("No history data found for agent {}", agentId);
            return;
        }

        List<ProcessHistory> history = processHistoryRepository.findByAgentIdAndCollectionRoundIn(agentId, recentRounds);
        int totalRounds = recentRounds.size();

        Map<String, List<ProcessHistory>> groupedByName = history.stream()
                .collect(Collectors.groupingBy(ProcessHistory::getName));

        for (Map.Entry<String, List<ProcessHistory>> entry : groupedByName.entrySet()) {
            String processName = entry.getKey();
            List<ProcessHistory> processList = entry.getValue();

            double frequency = (double) processList.size() / totalRounds;
            String category = categorizeFrequency(frequency);

            LocalDateTime firstSeen = processList.stream()
                    .map(ProcessHistory::getCollectedAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            LocalDateTime lastSeen = processList.stream()
                    .map(ProcessHistory::getCollectedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            Double avgCpu = processList.stream()
                    .map(ProcessHistory::getCpuPercent)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            Double avgMemory = processList.stream()
                    .map(ProcessHistory::getMemoryPercent)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            Optional<ProcessBaseline> existing = processBaselineRepository
                    .findByAgentIdAndProcessName(agentId, processName);

            ProcessBaseline baseline;
            if (existing.isPresent()) {
                baseline = existing.get();
            } else {
                baseline = new ProcessBaseline();
                baseline.setAgentId(agentId);
                baseline.setProcessName(processName);
            }

            baseline.setFrequency(frequency);
            baseline.setFrequencyCategory(category);
            baseline.setFirstSeen(firstSeen);
            baseline.setLastSeen(lastSeen);
            baseline.setTotalAppearances(processList.size());
            baseline.setAvgCpuPercent(avgCpu);
            baseline.setAvgMemoryPercent(avgMemory);

            processBaselineRepository.save(baseline);
        }

        logger.info("Process baseline built for agent {} with {} entries", agentId, groupedByName.size());
    }

    @Transactional
    public void buildPortBaseline(String agentId, int rounds) {
        logger.info("Building port baseline for agent {} using {} rounds", agentId, rounds);

        List<Integer> recentRounds = portHistoryRepository.findRecentRounds(agentId, rounds);
        if (recentRounds.isEmpty()) {
            logger.warn("No port history data found for agent {}", agentId);
            return;
        }

        List<PortHistory> history = portHistoryRepository.findByAgentIdAndCollectionRoundIn(agentId, recentRounds);
        int totalRounds = recentRounds.size();

        Map<String, List<PortHistory>> groupedByKey = history.stream()
                .collect(Collectors.groupingBy(h -> h.getPort() + ":" + h.getProtocol()));

        for (Map.Entry<String, List<PortHistory>> entry : groupedByKey.entrySet()) {
            String key = entry.getKey();
            List<PortHistory> portList = entry.getValue();

            PortHistory first = portList.get(0);
            double frequency = (double) portList.size() / totalRounds;
            String category = categorizeFrequency(frequency);

            LocalDateTime firstSeen = portList.stream()
                    .map(PortHistory::getCollectedAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            LocalDateTime lastSeen = portList.stream()
                    .map(PortHistory::getCollectedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            Optional<PortBaseline> existing = portBaselineRepository
                    .findByAgentIdAndPortAndProtocol(agentId, first.getPort(), first.getProtocol());

            PortBaseline baseline;
            if (existing.isPresent()) {
                baseline = existing.get();
            } else {
                baseline = new PortBaseline();
                baseline.setAgentId(agentId);
                baseline.setPort(first.getPort());
                baseline.setProtocol(first.getProtocol());
            }

            baseline.setFrequency(frequency);
            baseline.setFrequencyCategory(category);
            baseline.setFirstSeen(firstSeen);
            baseline.setLastSeen(lastSeen);
            baseline.setTotalAppearances(portList.size());

            portBaselineRepository.save(baseline);
        }

        logger.info("Port baseline built for agent {} with {} entries", agentId, groupedByKey.size());
    }

    private String categorizeFrequency(double frequency) {
        if (frequency >= ALWAYS_THRESHOLD) return "ALWAYS";
        if (frequency >= COMMON_THRESHOLD) return "COMMON";
        if (frequency >= RARE_THRESHOLD) return "RARE";
        return "NEW";
    }

    public List<ProcessBaseline> getProcessBaseline(String agentId) {
        return processBaselineRepository.findByAgentId(agentId);
    }

    public List<PortBaseline> getPortBaseline(String agentId) {
        return portBaselineRepository.findByAgentId(agentId);
    }
}
