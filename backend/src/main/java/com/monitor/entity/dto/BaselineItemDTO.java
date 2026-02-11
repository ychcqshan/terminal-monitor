package com.monitor.entity.dto;

import java.util.List;
import java.util.Map;

public class BaselineItemDTO {
    private String itemKey;
    private String itemValue;
    private String itemType;

    public BaselineItemDTO() {}

    public BaselineItemDTO(String itemKey, String itemValue, String itemType) {
        this.itemKey = itemKey;
        this.itemValue = itemValue;
        this.itemType = itemType;
    }

    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }

    public String getItemValue() { return itemValue; }
    public void setItemValue(String itemValue) { this.itemValue = itemValue; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
}
