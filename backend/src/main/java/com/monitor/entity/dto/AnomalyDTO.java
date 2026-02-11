package com.monitor.entity.dto;

import java.util.Map;

public class AnomalyDTO {
    private String anomalyType;
    private String itemKey;
    private Map<String, Object> currentValue;
    private Map<String, Object> baselineValue;
    private String alertLevel;

    public AnomalyDTO() {}

    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }

    public Map<String, Object> getCurrentValue() { return currentValue; }
    public void setCurrentValue(Map<String, Object> currentValue) { this.currentValue = currentValue; }

    public Map<String, Object> getBaselineValue() { return baselineValue; }
    public void setBaselineValue(Map<String, Object> baselineValue) { this.baselineValue = baselineValue; }

    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
}
