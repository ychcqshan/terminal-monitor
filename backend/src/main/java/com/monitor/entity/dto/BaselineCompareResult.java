package com.monitor.entity.dto;

import java.util.List;

public class BaselineCompareResult {
    private String baselineType;
    private int newItemsCount;
    private int missingItemsCount;
    private int modifiedItemsCount;
    private List<AnomalyDTO> newItems;
    private List<AnomalyDTO> missingItems;
    private List<AnomalyDTO> modifiedItems;

    public BaselineCompareResult() {}

    public String getBaselineType() { return baselineType; }
    public void setBaselineType(String baselineType) { this.baselineType = baselineType; }

    public int getNewItemsCount() { return newItemsCount; }
    public void setNewItemsCount(int newItemsCount) { this.newItemsCount = newItemsCount; }

    public int getMissingItemsCount() { return missingItemsCount; }
    public void setMissingItemsCount(int missingItemsCount) { this.missingItemsCount = missingItemsCount; }

    public int getModifiedItemsCount() { return modifiedItemsCount; }
    public void setModifiedItemsCount(int modifiedItemsCount) { this.modifiedItemsCount = modifiedItemsCount; }

    public List<AnomalyDTO> getNewItems() { return newItems; }
    public void setNewItems(List<AnomalyDTO> newItems) { this.newItems = newItems; }

    public List<AnomalyDTO> getMissingItems() { return missingItems; }
    public void setMissingItems(List<AnomalyDTO> missingItems) { this.missingItems = missingItems; }

    public List<AnomalyDTO> getModifiedItems() { return modifiedItems; }
    public void setModifiedItems(List<AnomalyDTO> modifiedItems) { this.modifiedItems = modifiedItems; }

    public boolean hasAnomalies() {
        return newItemsCount > 0 || missingItemsCount > 0 || modifiedItemsCount > 0;
    }

    public int getTotalAnomalies() {
        return newItemsCount + missingItemsCount + modifiedItemsCount;
    }
}
