package com.gosling.bms.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeleteItemsResult {
    private String type;
    private String task;
    private int successCount;
    private int failCount;
    private List<String> failedNames;
}
