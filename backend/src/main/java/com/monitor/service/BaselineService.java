package com.monitor.service;

import com.monitor.entity.*;
import com.monitor.entity.dto.*;
import com.monitor.repository.*;
import com.monitor.repository.ProcessHistoryRepository;
import com.monitor.repository.PortHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BaselineService {

    private static final Logger logger = LoggerFactory.getLogger(BaselineService.class);

    private final BaselineConfigRepository configRepository;
    private final BaselineSnapshotRepository snapshotRepository;
    private final BaselineItemRepository itemRepository;
    private final HostInfoRepository hostInfoRepository;
    private final CurrentProcessInfoRepository currentProcessInfoRepository;
    private final CurrentPortInfoRepository currentPortInfoRepository;
    private final ProcessHistoryRepository processHistoryRepository;
    private final PortHistoryRepository portHistoryRepository;
    private final UsbDeviceRepository usbDeviceRepository;
    private final LoginLogRepository loginLogRepository;
    private final InstalledSoftwareRepository installedSoftwareRepository;

    public BaselineService(BaselineConfigRepository configRepository,
                          BaselineSnapshotRepository snapshotRepository,
                          BaselineItemRepository itemRepository,
                          HostInfoRepository hostInfoRepository,
                          CurrentProcessInfoRepository currentProcessInfoRepository,
                          CurrentPortInfoRepository currentPortInfoRepository,
                          ProcessHistoryRepository processHistoryRepository,
                          PortHistoryRepository portHistoryRepository,
                          UsbDeviceRepository usbDeviceRepository,
                          LoginLogRepository loginLogRepository,
                          InstalledSoftwareRepository installedSoftwareRepository) {
        this.configRepository = configRepository;
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
        this.hostInfoRepository = hostInfoRepository;
        this.currentProcessInfoRepository = currentProcessInfoRepository;
        this.currentPortInfoRepository = currentPortInfoRepository;
        this.processHistoryRepository = processHistoryRepository;
        this.portHistoryRepository = portHistoryRepository;
        this.usbDeviceRepository = usbDeviceRepository;
        this.loginLogRepository = loginLogRepository;
        this.installedSoftwareRepository = installedSoftwareRepository;
    }

    // ===== 学习模式 =====
    public BaselineConfig startQuickLearn(String agentId, String type) {
        return startLearn(agentId, type, "QUICK", 0);
    }

    public BaselineConfig startStandardLearn(String agentId, String type) {
        return startLearn(agentId, type, "STANDARD", 7);
    }

    public BaselineConfig startCustomLearn(String agentId, String type, int days) {
        return startLearn(agentId, type, "CUSTOM", days);
    }

    private BaselineConfig startLearn(String agentId, String type, String mode, int days) {
        logger.info("Starting {} learning for agent {} type {}", mode, agentId, type);

        Optional<BaselineConfig> existingConfig = configRepository.findByAgentIdAndBaselineType(agentId, type);

        BaselineConfig config;
        if (existingConfig.isPresent()) {
            config = existingConfig.get();
            config.setStatus("LEARNING");
            config.setLearningMode(mode);
            config.setLearningDays(days > 0 ? days : (mode.equals("STANDARD") ? 7 : 1));
            config.setLearnStart(LocalDateTime.now());
            config.setLearnEnd(LocalDateTime.now().plusDays(config.getLearningDays()));
        } else {
            config = new BaselineConfig();
            config.setAgentId(agentId);
            config.setBaselineType(type);
            config.setStatus("LEARNING");
            config.setLearningMode(mode);
            config.setLearningDays(days > 0 ? days : (mode.equals("STANDARD") ? 7 : 1));
            config.setLearnStart(LocalDateTime.now());
            config.setLearnEnd(LocalDateTime.now().plusDays(config.getLearningDays()));
        }

        return configRepository.save(config);
    }

    // ===== 手动创建 =====
    public BaselineConfig importFromCurrent(String agentId, String type) {
        logger.info("Importing current data as baseline for agent {} type {}", agentId, type);

        List<Map<String, Object>> currentData = getCurrentData(agentId, type);

        return createManualBaseline(agentId, type, currentData, "IMPORT", null);
    }

    public BaselineConfig copyFromAgent(String sourceAgentId, String targetAgentId, String type) {
        logger.info("Copying baseline from agent {} to {} type {}", sourceAgentId, targetAgentId, type);

        Optional<BaselineSnapshot> sourceSnapshot = snapshotRepository
                .findFirstByAgentIdAndBaselineTypeOrderByCreatedAtDesc(sourceAgentId, type);

        if (sourceSnapshot.isEmpty()) {
            throw new RuntimeException("Source agent has no baseline for type: " + type);
        }

        List<BaselineItem> sourceItems = itemRepository.findBySnapshotId(sourceSnapshot.get().getId());
        List<Map<String, Object>> itemsData = sourceItems.stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("itemKey", item.getItemKey());
                    map.put("itemValue", item.getItemValue());
                    map.put("itemType", item.getItemType());
                    return map;
                })
                .collect(Collectors.toList());

        return createManualBaseline(targetAgentId, type, itemsData, "COPY", sourceAgentId);
    }

    public BaselineConfig manualCreate(String agentId, String type, List<BaselineItemDTO> items) {
        logger.info("Manual baseline creation for agent {} type {} with {} items", agentId, type, items.size());

        List<Map<String, Object>> itemsData = items.stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("itemKey", item.getItemKey());
                    map.put("itemValue", item.getItemValue());
                    map.put("itemType", item.getItemType());
                    return map;
                })
                .collect(Collectors.toList());

        return createManualBaseline(agentId, type, itemsData, "MANUAL", null);
    }

    private BaselineConfig createManualBaseline(String agentId, String type,
                                                List<Map<String, Object>> itemsData,
                                                String createdType, String sourceAgentId) {
        Optional<BaselineConfig> existingConfig = configRepository.findByAgentIdAndBaselineType(agentId, type);

        BaselineConfig config;
        if (existingConfig.isPresent()) {
            config = existingConfig.get();
            config.setStatus("ACTIVE");
            config.setLearningMode("MANUAL");
            config.setCreatedType(createdType);
            config.setSourceAgentId(sourceAgentId);
            config.setLearnStart(LocalDateTime.now());
            config.setLearnEnd(LocalDateTime.now());
        } else {
            config = new BaselineConfig();
            config.setAgentId(agentId);
            config.setBaselineType(type);
            config.setStatus("ACTIVE");
            config.setLearningMode("MANUAL");
            config.setCreatedType(createdType);
            config.setSourceAgentId(sourceAgentId);
            config.setLearnStart(LocalDateTime.now());
            config.setLearnEnd(LocalDateTime.now());
        }

        config = configRepository.save(config);

        createSnapshot(agentId, type, config.getId(), itemsData);

        return config;
    }

    // ===== 快照管理 =====
    @Transactional
    public BaselineSnapshot createSnapshot(String agentId, String type, Long configId, List<Map<String, Object>> itemsData) {
        logger.info("Creating snapshot for agent {} type {}", agentId, type);

        List<BaselineItem> items = new ArrayList<>();
        StringBuilder hashContent = new StringBuilder();

        for (Map<String, Object> itemData : itemsData) {
            String itemKey = String.valueOf(itemData.get("itemKey"));
            String itemValue = String.valueOf(itemData.getOrDefault("itemValue", ""));
            String itemType = String.valueOf(itemData.getOrDefault("itemType", type.toLowerCase()));

            BaselineItem item = new BaselineItem();
            item.setSnapshotId(0L);
            item.setItemKey(itemKey);
            item.setItemValue(itemValue);
            item.setItemType(itemType);
            item.setItemHash(computeHash(itemKey + itemValue));

            items.add(item);
            hashContent.append(itemKey).append(itemValue);
        }

        String snapshotHash = computeHash(hashContent.toString());

        BaselineSnapshot snapshot = new BaselineSnapshot();
        snapshot.setAgentId(agentId);
        snapshot.setBaselineType(type);
        snapshot.setConfigId(configId);
        snapshot.setSnapshotHash(snapshotHash);
        snapshot.setItemCount(items.size());
        snapshot.setValidFrom(LocalDateTime.now());

        snapshot = snapshotRepository.save(snapshot);

        for (BaselineItem item : items) {
            item.setSnapshotId(snapshot.getId());
        }
        itemRepository.saveAll(items);

        logger.info("Snapshot created with {} items, hash: {}", items.size(), snapshotHash);
        return snapshot;
    }

    public void completeLearning(String agentId, String type) {
        configRepository.findByAgentIdAndBaselineType(agentId, type).ifPresent(config -> {
            if ("LEARNING".equals(config.getStatus())) {
                config.setStatus("ACTIVE");
                configRepository.save(config);
                logger.info("Learning completed for agent {} type {}", agentId, type);

                createSnapshotFromHistory(agentId, type, config.getId());
            }
        });
    }

    private void createSnapshotFromHistory(String agentId, String type, Long configId) {
        configRepository.findByAgentIdAndBaselineType(agentId, type).ifPresent(cfg -> {
            LocalDateTime since = LocalDateTime.now().minusDays(cfg.getLearningDays() != null ? cfg.getLearningDays() : 7);
            List<Map<String, Object>> historicalData = getHistoricalData(agentId, type, since);

            if (!historicalData.isEmpty()) {
                createSnapshot(agentId, type, configId, historicalData);
            } else {
                logger.warn("No historical data found for agent {} type {}", agentId, type);
            }
        });
    }

    // ===== 查询方法 =====
    public List<BaselineConfig> getBaselineConfigs(String agentId) {
        return configRepository.findByAgentId(agentId);
    }

    public Optional<BaselineConfig> getBaselineConfig(String agentId, String type) {
        return configRepository.findByAgentIdAndBaselineType(agentId, type);
    }

    public List<BaselineSnapshot> getSnapshots(String agentId, String type) {
        return snapshotRepository.findByAgentIdAndBaselineTypeOrderByCreatedAtDesc(agentId, type);
    }

    public List<BaselineItem> getBaselineItems(String agentId, String type) {
        Optional<BaselineSnapshot> snapshot = snapshotRepository
                .findFirstByAgentIdAndBaselineTypeOrderByCreatedAtDesc(agentId, type);
        if (snapshot.isPresent()) {
            return itemRepository.findBySnapshotId(snapshot.get().getId());
        }
        return Collections.emptyList();
    }

    public BaselineCompareResult compareWithBaseline(String agentId, String type) {
        List<Map<String, Object>> currentData = getCurrentData(agentId, type);
        List<BaselineItem> baselineItems = getBaselineItems(agentId, type);

        Set<String> baselineKeys = baselineItems.stream()
                .map(BaselineItem::getItemKey)
                .collect(Collectors.toSet());

        Map<String, BaselineItem> baselineMap = baselineItems.stream()
                .collect(Collectors.toMap(BaselineItem::getItemKey, i -> i));

        List<AnomalyDTO> newItems = new ArrayList<>();
        List<AnomalyDTO> missingItems = new ArrayList<>();
        List<AnomalyDTO> modifiedItems = new ArrayList<>();

        for (Map<String, Object> current : currentData) {
            String itemKey = String.valueOf(current.get("itemKey"));
            BaselineItem baseline = baselineMap.get(itemKey);

            if (baseline == null) {
                AnomalyDTO anomaly = new AnomalyDTO();
                anomaly.setAnomalyType("NEW");
                anomaly.setItemKey(itemKey);
                anomaly.setCurrentValue(current);
                anomaly.setAlertLevel("MEDIUM");
                newItems.add(anomaly);
            } else if (!Objects.equals(current.get("itemValue"), baseline.getItemValue())) {
                AnomalyDTO anomaly = new AnomalyDTO();
                anomaly.setAnomalyType("MODIFIED");
                anomaly.setItemKey(itemKey);
                anomaly.setCurrentValue(current);

                Map<String, Object> baselineValue = new HashMap<>();
                baselineValue.put("itemValue", baseline.getItemValue());
                baselineValue.put("itemType", baseline.getItemType());
                anomaly.setBaselineValue(baselineValue);
                anomaly.setAlertLevel("LOW");
                modifiedItems.add(anomaly);
            }
        }

        for (BaselineItem baseline : baselineItems) {
            boolean exists = currentData.stream()
                    .anyMatch(c -> String.valueOf(c.get("itemKey")).equals(baseline.getItemKey()));

            if (!exists) {
                AnomalyDTO anomaly = new AnomalyDTO();
                anomaly.setAnomalyType("MISSING");
                anomaly.setItemKey(baseline.getItemKey());

                Map<String, Object> baselineValue = new HashMap<>();
                baselineValue.put("itemValue", baseline.getItemValue());
                baselineValue.put("itemType", baseline.getItemType());
                anomaly.setBaselineValue(baselineValue);
                anomaly.setAlertLevel("LOW");
                missingItems.add(anomaly);
            }
        }

        BaselineCompareResult result = new BaselineCompareResult();
        result.setBaselineType(type);
        result.setNewItemsCount(newItems.size());
        result.setMissingItemsCount(missingItems.size());
        result.setModifiedItemsCount(modifiedItems.size());
        result.setNewItems(newItems);
        result.setMissingItems(missingItems);
        result.setModifiedItems(modifiedItems);

        return result;
    }

    // ===== 删除基线 =====
    public void deleteBaseline(String agentId, String type) {
        Optional<BaselineConfig> config = configRepository.findByAgentIdAndBaselineType(agentId, type);
        if (config.isPresent()) {
            List<BaselineSnapshot> snapshots = snapshotRepository.findByAgentIdAndBaselineType(agentId, type);
            for (BaselineSnapshot snapshot : snapshots) {
                itemRepository.deleteBySnapshotId(snapshot.getId());
            }
            snapshotRepository.deleteByAgentIdAndBaselineType(agentId, type);
            configRepository.delete(config.get());
            logger.info("Baseline deleted for agent {} type {}", agentId, type);
        }
    }

    // ===== 辅助方法 =====
    private List<Map<String, Object>> getCurrentData(String agentId, String type) {
        switch (type.toUpperCase()) {
            case "PROCESS":
                return currentProcessInfoRepository.findByAgentIdOrderByCollectedAtDesc(agentId).stream()
                        .limit(500)
                        .map(this::currentProcessToMap)
                        .collect(Collectors.toList());
            case "PORT":
                return currentPortInfoRepository.findByAgentIdOrderByCollectedAtDesc(agentId).stream()
                        .limit(500)
                        .map(this::currentPortToMap)
                        .collect(Collectors.toList());
            case "USB":
                return usbDeviceRepository.findByAgentIdOrderByCollectedAtDesc(agentId).stream()
                        .limit(500)
                        .map(this::usbToMap)
                        .collect(Collectors.toList());
            case "LOGIN":
                return loginLogRepository.findByAgentIdOrderByLoginTimeDesc(agentId).stream()
                        .limit(500)
                        .map(this::loginToMap)
                        .collect(Collectors.toList());
            case "SOFTWARE":
                return installedSoftwareRepository.findByAgentIdOrderByCollectedAtDesc(agentId).stream()
                        .limit(500)
                        .map(this::softwareToMap)
                        .collect(Collectors.toList());
            default:
                return Collections.emptyList();
        }
    }

    private List<Map<String, Object>> getHistoricalData(String agentId, String type, LocalDateTime since) {
        switch (type.toUpperCase()) {
            case "PROCESS":
                return processHistoryRepository.findByAgentId(agentId).stream()
                        .filter(p -> p.getCollectedAt() != null && p.getCollectedAt().isAfter(since))
                        .map(this::historyProcessToMap)
                        .collect(Collectors.toList());
            case "PORT":
                return portHistoryRepository.findByAgentId(agentId).stream()
                        .filter(p -> p.getCollectedAt() != null && p.getCollectedAt().isAfter(since))
                        .map(this::historyPortToMap)
                        .collect(Collectors.toList());
            default:
                return getCurrentData(agentId, type);
        }
    }

    private Map<String, Object> processToMap(ProcessInfo p) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", p.getPid() + ":" + p.getName());
        map.put("itemValue", p.getName() + "|" + p.getPid() + "|" + p.getCpuPercent() + "|" + p.getMemoryPercent());
        map.put("itemType", "process");
        return map;
    }

    private Map<String, Object> portToMap(PortInfo p) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", p.getPort() + ":" + p.getProtocol());
        map.put("itemValue", p.getPort() + "|" + p.getProtocol() + "|" + p.getStatus() + "|" + p.getProcessName());
        map.put("itemType", "port");
        return map;
    }

    private Map<String, Object> usbToMap(UsbDevice u) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", u.getDeviceName() + ":" + u.getSerialNumber());
        map.put("itemValue", u.getDeviceName() + "|" + u.getDeviceType() + "|" + u.getManufacturer() + "|" + u.getSerialNumber());
        map.put("itemType", "usb");
        return map;
    }

    private Map<String, Object> loginToMap(LoginLog l) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", l.getUsername() + ":" + l.getLoginType());
        map.put("itemValue", l.getUsername() + "|" + l.getLoginType() + "|" + l.getLoginIp() + "|" + l.getLoginTime());
        map.put("itemType", "login");
        return map;
    }

    private Map<String, Object> currentProcessToMap(CurrentProcessInfo p) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", p.getPid() + ":" + p.getName());
        map.put("itemValue", p.getName() + "|" + p.getPid() + "|" + p.getCpuPercent() + "|" + p.getMemoryPercent());
        map.put("itemType", "process");
        return map;
    }

    private Map<String, Object> currentPortToMap(CurrentPortInfo p) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", p.getPort() + ":" + p.getProtocol());
        map.put("itemValue", p.getPort() + "|" + p.getProtocol() + "|" + p.getStatus() + "|" + p.getProcessName());
        map.put("itemType", "port");
        return map;
    }

    private Map<String, Object> historyProcessToMap(ProcessHistory p) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", p.getPid() + ":" + p.getName());
        map.put("itemValue", p.getName() + "|" + p.getPid() + "|" + p.getCpuPercent() + "|" + p.getMemoryPercent());
        map.put("itemType", "process");
        map.put("collectionRound", p.getCollectionRound());
        return map;
    }

    private Map<String, Object> historyPortToMap(PortHistory p) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", p.getPort() + ":" + p.getProtocol());
        map.put("itemValue", p.getPort() + "|" + p.getProtocol() + "|" + p.getStatus() + "|" + p.getProcessName());
        map.put("itemType", "port");
        map.put("collectionRound", p.getCollectionRound());
        return map;
    }

    private Map<String, Object> softwareToMap(InstalledSoftware s) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", s.getSoftwareName());
        map.put("itemValue", s.getSoftwareName() + "|" + s.getVersion() + "|" + s.getPublisher());
        map.put("itemType", "software");
        return map;
    }

    private String computeHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(content.hashCode());
        }
    }

    private BaselineConfig getConfig() { return new BaselineConfig(); }
}
