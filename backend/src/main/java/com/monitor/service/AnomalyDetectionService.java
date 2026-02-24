package com.monitor.service;

import com.monitor.entity.BaselineItem;
import com.monitor.entity.BaselineConfig;
import com.monitor.entity.SecurityAlert;
import com.monitor.entity.dto.AnomalyDTO;
import com.monitor.entity.dto.BaselineCompareResult;
import com.monitor.repository.BaselineConfigRepository;
import com.monitor.repository.BaselineItemRepository;
import com.monitor.repository.BaselineSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnomalyDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final BaselineConfigRepository configRepository;
    private final BaselineSnapshotRepository snapshotRepository;
    private final BaselineItemRepository itemRepository;
    private final AlertService alertService;

    public AnomalyDetectionService(BaselineConfigRepository configRepository,
                                   BaselineSnapshotRepository snapshotRepository,
                                   BaselineItemRepository itemRepository,
                                   AlertService alertService) {
        this.configRepository = configRepository;
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
        this.alertService = alertService;
    }

    @Transactional
    public void detectAndAlert(String agentId, String dataType, List<?> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        Optional<BaselineConfig> configOpt = configRepository.findByAgentIdAndBaselineType(agentId, dataType);
        if (configOpt.isEmpty() || !"ACTIVE".equals(configOpt.get().getStatus())) {
            logger.debug("No active baseline for agent {} type {}", agentId, dataType);
            return;
        }

        BaselineCompareResult result = compareData(agentId, dataType, data);
        if (result != null && result.hasAnomalies()) {
            generateAlerts(agentId, dataType, result);
        }
    }

    public BaselineCompareResult compareData(String agentId, String dataType, List<?> data) {
        List<BaselineItem> baselineItems = getLatestBaselineItems(agentId, dataType);
        if (baselineItems.isEmpty()) {
            return null;
        }

        Map<String, BaselineItem> baselineMap = baselineItems.stream()
                .collect(Collectors.toMap(BaselineItem::getItemKey, i -> i, (a, b) -> a));

        List<AnomalyDTO> newItems = new ArrayList<>();
        List<AnomalyDTO> missingItems = new ArrayList<>();
        List<AnomalyDTO> modifiedItems = new ArrayList<>();

        Set<String> currentKeys = new HashSet<>();

        for (Object item : data) {
            Map<String, Object> itemMap = convertToMap(item);
            String key = extractKey(dataType, itemMap);
            if (key == null) continue;

            currentKeys.add(key);

            BaselineItem baseline = baselineMap.get(key);
            if (baseline == null) {
                AnomalyDTO anomaly = createAnomaly("NEW", key, null, itemMap, dataType);
                newItems.add(anomaly);
            } else {
                String currentValue = extractValue(dataType, itemMap);
                if (!Objects.equals(baseline.getItemValue(), currentValue)) {
                    AnomalyDTO anomaly = createAnomaly("MODIFIED", key, 
                            convertBaselineToMap(baseline), itemMap, dataType);
                    modifiedItems.add(anomaly);
                }
            }
        }

        for (BaselineItem baseline : baselineItems) {
            if (!currentKeys.contains(baseline.getItemKey())) {
                AnomalyDTO anomaly = createAnomaly("MISSING", baseline.getItemKey(),
                        convertBaselineToMap(baseline), null, dataType);
                missingItems.add(anomaly);
            }
        }

        BaselineCompareResult result = new BaselineCompareResult();
        result.setBaselineType(dataType);
        result.setNewItemsCount(newItems.size());
        result.setMissingItemsCount(missingItems.size());
        result.setModifiedItemsCount(modifiedItems.size());
        result.setNewItems(newItems);
        result.setMissingItems(missingItems);
        result.setModifiedItems(modifiedItems);

        return result;
    }

    private List<BaselineItem> getLatestBaselineItems(String agentId, String baselineType) {
        return snapshotRepository.findFirstByAgentIdAndBaselineTypeOrderByCreatedAtDesc(agentId, baselineType)
                .map(snapshot -> itemRepository.findBySnapshotId(snapshot.getId()))
                .orElse(Collections.emptyList());
    }

    private void generateAlerts(String agentId, String dataType, BaselineCompareResult result) {
        for (AnomalyDTO anomaly : result.getNewItems()) {
            createAlertForAnomaly(agentId, dataType, anomaly);
        }

        for (AnomalyDTO anomaly : result.getModifiedItems()) {
            createAlertForAnomaly(agentId, dataType, anomaly);
        }

        for (AnomalyDTO anomaly : result.getMissingItems()) {
            createAlertForAnomaly(agentId, dataType, anomaly);
        }

        logger.info("Generated {} alerts for agent {} type {}", 
                result.getTotalAnomalies(), agentId, dataType);
    }

    private void createAlertForAnomaly(String agentId, String dataType, AnomalyDTO anomaly) {
        String title = buildAlertTitle(dataType, anomaly);
        String content = buildAlertContent(dataType, anomaly);
        String level = determineAlertLevel(dataType, anomaly.getAnomalyType());

        alertService.createAlert(agentId, dataType, level, title, content, 
                anomaly.getAnomalyType(), 
                anomaly.getBaselineValue() != null ? anomaly.getBaselineValue().toString() : null,
                anomaly.getCurrentValue() != null ? anomaly.getCurrentValue().toString() : null);
    }

    private String buildAlertTitle(String dataType, AnomalyDTO anomaly) {
        String typeLabel = getTypeLabel(dataType);
        String anomalyLabel = getAnomalyLabel(anomaly.getAnomalyType());
        return String.format("[%s] %s: %s", typeLabel, anomalyLabel, anomaly.getItemKey());
    }

    private String buildAlertContent(String dataType, AnomalyDTO anomaly) {
        StringBuilder sb = new StringBuilder();
        sb.append("检测到").append(getAnomalyLabel(anomaly.getAnomalyType())).append("异常\n");
        sb.append("类型: ").append(getTypeLabel(dataType)).append("\n");
        sb.append("项目: ").append(anomaly.getItemKey()).append("\n");
        
        if ("NEW".equals(anomaly.getAnomalyType()) && anomaly.getCurrentValue() != null) {
            sb.append("当前值: ").append(anomaly.getCurrentValue());
        } else if ("MISSING".equals(anomaly.getAnomalyType()) && anomaly.getBaselineValue() != null) {
            sb.append("基线值: ").append(anomaly.getBaselineValue());
        } else if ("MODIFIED".equals(anomaly.getAnomalyType())) {
            if (anomaly.getBaselineValue() != null) {
                sb.append("基线值: ").append(anomaly.getBaselineValue()).append("\n");
            }
            if (anomaly.getCurrentValue() != null) {
                sb.append("当前值: ").append(anomaly.getCurrentValue());
            }
        }
        
        return sb.toString();
    }

    private String determineAlertLevel(String dataType, String anomalyType) {
        if ("PROCESS".equals(dataType) || "PORT".equals(dataType)) {
            if ("NEW".equals(anomalyType)) {
                return "HIGH";
            }
        }
        if ("USB".equals(dataType) && "NEW".equals(anomalyType)) {
            return "MEDIUM";
        }
        if ("LOGIN".equals(dataType) && "NEW".equals(anomalyType)) {
            return "HIGH";
        }
        return "LOW";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object item) {
        if (item instanceof Map) {
            return (Map<String, Object>) item;
        }
        return new HashMap<>();
    }

    private String extractKey(String dataType, Map<String, Object> item) {
        switch (dataType) {
            case "PROCESS":
                Object pid = item.get("pid");
                Object name = item.get("name");
                if (pid != null && name != null) {
                    return name + ":" + pid;
                }
                return name != null ? name.toString() : null;
            case "PORT":
                Object port = item.get("port");
                Object protocol = item.get("protocol");
                if (port != null) {
                    return protocol != null ? protocol + ":" + port : port.toString();
                }
                return null;
            case "USB":
                Object serial = item.get("serialNumber");
                if (serial != null) return serial.toString();
                Object vid = item.get("vendorId");
                Object pidUsb = item.get("productId");
                if (vid != null && pidUsb != null) {
                    return vid + ":" + pidUsb;
                }
                return item.get("deviceName") != null ? item.get("deviceName").toString() : null;
            case "LOGIN":
                Object username = item.get("username");
                Object loginType = item.get("loginType");
                if (username != null) {
                    return loginType != null ? username + ":" + loginType : username.toString();
                }
                return null;
            case "SOFTWARE":
                Object swName = item.get("name");
                Object swVersion = item.get("version");
                if (swName != null) {
                    return swVersion != null ? swName + ":" + swVersion : swName.toString();
                }
                return null;
            default:
                return null;
        }
    }

    private String extractValue(String dataType, Map<String, Object> item) {
        try {
            return toJson(item);
        } catch (Exception e) {
            return item.toString();
        }
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() == null) {
                sb.append("null");
            } else if (entry.getValue() instanceof String) {
                sb.append("\"").append(entry.getValue()).append("\"");
            } else {
                sb.append(entry.getValue());
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, Object> convertBaselineToMap(BaselineItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("itemKey", item.getItemKey());
        map.put("itemValue", item.getItemValue());
        map.put("itemType", item.getItemType());
        return map;
    }

    private AnomalyDTO createAnomaly(String anomalyType, String itemKey,
                                     Map<String, Object> baselineValue,
                                     Map<String, Object> currentValue,
                                     String dataType) {
        AnomalyDTO dto = new AnomalyDTO();
        dto.setAnomalyType(anomalyType);
        dto.setItemKey(itemKey);
        dto.setBaselineValue(baselineValue);
        dto.setCurrentValue(currentValue);
        dto.setAlertLevel(determineAlertLevel(dataType, anomalyType));
        return dto;
    }

    private String getTypeLabel(String dataType) {
        Map<String, String> labels = new HashMap<>();
        labels.put("PROCESS", "进程");
        labels.put("PORT", "端口");
        labels.put("USB", "USB设备");
        labels.put("LOGIN", "登录");
        labels.put("SOFTWARE", "软件");
        return labels.getOrDefault(dataType, dataType);
    }

    private String getAnomalyLabel(String anomalyType) {
        Map<String, String> labels = new HashMap<>();
        labels.put("NEW", "新增");
        labels.put("MISSING", "缺失");
        labels.put("MODIFIED", "修改");
        return labels.getOrDefault(anomalyType, anomalyType);
    }
}
